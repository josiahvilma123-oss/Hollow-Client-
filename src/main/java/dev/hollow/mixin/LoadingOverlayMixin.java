package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hollow.Hollow;
import dev.hollow.menu.HoloLoadingScreen;
import dev.hollow.modules.LoadingScreenModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restyles Minecraft's loading overlay as the Holo loading screen. The overlay's own logic is
 * untouched: it still decides when loading is done and when to hand over, using real progress.
 * Only the drawing is swapped (background color, logo, progress bar), and with the Loading
 * Screen module off every hook passes straight through.
 */
@Mixin(LoadingOverlay.class)
abstract class LoadingOverlayMixin {
	@Shadow
	@Final
	private ReloadInstance reload;
	@Shadow
	@Final
	private boolean fadeIn;
	@Shadow
	private float currentProgress;
	@Shadow
	private long fadeOutStart;

	@Unique
	private HoloLoadingScreen hollow$screen;
	@Unique
	private boolean hollow$handedOff;

	@Unique
	private HoloLoadingScreen hollow$screen() {
		if (hollow$screen == null) {
			hollow$screen = new HoloLoadingScreen(fadeIn);
		}
		return hollow$screen;
	}

	@ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/IntSupplier;getAsInt()I"))
	private int hollow$background(int original) {
		return HoloLoadingScreen.active() ? 0xFF06040A : original;
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIIIII)V"))
	private void hollow$logo(GuiGraphics g, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v, int width, int height,
			int uWidth, int vHeight, int textureWidth, int textureHeight, int color, Operation<Void> original) {
		if (!HoloLoadingScreen.active()) {
			original.call(g, pipeline, texture, x, y, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight, color);
			return;
		}
		// Vanilla draws the logo in two halves; the first call draws the whole Holo scene, the second is dropped.
		if (v == 0f) {
			hollow$screen().renderScene(g, g.guiWidth(), g.guiHeight(), currentProgress, ARGB.alpha(color) / 255f);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
	private void hollow$progress(LoadingOverlay overlay, GuiGraphics g, int x1, int y1, int x2, int y2, float alpha, Operation<Void> original) {
		if (!HoloLoadingScreen.active()) {
			original.call(overlay, g, x1, y1, x2, y2, alpha);
			return;
		}
		hollow$screen().renderProgress(g, g.guiWidth(), g.guiHeight(), currentProgress, reload.isDone(), alpha);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void hollow$quickHandoff(CallbackInfo ci) {
		if (fadeOutStart > -1L && !hollow$handedOff) {
			hollow$handedOff = true;
			if (HoloLoadingScreen.active() && Hollow.module(LoadingScreenModule.class).quickHandoff.get()) {
				// Vanilla holds the finished screen for a second, then fades for another. Skip the hold
				// and most of the fade; loading itself was already complete when fadeOutStart was set.
				fadeOutStart -= 1650L;
			}
		}
	}
}
