package dev.hollow.detection;

import java.util.Collection;
import java.util.Set;

/**
 * Decides whether an entity type is shown, given its category and the user's custom filters.
 * Entity ids use the "namespace:path" form, e.g. "minecraft:creeper".
 */
public final class EntityFilter {
	private final Set<EntityCategory> enabledCategories;
	private final boolean onlyListed;
	private final Set<String> listed;
	private final Set<String> hidden;

	public EntityFilter(Collection<EntityCategory> enabledCategories, boolean onlyListed, Collection<String> listed, Collection<String> hidden) {
		this.enabledCategories = Set.copyOf(enabledCategories);
		this.onlyListed = onlyListed;
		this.listed = Set.copyOf(listed);
		this.hidden = Set.copyOf(hidden);
	}

	/**
	 * @param category the entity's natural category, or null if it has none (items, projectiles...)
	 * @return the category to display it under, or null when it is filtered out
	 */
	public EntityCategory resolve(String entityId, EntityCategory category) {
		if (hidden.contains(entityId)) {
			return null;
		}
		boolean isListed = listed.contains(entityId);
		if (onlyListed) {
			if (!isListed) {
				return null;
			}
			if (category == EntityCategory.PLAYERS) {
				return enabledCategories.contains(EntityCategory.PLAYERS) ? category : null;
			}
			return category == null ? EntityCategory.CUSTOM : category;
		}
		if (category != null && enabledCategories.contains(category)) {
			return category;
		}
		if (isListed && category != EntityCategory.PLAYERS && enabledCategories.contains(EntityCategory.CUSTOM)) {
			return EntityCategory.CUSTOM;
		}
		return null;
	}
}
