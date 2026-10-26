package dev.hollow.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.hollow.render.ScreenProjector;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
	/** Captures this frame's view and projection so HUD labels line up with world overlays. */
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void hollow$captureMatrices(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera,
			Matrix4f view, Matrix4f projection, Matrix4f cullProjection, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
		Vec3 position = camera.position();
		ScreenProjector.INSTANCE.capture(view, projection, position.x, position.y, position.z, deltaTracker.getGameTimeDeltaPartialTick(false));
	}
}
