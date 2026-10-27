package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.hollow.combat.AimAssist;
import dev.hollow.combat.KillAura;
import dev.hollow.camera.Freecam;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse hooks. Aim Assist adds its correction right after the mouse turns the player (only ever
 * called while the mouse is grabbed, so never in menus). Freecam redirects mouse turning to the
 * detached camera, and mouse-wheel scrolling to its speed.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
	@Shadow
	private double accumulatedDX;
	@Shadow
	private double accumulatedDY;

	@Unique
	private boolean hollow$mouseMoved;

	@Inject(method = "turnPlayer", at = @At("HEAD"))
	private void hollow$beforeTurn(double seconds, CallbackInfo ci) {
		hollow$mouseMoved = accumulatedDX != 0.0 || accumulatedDY != 0.0;
	}

	@Inject(method = "turnPlayer", at = @At("TAIL"))
	private void hollow$afterTurn(double seconds, CallbackInfo ci) {
		if (!Freecam.INSTANCE.active()) {
			AimAssist.INSTANCE.onMouseFrame(seconds, hollow$mouseMoved);
			KillAura.INSTANCE.onFrame(seconds);
		}
	}

	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void hollow$turn(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
		if (Freecam.INSTANCE.active()) {
			Freecam.INSTANCE.turn(yaw, pitch);
		} else {
			original.call(player, yaw, pitch);
		}
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void hollow$scroll(long window, double scrollX, double scrollY, CallbackInfo ci) {
		if (Freecam.INSTANCE.active() && net.minecraft.client.Minecraft.getInstance().screen == null && scrollY != 0) {
			Freecam.INSTANCE.scroll(scrollY);
			ci.cancel();
		}
	}
}
