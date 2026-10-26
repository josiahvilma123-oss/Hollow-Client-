package dev.hollow.detection;

import dev.hollow.Hollow;
import dev.hollow.modules.BlockEspModule;
import dev.hollow.modules.BlockFinderModule;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Turns raw scanner matches into the list Block ESP draws: optionally merges touching blocks
 * of the same category into veins, then applies range, per-category and total limits.
 */
public final class BlockResults {
	public record Cluster(String categoryId, AABB box, int count, double distance) {
		public Vec3 center() {
			return box.getCenter();
		}
	}

	private final BlockScanner scanner;
	private List<Cluster> clusters = List.of();
	private List<Cluster> visible = List.of();
	private long builtVersion = -1;
	private boolean builtMerged;

	public BlockResults(BlockScanner scanner) {
		this.scanner = scanner;
	}

	/** Nearest-first clusters to draw this frame. */
	public List<Cluster> visible() {
		return visible;
	}

	public void tick(ClientLevel level, Vec3 eye) {
		BlockFinderModule finder = Hollow.module(BlockFinderModule.class);
		BlockEspModule esp = Hollow.module(BlockEspModule.class);
		if (level == null || !finder.isEnabled()) {
			clusters = List.of();
			visible = List.of();
			builtVersion = -1;
			return;
		}
		boolean merge = esp.merge.get();
		if (scanner.version() != builtVersion || merge != builtMerged) {
			clusters = build(level, merge);
			builtVersion = scanner.version();
			builtMerged = merge;
		}
		visible = select(finder, esp, eye);
	}

	private List<Cluster> build(ClientLevel level, boolean merge) {
		Long2IntOpenHashMap matches = scanner.matches();
		List<Cluster> result = new ArrayList<>();
		LongOpenHashSet visited = new LongOpenHashSet();
		LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (Long2IntMap.Entry entry : matches.long2IntEntrySet()) {
			long start = entry.getLongKey();
			if (!visited.add(start)) {
				continue;
			}
			int category = entry.getIntValue();
			if (!merge) {
				result.add(new Cluster(scanner.categoryId(category), singleBlockBox(level, cursor.set(start)), 1, 0));
				continue;
			}
			int minX = BlockPos.getX(start);
			int minY = BlockPos.getY(start);
			int minZ = BlockPos.getZ(start);
			int maxX = minX;
			int maxY = minY;
			int maxZ = minZ;
			int count = 0;
			queue.enqueue(start);
			while (!queue.isEmpty()) {
				long current = queue.dequeueLong();
				count++;
				int x = BlockPos.getX(current);
				int y = BlockPos.getY(current);
				int z = BlockPos.getZ(current);
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				minZ = Math.min(minZ, z);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				maxZ = Math.max(maxZ, z);
				for (Direction direction : Direction.values()) {
					long neighbour = BlockPos.offset(current, direction);
					if (matches.containsKey(neighbour) && matches.get(neighbour) == category && visited.add(neighbour)) {
						queue.enqueue(neighbour);
					}
				}
			}
			AABB box = count == 1
				? singleBlockBox(level, cursor.set(start))
				: new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
			result.add(new Cluster(scanner.categoryId(category), box, count, 0));
		}
		return result;
	}

	private static AABB singleBlockBox(ClientLevel level, BlockPos pos) {
		VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
		if (shape.isEmpty()) {
			return new AABB(pos);
		}
		return shape.bounds().move(pos);
	}

	private List<Cluster> select(BlockFinderModule finder, BlockEspModule esp, Vec3 eye) {
		double radius = finder.radius.get();
		List<Cluster> inRange = new ArrayList<>();
		for (Cluster cluster : clusters) {
			BlockFinderModule.CategorySettings settings = finder.category(cluster.categoryId());
			double distance = Math.sqrt(cluster.box().distanceToSqr(eye));
			if (distance > Math.min(radius, settings.range.get())) {
				continue;
			}
			inRange.add(new Cluster(cluster.categoryId(), cluster.box(), cluster.count(), distance));
		}
		inRange.sort(Comparator.comparingDouble(Cluster::distance));
		int perCategory = esp.perCategory.getInt();
		int max = esp.maxBoxes.getInt();
		Map<String, Integer> counts = new HashMap<>();
		List<Cluster> selected = new ArrayList<>();
		for (Cluster cluster : inRange) {
			int used = counts.merge(cluster.categoryId(), 1, Integer::sum);
			if (used > perCategory) {
				continue;
			}
			selected.add(cluster);
			if (selected.size() >= max) {
				break;
			}
		}
		return selected;
	}
}
