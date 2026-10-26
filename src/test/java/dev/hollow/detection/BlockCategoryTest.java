package dev.hollow.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BlockCategoryTest {
	@Test
	void idsAreUniqueAndNamespaced() {
		Set<String> categoryIds = new HashSet<>();
		Set<String> blocks = new HashSet<>();
		for (BlockCategory category : BlockCategory.ALL) {
			assertTrue(categoryIds.add(category.id()), "duplicate category " + category.id());
			for (String block : category.blocks()) {
				assertTrue(block.startsWith("minecraft:"), block);
				assertTrue(blocks.add(block), "block in two categories: " + block);
			}
		}
		assertTrue(!categoryIds.contains(BlockFilter.CUSTOM_CATEGORY));
	}

	@Test
	void amethystStagesAreIndividuallyConfigurable() {
		long amethyst = BlockCategory.ALL.stream().filter(c -> c.group().equals(BlockCategory.AMETHYST)).count();
		assertEquals(5, amethyst);
	}

	@Test
	void shulkerBoxesCoverAllColors() {
		BlockCategory shulkers = BlockCategory.ALL.stream().filter(c -> c.id().equals("shulker_boxes")).findFirst().orElseThrow();
		assertEquals(17, shulkers.blocks().size());
	}
}
