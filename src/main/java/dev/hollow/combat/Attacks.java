package dev.hollow.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Attacks through the normal attack interaction, with vanilla's own reach and cooldown rules. */
public final class Attacks {
	private Attacks() {
	}

	/** Attack charge reached (1.0 = fully charged, as vanilla shows on the crosshair). */
	public static boolean charged(LocalPlayer player, double threshold) {
		return player.getAttackStrengthScale(0.5f) >= threshold;
	}

	/** Within the game's own entity interaction range (measured to the hitbox, like vanilla). */
	public static boolean inReach(LocalPlayer player, Entity entity) {
		return player.isWithinEntityInteractionRange(entity, 0.0);
	}

	public static boolean holdingWeapon(LocalPlayer player) {
		ItemStack stack = player.getMainHandItem();
		return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
	}

	/** One ordinary attack, exactly like a left-click on the entity. */
	public static void attack(Minecraft minecraft, LocalPlayer player, Entity entity) {
		minecraft.gameMode.attack(player, entity);
		player.swing(InteractionHand.MAIN_HAND);
	}
}
