package dev.hollow.mixin;

import dev.hollow.menu.HoloTitleScreen;
import dev.hollow.menu.MenuScene;
import dev.hollow.menu.TitleScreenSwap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Out-of-world menus (world list, options, multiplayer...) draw the Holo scene where vanilla draws
 * its panorama, so the whole menu flow shares one backdrop. Vanilla's blur and darkening still
 * apply on top.
 */
@Mixin(Screen.class)
abstract class ScreenMixin {
	@Shadow
	public int width;
	@Shadow
	public int height;

	@Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
	private void hollow$menuScene(GuiGraphics g, float partialTick, CallbackInfo ci) {
		if (TitleScreenSwap.sceneEnabled() && !((Object) this instanceof HoloTitleScreen)) {
			MenuScene.render(g, width, height, -1, -1, -1);
			ci.cancel();
		}
	}
}
