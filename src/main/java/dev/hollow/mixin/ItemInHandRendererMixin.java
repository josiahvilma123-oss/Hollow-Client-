package dev.hollow.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.hollow.hands.HandRenderHooks;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@Inject(method = "renderHandsWithItems", at = @At("HEAD"))
	private void hollow$beginFrame(float partialTick, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int light,
			CallbackInfo ci) {
		HandRenderHooks.beginFrame(player, partialTick);
	}

	@Inject(method = "renderHandsWithItems", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures()V"))
	private void hollow$submitTrails(float partialTick, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int light,
			CallbackInfo ci) {
		HandRenderHooks.submitTrails(collector);
	}

	@WrapMethod(method = "renderArmWithItem")
	private void hollow$renderArm(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
			float equip, PoseStack poseStack, SubmitNodeCollector collector, int light, Operation<Void> original) {
		HandRenderHooks.renderArm(player, hand, swing, equip, poseStack,
			(newSwing, newEquip) -> original.call(player, partialTick, pitch, hand, newSwing, stack, newEquip, poseStack, collector, light));
	}

	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void hollow$swingArm(ItemInHandRenderer self, float progress, PoseStack poseStack, int side, HumanoidArm arm, Operation<Void> original) {
		if (!HandRenderHooks.swingArm(progress, poseStack, arm)) {
			original.call(self, progress, poseStack, side, arm);
		}
	}

	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
	private void hollow$renderItem(ItemInHandRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
			SubmitNodeCollector collector, int light, Operation<Void> original) {
		HandRenderHooks.beforeHeldItem();
		try {
			original.call(self, entity, stack, context, poseStack, collector, light);
		} finally {
			HandRenderHooks.afterHeldItem();
		}
	}
}
