package dev.hollow.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hollow.hands.HandRenderHooks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
abstract class ItemLayerRenderStateMixin {
	/** Samples the held item's model space for Hollow Hands swing trails. */
	@Inject(method = "submit", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/block/model/ItemTransform;apply(ZLcom/mojang/blaze3d/vertex/PoseStack$Pose;)V",
		shift = At.Shift.AFTER))
	private void hollow$sampleTrail(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline, CallbackInfo ci) {
		HandRenderHooks.captureItemTransform(poseStack);
	}
}
