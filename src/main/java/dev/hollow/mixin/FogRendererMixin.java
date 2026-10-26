package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hollow.sky.HoloSkyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
	/** Lets distant terrain fade into the Holo sky's horizon instead of vanilla's fog color. */
	@ModifyReturnValue(method = "computeFogColor", at = @At("RETURN"))
	private Vector4f hollow$tintFog(Vector4f original, @Local(argsOnly = true) Camera camera) {
		if (camera.getFluidInCamera() != FogType.NONE) {
			return original;
		}
		return HoloSkyRenderer.INSTANCE.tintFog(original);
	}
}
