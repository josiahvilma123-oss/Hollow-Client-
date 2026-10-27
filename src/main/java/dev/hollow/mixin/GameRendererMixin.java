package dev.hollow.mixin;

import dev.hollow.blur.MotionBlurRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	/** Frame timing for the performance monitor and adaptive quality. */
	@Inject(method = "render", at = @At("HEAD"))
	private void hollow$frame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		dev.hollow.perf.PerformanceState.INSTANCE.onFrame();
	}

	/** After the world, hand and vanilla post effects are drawn, before any HUD or screen. */
	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"))
	private void hollow$motionBlur(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		if (renderLevel && minecraft.level != null && minecraft.isGameLoadFinished()) {
			MotionBlurRenderer.INSTANCE.onWorldRendered(minecraft);
		}
	}
}
