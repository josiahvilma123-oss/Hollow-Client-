package dev.hollow.detection;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.modules.BlockFinderModule;
import dev.hollow.modules.ServerRulesModule;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/**
 * Incrementally scans chunks the client already has loaded for the selected blocks.
 * Unloaded chunks are never requested or touched: lookups use {@code getChunk(..., false)},
 * which returns null instead of loading anything. Work is spread across ticks.
 */
public final class BlockScanner {
	/** Safety cap so an over-broad whitelist (e.g. stone) cannot exhaust memory. */
	public static final int MAX_MATCHES = 30_000;

	private final Long2ObjectMap<long[]> matchesByChunk = new Long2ObjectOpenHashMap<>();
	private final Long2IntOpenHashMap categoryByPos = new Long2IntOpenHashMap();
	private final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
	private final LongOpenHashSet queued = new LongOpenHashSet();
	private final LongOpenHashSet alerted = new LongOpenHashSet();
	private final Map<Block, Integer> targets = new IdentityHashMap<>();
	private final List<String> categoryIds = new ArrayList<>();
	private Predicate<BlockState> targetPredicate = state -> false;
	private ClientLevel level;
	private boolean rebuildRequested = true;
	private boolean dirty = true;
	private int tickCounter;
	private long version;
	private boolean capWarned;

	public void requestRebuild() {
		rebuildRequested = true;
	}

	/** Drops cached results and rescans every loaded chunk in range. */
	public void invalidateAll() {
		matchesByChunk.clear();
		categoryByPos.clear();
		queue.clear();
		queued.clear();
		dirty = true;
		version++;
	}

	public void clear() {
		invalidateAll();
		alerted.clear();
	}

	/** Monotonic counter bumped whenever the set of matches changes. */
	public long version() {
		return version;
	}

	public String categoryId(int index) {
		return categoryIds.get(index);
	}

	/** Number of current matches per category id. */
	public Map<String, Integer> countsByCategory() {
		int[] counts = new int[categoryIds.size()];
		for (int index : categoryByPos.values()) {
			counts[index]++;
		}
		Map<String, Integer> result = new java.util.HashMap<>();
		for (int i = 0; i < counts.length; i++) {
			result.put(categoryIds.get(i), counts[i]);
		}
		return result;
	}

	/** All current matches as packed positions mapped to category index. Do not modify. */
	public Long2IntOpenHashMap matches() {
		return categoryByPos;
	}

	public void tick(Minecraft minecraft) {
		BlockFinderModule module = Hollow.module(BlockFinderModule.class);
		ClientLevel current = minecraft.level;
		if (current != level) {
			level = current;
			clear();
			rebuildRequested = true;
		}
		if (!module.isEnabled() || current == null || minecraft.player == null
				|| !Hollow.module(ServerRulesModule.class).detectionAllowed()) {
			if (!categoryByPos.isEmpty()) {
				clear();
			}
			return;
		}
		if (rebuildRequested) {
			rebuildRequested = false;
			rebuildTargets(module);
			invalidateAll();
		}
		if (targets.isEmpty()) {
			return;
		}
		ChunkPos center = minecraft.player.chunkPosition();
		int radiusChunks = (int) Math.ceil(module.radius.get() / 16.0);
		if (tickCounter++ % 10 == 0) {
			enqueueMissing(current, center, radiusChunks);
			evictFarChunks(center, radiusChunks + 2);
		}
		int budget = module.scanSpeed.getInt();
		while (budget > 0 && !queue.isEmpty()) {
			long key = queue.dequeueLong();
			queued.remove(key);
			LevelChunk chunk = current.getChunkSource().getChunk(ChunkPos.getX(key), ChunkPos.getZ(key), ChunkStatus.FULL, false);
			if (chunk == null) {
				continue;
			}
			budget -= scanChunk(chunk, module, minecraft.player.getEyePosition());
		}
	}

	public void onChunkLoaded(LevelChunk chunk) {
		if (chunk.getLevel() == level) {
			enqueue(chunk.getPos().toLong());
		}
	}

	public void onChunkUnloaded(LevelChunk chunk) {
		removeChunk(chunk.getPos().toLong());
	}

	/** Keeps results current when a single block changes (mined, placed, grown). */
	public void onBlockChanged(ClientLevel changedLevel, BlockPos pos, BlockState newState) {
		if (changedLevel != level || targets.isEmpty()) {
			return;
		}
		long chunkKey = ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
		if (!matchesByChunk.containsKey(chunkKey)) {
			return;
		}
		long packed = pos.asLong();
		Integer category = targets.get(newState.getBlock());
		boolean had = categoryByPos.containsKey(packed);
		if (category == null && had) {
			categoryByPos.remove(packed);
			matchesByChunk.put(chunkKey, removeFrom(matchesByChunk.get(chunkKey), packed));
			dirty = true;
			version++;
		} else if (category != null && (!had || categoryByPos.get(packed) != category) && categoryByPos.size() < MAX_MATCHES) {
			categoryByPos.put(packed, (int) category);
			if (!had) {
				matchesByChunk.put(chunkKey, append(matchesByChunk.get(chunkKey), packed));
			}
			dirty = true;
			version++;
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.player != null) {
				maybeAlert(Hollow.module(BlockFinderModule.class), packed, category, minecraft.player.getEyePosition());
			}
		}
	}

	private void rebuildTargets(BlockFinderModule module) {
		Map<String, List<String>> definitions = new java.util.LinkedHashMap<>();
		Set<String> enabled = new HashSet<>();
		for (BlockFinderModule.CategorySettings settings : module.categories().values()) {
			definitions.put(settings.definition.id(), settings.definition.blocks());
			if (settings.isOn()) {
				enabled.add(settings.definition.id());
			}
		}
		List<String> whitelist = new ArrayList<>(expand(module.whitelist.get()));
		if (!module.category(BlockFilter.CUSTOM_CATEGORY).isOn()) {
			whitelist.removeIf(id -> !definitionContains(definitions, id));
		}
		BlockFilter filter = BlockFilter.build(module.mode.get(), definitions, enabled, whitelist, expand(module.blacklist.get()));

		targets.clear();
		categoryIds.clear();
		Map<String, Integer> indexByCategory = new java.util.HashMap<>();
		for (Map.Entry<String, String> entry : filter.asMap().entrySet()) {
			Identifier id = Identifier.tryParse(entry.getKey());
			if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
				continue;
			}
			Block block = BuiltInRegistries.BLOCK.getValue(id);
			int index = indexByCategory.computeIfAbsent(entry.getValue(), category -> {
				categoryIds.add(category);
				return categoryIds.size() - 1;
			});
			targets.put(block, index);
		}
		Set<Block> blocks = Collections.newSetFromMap(new IdentityHashMap<>());
		blocks.addAll(targets.keySet());
		targetPredicate = state -> blocks.contains(state.getBlock());
	}

	private static boolean definitionContains(Map<String, List<String>> definitions, String id) {
		return definitions.values().stream().anyMatch(list -> list.contains(id));
	}

	/** Expands "#namespace:tag" entries into the block ids currently in that tag. */
	private static List<String> expand(List<String> entries) {
		List<String> ids = new ArrayList<>();
		for (String entry : entries) {
			if (!entry.startsWith("#")) {
				ids.add(entry);
				continue;
			}
			Identifier tagId = Identifier.tryParse(entry.substring(1));
			if (tagId == null) {
				continue;
			}
			TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
			for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
				ids.add(BuiltInRegistries.BLOCK.getKey(holder.value()).toString());
			}
		}
		return ids;
	}

	private void enqueueMissing(ClientLevel current, ChunkPos center, int radius) {
		// Nearest chunks first so results appear around the player before the edges.
		for (int ring = 0; ring <= radius; ring++) {
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					int x = center.x + dx;
					int z = center.z + dz;
					long key = ChunkPos.asLong(x, z);
					if (matchesByChunk.containsKey(key) || queued.contains(key)) {
						continue;
					}
					if (current.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) != null) {
						enqueue(key);
					}
				}
			}
		}
	}

	private void enqueue(long key) {
		if (queued.add(key)) {
			queue.enqueue(key);
		}
	}

	private void evictFarChunks(ChunkPos center, int keepRadius) {
		List<Long> far = new ArrayList<>();
		for (long key : matchesByChunk.keySet()) {
			if (Math.abs(ChunkPos.getX(key) - center.x) > keepRadius || Math.abs(ChunkPos.getZ(key) - center.z) > keepRadius) {
				far.add(key);
			}
		}
		far.forEach(this::removeChunk);
	}

	private void removeChunk(long key) {
		long[] positions = matchesByChunk.remove(key);
		if (positions != null && positions.length > 0) {
			for (long pos : positions) {
				categoryByPos.remove(pos);
			}
			dirty = true;
			version++;
		}
	}

	/** Scans one chunk and returns the number of sections that cost real work. */
	private int scanChunk(LevelChunk chunk, BlockFinderModule module, Vec3 eye) {
		long chunkKey = chunk.getPos().toLong();
		removeChunk(chunkKey);
		LevelChunkSection[] sections = chunk.getSections();
		LongArrayList found = new LongArrayList();
		int work = 1;
		int baseX = chunk.getPos().getMinBlockX();
		int baseZ = chunk.getPos().getMinBlockZ();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int i = 0; i < sections.length; i++) {
			LevelChunkSection section = sections[i];
			if (section == null || section.hasOnlyAir() || !section.maybeHas(targetPredicate)) {
				continue;
			}
			work++;
			int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					for (int x = 0; x < 16; x++) {
						BlockState state = section.getBlockState(x, y, z);
						Integer category = targets.get(state.getBlock());
						if (category == null) {
							continue;
						}
						if (categoryByPos.size() >= MAX_MATCHES) {
							warnCap();
							continue;
						}
						long packed = cursor.set(baseX + x, baseY + y, baseZ + z).asLong();
						categoryByPos.put(packed, (int) category);
						found.add(packed);
						maybeAlert(module, packed, category, eye);
					}
				}
			}
		}
		matchesByChunk.put(chunkKey, found.toLongArray());
		if (!found.isEmpty()) {
			dirty = true;
			version++;
		}
		return work;
	}

	private void maybeAlert(BlockFinderModule module, long packed, int categoryIndex, Vec3 eye) {
		if (!alerted.add(packed)) {
			return;
		}
		BlockFinderModule.CategorySettings settings = module.category(categoryIds.get(categoryIndex));
		if (!settings.alert.get()) {
			return;
		}
		double distance = Math.sqrt(eye.distanceToSqr(BlockPos.getX(packed) + 0.5, BlockPos.getY(packed) + 0.5, BlockPos.getZ(packed) + 0.5));
		if (distance > Math.min(settings.range.get(), module.radius.get())) {
			alerted.remove(packed);
			return;
		}
		Hollow.alerts().report(new Alert(AlertSource.BLOCKS, settings.name(), "Detected", distance, "block:" + settings.definition.id()));
	}

	private void warnCap() {
		if (!capWarned) {
			capWarned = true;
			Hollow.LOGGER.warn("Block Finder reached {} matches; narrow the whitelist or categories to see more", MAX_MATCHES);
		}
	}

	public boolean consumeDirty() {
		boolean was = dirty;
		dirty = false;
		return was;
	}

	private static long[] append(long[] array, long value) {
		long[] copy = java.util.Arrays.copyOf(array, array.length + 1);
		copy[array.length] = value;
		return copy;
	}

	private static long[] removeFrom(long[] array, long value) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == value) {
				long[] copy = new long[array.length - 1];
				System.arraycopy(array, 0, copy, 0, i);
				System.arraycopy(array, i + 1, copy, i, array.length - i - 1);
				return copy;
			}
		}
		return array;
	}
}
