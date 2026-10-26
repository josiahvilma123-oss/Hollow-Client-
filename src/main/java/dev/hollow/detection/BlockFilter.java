package dev.hollow.detection;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resolves which block ids are searched for and which category each belongs to.
 * Precedence: blacklist &gt; whitelist &gt; categories.
 */
public final class BlockFilter {
	public static final String CUSTOM_CATEGORY = "custom";

	public enum Mode {
		/** Enabled categories plus whitelisted extras. */
		CATEGORIES,
		/** Only whitelisted blocks; categories only provide styling. */
		WHITELIST_ONLY
	}

	private final Map<String, String> categoryByBlock;

	private BlockFilter(Map<String, String> categoryByBlock) {
		this.categoryByBlock = Map.copyOf(categoryByBlock);
	}

	/**
	 * @param definitions category id -&gt; block ids for every known category
	 * @param enabled     ids of the categories the user turned on
	 * @param whitelist   expanded block ids the user always wants (tags already resolved)
	 * @param blacklist   expanded block ids that must never be shown
	 */
	public static BlockFilter build(Mode mode, Map<String, ? extends Collection<String>> definitions, Set<String> enabled,
			Collection<String> whitelist, Collection<String> blacklist) {
		Map<String, String> definedCategory = new HashMap<>();
		definitions.forEach((category, blocks) -> blocks.forEach(block -> definedCategory.putIfAbsent(block, category)));

		Map<String, String> result = new LinkedHashMap<>();
		if (mode == Mode.CATEGORIES) {
			definitions.forEach((category, blocks) -> {
				if (enabled.contains(category)) {
					blocks.forEach(block -> result.putIfAbsent(block, category));
				}
			});
		}
		for (String block : whitelist) {
			result.putIfAbsent(block, definedCategory.getOrDefault(block, CUSTOM_CATEGORY));
		}
		blacklist.forEach(result::remove);
		return new BlockFilter(result);
	}

	/** The category of a block id, or null when the block is not searched for. */
	public String categoryOf(String blockId) {
		return categoryByBlock.get(blockId);
	}

	public Set<String> blocks() {
		return categoryByBlock.keySet();
	}

	public Map<String, String> asMap() {
		return categoryByBlock;
	}

	public boolean isEmpty() {
		return categoryByBlock.isEmpty();
	}
}
