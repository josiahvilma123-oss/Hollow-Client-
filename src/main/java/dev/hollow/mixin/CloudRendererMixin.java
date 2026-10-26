package dev.hollow.mixin;

import dev.hollow.sky.HoloSkyRenderer;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
abstract class CloudRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void hollow$hideClouds(int color, CloudStatus status, float height, Vec3 camera, long ticks, float partialTick, CallbackInfo ci) {
		if (HoloSkyRenderer.INSTANCE.hideClouds()) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int hollow$tintClouds(int color) {
		return HoloSkyRenderer.INSTANCE.tintClouds(color);
	}
}
