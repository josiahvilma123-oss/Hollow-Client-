package dev.hollow;

import dev.hollow.blur.MotionBlurRenderer;
import dev.hollow.hands.HandRenderHooks;
import dev.hollow.modules.HollowHandsModule;
import dev.hollow.modules.MotionBlurModule;
import net.minecraft.client.Minecraft;

/**
 * Detects render hooks that never run, which happens when another mod replaces the same
 * renderer. Affected modules are disabled with an explanation instead of silently failing.
 */
final class HookHealth {
	private static final int GRACE_TICKS = 200;
	private int worldTicks;
	private int firstPersonTicks;
	private boolean checkedBlur;
	private boolean checkedHands;

	void tick(Minecraft client) {
		if (client.level == null || client.player == null) {
			return;
		}
		worldTicks++;
		if (client.options.getCameraType().isFirstPerson() && !client.options.hideGui) {
			firstPersonTicks++;
		}
		if (!checkedBlur && worldTicks > GRACE_TICKS) {
			checkedBlur = true;
			if (!MotionBlurRenderer.INSTANCE.hookReached()) {
				Hollow.module(MotionBlurModule.class).markUnavailable("The frame hook never ran. Another mod may replace the game renderer.");
				Hollow.LOGGER.warn("Motion Blur disabled: GameRenderer hook was not reached");
			}
		}
		if (!checkedHands && firstPersonTicks > GRACE_TICKS) {
			checkedHands = true;
			if (!HandRenderHooks.hookReached()) {
				Hollow.module(HollowHandsModule.class).markUnavailable("The hand render hook never ran. Another mod may replace first-person rendering.");
				Hollow.LOGGER.warn("Hollow Hands disabled: ItemInHandRenderer hook was not reached");
			}
		}
	}
}
