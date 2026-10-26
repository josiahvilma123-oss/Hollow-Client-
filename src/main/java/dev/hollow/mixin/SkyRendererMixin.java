package dev.hollow.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hollow.sky.HoloSkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the overworld sky with Holo Sky Studio's renderer. The sky disc call decides once
 * per frame; the remaining vanilla sky calls are skipped only when that decision was "replace".
 */
@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void hollow$capture(ClientLevel level, float partialTick, Camera camera, SkyRenderState state, CallbackInfo ci) {
		HoloSkyRenderer.INSTANCE.capture(state);
	}

	@Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
	private void hollow$replaceSky(int skyColor, CallbackInfo ci) {
		if (HoloSkyRenderer.INSTANCE.beginWorldSky()) {
			HoloSkyRenderer.INSTANCE.renderWorldSky();
			ci.cancel();
		}
	}

	@Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
	private void hollow$skipSunrise(PoseStack poseStack, float sunAngle, int color, CallbackInfo ci) {
		if (HoloSkyRenderer.INSTANCE.replacingThisFrame()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
	private void hollow$skipCelestial(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase phase, float rain,
			float stars, CallbackInfo ci) {
		if (HoloSkyRenderer.INSTANCE.replacingThisFrame()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
	private void hollow$skipDarkDisc(CallbackInfo ci) {
		if (HoloSkyRenderer.INSTANCE.replacingThisFrame()) {
			ci.cancel();
		}
	}
}
