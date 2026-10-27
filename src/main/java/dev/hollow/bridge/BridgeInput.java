package dev.hollow.bridge;

import dev.hollow.Hollow;
import dev.hollow.core.bridge.BridgeMath;
import dev.hollow.modules.AutoBridgeModule;
import dev.hollow.modules.SpeedBridgeModule;
import dev.hollow.policy.Automation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Sneak assistance for bridging: holds sneak while the player is at a block edge with nothing
 * below the next step, so they can't walk off. Computed from the live player state each time
 * movement input is read.
 */
public final class BridgeInput {
	/** How far ahead of the player's centre the edge is checked (just under half a player's width). */
	public static final double LOOKAHEAD = 0.28;

	private BridgeInput() {
	}

	public static boolean forceSneak() {
		if (Hollow.modules() == null) {
			return false;
		}
		AutoBridgeModule auto = Hollow.module(AutoBridgeModule.class);
		SpeedBridgeModule speed = Hollow.module(SpeedBridgeModule.class);
		boolean wanted = auto.isEnabled() && auto.sneakAssist.get() || speed.isEnabled() && speed.sneakAssistActive();
		if (!wanted || !Automation.allowed()) {
			return false;
		}
		return overEdge();
	}

	/** Whether the player is on the ground and the next step in their movement direction is over open air. */
	public static boolean overEdge() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || !player.onGround() || player.getAbilities().flying) {
			return false;
		}
		Vec3 move = Bridging.movement(player);
		return BridgeMath.atEdge(Bridging.world(minecraft.level), player.getX(), player.getY(), player.getZ(), move.x, move.z, LOOKAHEAD);
	}
}
