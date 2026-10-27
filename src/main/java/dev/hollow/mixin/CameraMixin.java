package dev.hollow.mixin;

import dev.hollow.camera.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Places the camera at the Freecam position and marks it detached, so the player's own body is drawn. */
@Mixin(Camera.class)
abstract class CameraMixin implements Freecam.CameraAccess {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Inject(method = "setup", at = @At("TAIL"))
	private void hollow$freecam(Level level, Entity entity, boolean detachedView, boolean mirrored, float partialTick, CallbackInfo ci) {
		if (Freecam.INSTANCE.active()) {
			Freecam.INSTANCE.applyTo(this);
		}
	}

	@Override
	public void hollow$place(double x, double y, double z, float yaw, float pitch) {
		detached = true;
		setRotation(yaw, pitch);
		setPosition(x, y, z);
	}
}
