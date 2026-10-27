package dev.hollow.mixin;

import dev.hollow.bridge.BridgeInput;
import dev.hollow.camera.Freecam;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Movement input hooks: Freecam takes the movement keys for the camera (the player stands
 * still), and bridging assistance may hold sneak at a block edge.
 */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void hollow$movement(CallbackInfo ci) {
		if (Freecam.INSTANCE.active()) {
			Freecam.INSTANCE.captureInput(keyPresses);
			keyPresses = Input.EMPTY;
			moveVector = Vec2.ZERO;
			return;
		}
		if (BridgeInput.forceSneak() && !keyPresses.shift()) {
			Input k = keyPresses;
			keyPresses = new Input(k.forward(), k.backward(), k.left(), k.right(), k.jump(), true, k.sprint());
		}
	}
}
