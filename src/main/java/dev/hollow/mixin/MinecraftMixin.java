package dev.hollow.mixin;

import dev.hollow.menu.TitleScreenSwap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps Minecraft's title screen for the Holo one whenever the game opens it (start-up, disconnect, quit to title). */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen hollow$titleScreen(Screen screen) {
		return TitleScreenSwap.swap(screen);
	}
}
