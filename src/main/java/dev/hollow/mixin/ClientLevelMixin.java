package dev.hollow.mixin;

import dev.hollow.Hollow;
import dev.hollow.detection.BlockScanner;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
	/** Keeps Block Finder results in sync when the server (or the player) changes a block. */
	@Inject(method = "sendBlockUpdated", at = @At("HEAD"))
	private void hollow$blockChanged(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
		BlockScanner scanner = Hollow.scanner();
		if (scanner != null) {
			scanner.onBlockChanged((ClientLevel) (Object) this, pos, newState);
		}
	}
}
