package dev.hollow.detection;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/** Sorts entities into Hollow's categories using only information the client already has. */
public final class EntityClassifier {
	private EntityClassifier() {
	}

	/** Returns the natural category of an entity, or null for things like items and projectiles. */
	public static EntityCategory classify(Entity entity) {
		if (entity instanceof Player) {
			return EntityCategory.PLAYERS;
		}
		if (entity instanceof AbstractVillager || entity instanceof NeutralMob) {
			return EntityCategory.NEUTRAL;
		}
		if (entity instanceof Enemy) {
			return EntityCategory.HOSTILE;
		}
		if (entity instanceof AbstractGolem) {
			return EntityCategory.NEUTRAL;
		}
		MobCategory category = entity.getType().getCategory();
		return switch (category) {
			case MONSTER -> EntityCategory.HOSTILE;
			case MISC -> null;
			default -> EntityCategory.PASSIVE;
		};
	}
}
