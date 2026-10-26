package dev.hollow.detection;

import java.util.ArrayList;
import java.util.List;

/**
 * A named group of block ids searched for together, such as "Diamond Ore" (normal and deepslate).
 * Pure data: Minecraft-specific lookups happen in the scanner.
 */
public record BlockCategory(String id, String name, String group, List<String> blocks, int defaultColor, boolean defaultEnabled,
		int defaultRange, boolean defaultAlert) {

	public static final String AMETHYST = "Amethyst";
	public static final String ORES = "Ores & Minerals";
	public static final String STORAGE = "Storage";
	public static final String RARE = "Rare & Structures";

	public static final List<BlockCategory> ALL = List.of(
		of("amethyst_cluster", "Amethyst Cluster", AMETHYST, 0xFFC77DFF, true, 48, true, "amethyst_cluster"),
		of("large_amethyst_bud", "Large Amethyst Bud", AMETHYST, 0xFFA66CFF, false, 32, false, "large_amethyst_bud"),
		of("medium_amethyst_bud", "Medium Amethyst Bud", AMETHYST, 0xFF8E5CE6, false, 32, false, "medium_amethyst_bud"),
		of("small_amethyst_bud", "Small Amethyst Bud", AMETHYST, 0xFF7550C4, false, 24, false, "small_amethyst_bud"),
		of("budding_amethyst", "Budding Amethyst", AMETHYST, 0xFFE0AAFF, true, 48, true, "budding_amethyst"),

		of("diamond_ore", "Diamond Ore", ORES, 0xFF5CE1E6, true, 48, true, "diamond_ore", "deepslate_diamond_ore"),
		of("emerald_ore", "Emerald Ore", ORES, 0xFF3DDC84, false, 48, false, "emerald_ore", "deepslate_emerald_ore"),
		of("gold_ore", "Gold Ore", ORES, 0xFFFFD34D, false, 32, false, "gold_ore", "deepslate_gold_ore", "nether_gold_ore"),
		of("iron_ore", "Iron Ore", ORES, 0xFFD9A38B, false, 32, false, "iron_ore", "deepslate_iron_ore"),
		of("copper_ore", "Copper Ore", ORES, 0xFFE07A4F, false, 24, false, "copper_ore", "deepslate_copper_ore"),
		of("lapis_ore", "Lapis Ore", ORES, 0xFF3F6FE0, false, 32, false, "lapis_ore", "deepslate_lapis_ore"),
		of("redstone_ore", "Redstone Ore", ORES, 0xFFE0303A, false, 32, false, "redstone_ore", "deepslate_redstone_ore"),
		of("coal_ore", "Coal Ore", ORES, 0xFF5A5A66, false, 24, false, "coal_ore", "deepslate_coal_ore"),
		of("quartz_ore", "Nether Quartz Ore", ORES, 0xFFEDE6DA, false, 24, false, "nether_quartz_ore"),
		of("ancient_debris", "Ancient Debris", ORES, 0xFFB0644A, true, 48, true, "ancient_debris"),

		of("chests", "Chest", STORAGE, 0xFFE8B04A, false, 48, false, "chest", "trapped_chest",
			"copper_chest", "exposed_copper_chest", "weathered_copper_chest", "oxidized_copper_chest",
			"waxed_copper_chest", "waxed_exposed_copper_chest", "waxed_weathered_copper_chest", "waxed_oxidized_copper_chest"),
		of("barrels", "Barrel", STORAGE, 0xFFB98552, false, 48, false, "barrel"),
		of("ender_chests", "Ender Chest", STORAGE, 0xFF2BC4A0, false, 48, false, "ender_chest"),
		of("shulker_boxes", "Shulker Box", STORAGE, 0xFFB57BFF, false, 48, false, shulkerBoxes()),

		of("spawners", "Spawner", RARE, 0xFFFF5470, false, 64, false, "spawner", "trial_spawner"),
		of("vaults", "Vault", RARE, 0xFFF2C14E, false, 64, false, "vault")
	);

	public static BlockCategory custom() {
		return new BlockCategory(BlockFilter.CUSTOM_CATEGORY, "Custom Block", "Custom", List.of(), 0xFF9D4EDD, true, 48, true);
	}

	private static BlockCategory of(String id, String name, String group, int color, boolean enabled, int range, boolean alert, String... blocks) {
		List<String> ids = new ArrayList<>();
		for (String block : blocks) {
			ids.add("minecraft:" + block);
		}
		return new BlockCategory(id, name, group, List.copyOf(ids), color, enabled, range, alert);
	}

	private static String[] shulkerBoxes() {
		String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
			"purple", "blue", "brown", "green", "red", "black"};
		String[] ids = new String[colors.length + 1];
		ids[0] = "shulker_box";
		for (int i = 0; i < colors.length; i++) {
			ids[i + 1] = colors[i] + "_shulker_box";
		}
		return ids;
	}
}
