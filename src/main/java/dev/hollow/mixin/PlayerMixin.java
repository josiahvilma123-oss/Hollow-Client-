package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.hollow.Hollow;
import dev.hollow.modules.SafeWalkModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Safe Walk: the local player keeps to the ground surface (the same edge check sneaking uses). */
@Mixin(Player.class)
abstract class PlayerMixin {
	@ModifyReturnValue(method = "isStayingOnGroundSurface", at = @At("RETURN"))
	private boolean hollow$safeWalk(boolean original) {
		if (original || Hollow.modules() == null || (Object) this != Minecraft.getInstance().player) {
			return original;
		}
		return Hollow.module(SafeWalkModule.class).active();
	}
}
