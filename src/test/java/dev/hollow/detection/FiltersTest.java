package dev.hollow.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FiltersTest {
	private static Map<String, List<String>> definitions() {
		Map<String, List<String>> defs = new LinkedHashMap<>();
		defs.put("amethyst_cluster", List.of("minecraft:amethyst_cluster"));
		defs.put("budding_amethyst", List.of("minecraft:budding_amethyst"));
		defs.put("diamond", List.of("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"));
		return defs;
	}

	@Test
	void categoriesModeUsesEnabledCategoriesPlusWhitelist() {
		BlockFilter filter = BlockFilter.build(BlockFilter.Mode.CATEGORIES, definitions(), Set.of("diamond"),
			List.of("minecraft:spawner"), List.of());
		assertEquals("diamond", filter.categoryOf("minecraft:deepslate_diamond_ore"));
		assertEquals(BlockFilter.CUSTOM_CATEGORY, filter.categoryOf("minecraft:spawner"));
		assertNull(filter.categoryOf("minecraft:amethyst_cluster"));
	}

	@Test
	void blacklistBeatsEverything() {
		BlockFilter filter = BlockFilter.build(BlockFilter.Mode.CATEGORIES, definitions(), Set.of("diamond"),
			List.of("minecraft:deepslate_diamond_ore"), List.of("minecraft:deepslate_diamond_ore"));
		assertEquals("diamond", filter.categoryOf("minecraft:diamond_ore"));
		assertNull(filter.categoryOf("minecraft:deepslate_diamond_ore"));
	}

	@Test
	void whitelistOnlyKeepsCategoryStyling() {
		BlockFilter filter = BlockFilter.build(BlockFilter.Mode.WHITELIST_ONLY, definitions(), Set.of("diamond"),
			List.of("minecraft:budding_amethyst"), List.of());
		assertNull(filter.categoryOf("minecraft:diamond_ore"));
		assertEquals("budding_amethyst", filter.categoryOf("minecraft:budding_amethyst"));
	}

	@Test
	void entityFilterHonoursCategoriesAndLists() {
		EntityFilter filter = new EntityFilter(Set.of(EntityCategory.HOSTILE, EntityCategory.CUSTOM), false,
			List.of("minecraft:armor_stand"), List.of("minecraft:zombie"));
		assertEquals(EntityCategory.HOSTILE, filter.resolve("minecraft:creeper", EntityCategory.HOSTILE));
		assertNull(filter.resolve("minecraft:zombie", EntityCategory.HOSTILE));
		assertNull(filter.resolve("minecraft:cow", EntityCategory.PASSIVE));
		assertEquals(EntityCategory.CUSTOM, filter.resolve("minecraft:armor_stand", null));
	}

	@Test
	void onlyListedModeNeverAddsPlayersWithoutPermission() {
		EntityFilter filter = new EntityFilter(Set.of(EntityCategory.HOSTILE), true,
			List.of("minecraft:player", "minecraft:cow"), List.of());
		assertNull(filter.resolve("minecraft:player", EntityCategory.PLAYERS));
		assertEquals(EntityCategory.PASSIVE, filter.resolve("minecraft:cow", EntityCategory.PASSIVE));
		assertNull(filter.resolve("minecraft:creeper", EntityCategory.HOSTILE));
	}
}
