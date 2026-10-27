package dev.hollow.modules;

import dev.hollow.Hollow;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.setting.StringListSetting;
import dev.hollow.detection.BlockCategory;
import dev.hollow.detection.BlockFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class BlockFinderModule extends Module {
	/** "namespace:path" or "#namespace:path" for block tags. */
	private static final Pattern BLOCK_ENTRY = Pattern.compile("#?[a-z0-9_.-]+:[a-z0-9_./-]+");

	public final NumberSetting radius = add("Search", new NumberSetting("radius", "Search Radius",
		"Upper limit for every category's range. Only chunks the client has loaded are scanned.", 64, 16, 256, 8).unit("m"));
	public final NumberSetting scanSpeed = add("Search", new NumberSetting("scan_speed", "Scan Speed",
		"Chunk sections examined per tick. Lower values spread the work out further.", 48, 8, 256, 8));
	public final EnumSetting<BlockFilter.Mode> mode = add("Search", new EnumSetting<>("mode", "Filter Mode",
		"Categories: search enabled categories plus the whitelist. Whitelist only: search just the whitelist.",
		BlockFilter.Mode.CATEGORIES));
	public final ActionSetting rescan = add("Search", new ActionSetting("rescan", "Rescan",
		"Forget cached results and scan loaded chunks again.", "Rescan now", () -> {
			if (Hollow.scanner() != null) {
				Hollow.scanner().invalidateAll();
			}
		}));

	public final StringListSetting whitelist = add("Whitelist & Blacklist", new StringListSetting("whitelist", "Whitelist",
		"Extra blocks to find, e.g. minecraft:spawner or #minecraft:logs.", List.of(), BLOCK_ENTRY.asMatchPredicate(), "minecraft:block_id"));
	public final StringListSetting blacklist = add("Whitelist & Blacklist", new StringListSetting("blacklist", "Blacklist",
		"Blocks that are never shown, even if a category includes them.", List.of(), BLOCK_ENTRY.asMatchPredicate(), "minecraft:block_id"));

	private final Map<String, CategorySettings> categories = new LinkedHashMap<>();

	public BlockFinderModule() {
		super("block_finder", "World Scanner", "Highlights and reports selected blocks in chunks your client has already loaded. Never loads chunks.",
			Category.EXPLORATION, false);
		for (BlockCategory category : BlockCategory.ALL) {
			categories.put(category.id(), new CategorySettings(category));
		}
		BlockCategory custom = BlockCategory.custom();
		categories.put(custom.id(), new CategorySettings(custom));

		Runnable rebuild = () -> {
			if (Hollow.scanner() != null) {
				Hollow.scanner().requestRebuild();
			}
		};
		mode.onChange(v -> rebuild.run());
		whitelist.onChange(v -> rebuild.run());
		blacklist.onChange(v -> rebuild.run());
		for (CategorySettings settings : categories.values()) {
			settings.enabled.onChange(v -> rebuild.run());
		}
	}

	public Map<String, CategorySettings> categories() {
		return Collections.unmodifiableMap(categories);
	}

	public CategorySettings category(String id) {
		CategorySettings settings = categories.get(id);
		return settings != null ? settings : categories.get(BlockFilter.CUSTOM_CATEGORY);
	}

	public List<BlockCategory> definitions() {
		List<BlockCategory> list = new ArrayList<>();
		for (CategorySettings settings : categories.values()) {
			list.add(settings.definition);
		}
		return list;
	}

	@Override
	protected void onEnable() {
		if (Hollow.scanner() != null) {
			Hollow.scanner().requestRebuild();
		}
	}

	@Override
	protected void onDisable() {
		if (Hollow.scanner() != null) {
			Hollow.scanner().clear();
		}
	}

	/** Per-category visibility, color, range, tracer and alert settings. */
	public final class CategorySettings {
		public final BlockCategory definition;
		public final BoolSetting enabled;
		public final ColorSetting color;
		public final NumberSetting range;
		public final BoolSetting tracer;
		public final BoolSetting alert;

		private CategorySettings(BlockCategory category) {
			this.definition = category;
			String group = category.group().equals("Custom") ? "Custom Blocks" : category.group() + " · " + category.name();
			String prefix = category.id() + ".";
			boolean custom = category.id().equals(BlockFilter.CUSTOM_CATEGORY);
			this.enabled = custom
				? new BoolSetting(prefix + "enabled", "Show Custom Blocks", "Show whitelisted blocks outside the built-in categories.", true)
				: new BoolSetting(prefix + "enabled", "Find " + category.name(), "Search for " + String.join(", ", category.blocks()) + ".",
					category.defaultEnabled());
			this.color = new ColorSetting(prefix + "color", "Color", "Box, tracer and label color.", category.defaultColor());
			this.range = new NumberSetting(prefix + "range", "Range", "Maximum distance for this category.",
				category.defaultRange(), 8, 256, 4).unit("m");
			this.tracer = new BoolSetting(prefix + "tracer", "Tracer", "Draw a line from the crosshair.", false);
			this.alert = new BoolSetting(prefix + "alert", "Alert", "Notify when a new block of this kind is found.", category.defaultAlert());
			for (Setting<?> setting : List.of(enabled, color, range, tracer, alert)) {
				add(group, setting);
			}
			BooleanVisibility.when(enabled, color, range, tracer, alert);
		}

		public boolean isOn() {
			return enabled.get();
		}

		public String name() {
			return definition.name();
		}
	}
}
