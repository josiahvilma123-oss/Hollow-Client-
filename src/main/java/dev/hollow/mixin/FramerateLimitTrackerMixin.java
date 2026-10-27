package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import dev.hollow.perf.FpsBoost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FPS Boost: lower frame rate while the game window is in the background. */
@Mixin(FramerateLimitTracker.class)
abstract class FramerateLimitTrackerMixin {
	@ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
	private int hollow$background(int limit) {
		int background = FpsBoost.backgroundLimit();
		return background > 0 ? Math.min(limit, background) : limit;
	}
}
