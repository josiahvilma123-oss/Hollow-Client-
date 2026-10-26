package dev.hollow.hands;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.hollow.Hollow;
import dev.hollow.core.util.Colors;
import dev.hollow.mixin.LivingEntityAccessor;
import dev.hollow.modules.HollowHandsModule;
import dev.hollow.render.HollowPipelines;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Glue between {@code ItemInHandRenderer} and {@link HandAnimator}. Every method is a no-op
 * pass-through while Hollow Hands is disabled, so vanilla rendering is fully restored.
 */
public final class HandRenderHooks {
	private static final TrailBuffer MAIN_TRAIL = new TrailBuffer(64);
	private static final TrailBuffer OFF_TRAIL = new TrailBuffer(64);
	private static final Vector3f NEAR = new Vector3f();
	private static final Vector3f FAR = new Vector3f();
	private static final Matrix4f SCRATCH = new Matrix4f();

	private static boolean active;
	private static boolean hookReached;
	private static boolean currentRight = true;
	private static boolean currentMain = true;
	private static boolean currentSwinging;

	private HandRenderHooks() {
	}

	public static boolean hookReached() {
		return hookReached;
	}

	private static HollowHandsModule module() {
		return Hollow.module(HollowHandsModule.class);
	}

	public static void beginFrame(LocalPlayer player, float partialTick) {
		hookReached = true;
		HollowHandsModule module = module();
		active = module.isEnabled();
		if (!active) {
			MAIN_TRAIL.clear();
			OFF_TRAIL.clear();
			return;
		}
		HandAnimator animator = module.animator();
		Vec3 motion = player.getDeltaMovement();
		boolean moving = player.onGround() && motion.horizontalDistanceSqr() > 2.5e-4;
		double walk = player.avatarState().getBackwardsInterpolatedWalkDistance(partialTick);
		animator.beginFrame(System.nanoTime() / 1e9, moving, player.isSprinting(), walk);
	}

	public interface ArmRenderer {
		void render(float swing, float equip);
	}

	/** Wraps one hand's render with Hollow's transform and re-timed swing and equip values. */
	public static void renderArm(AbstractClientPlayer player, InteractionHand hand, float swing, float equip, PoseStack pose,
			ArmRenderer original) {
		if (!active) {
			original.render(swing, equip);
			return;
		}
		HollowHandsModule module = module();
		HandAnimator animator = module.animator();
		boolean main = hand == InteractionHand.MAIN_HAND;
		HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
		boolean right = arm == HumanoidArm.RIGHT;
		HandAnimator.Hand state = animator.hand(main);
		int duration = ((LivingEntityAccessor) player).hollow$getCurrentSwingDuration();
		float newSwing = (float) animator.swing(state, swing, duration);
		float newEquip = (float) animator.equip(state, equip);
		boolean using = player.isUsingItem() && player.getUsedItemHand() == hand;
		HandPose handPose = animator.pose(state, right, using);

		pose.pushPose();
		pose.translate(handPose.tx(), handPose.ty(), handPose.tz());
		float ax = (float) HandPose.anchorX(right);
		float ay = (float) HandPose.ANCHOR_Y;
		float az = (float) HandPose.ANCHOR_Z;
		pose.translate(ax, ay, az);
		pose.mulPose(Axis.YP.rotationDegrees((float) handPose.rotY()));
		pose.mulPose(Axis.XP.rotationDegrees((float) handPose.rotX()));
		pose.mulPose(Axis.ZP.rotationDegrees((float) handPose.rotZ()));
		float scale = (float) handPose.scale();
		pose.scale(scale, scale, scale);
		pose.translate(-ax, -ay, -az);
		currentRight = right;
		currentMain = main;
		currentSwinging = state.swinging();
		try {
			original.render(newSwing, newEquip);
		} finally {
			pose.popPose();
		}
	}

	/** Replaces vanilla's swing arc with one whose width follows the intensity setting. */
	public static boolean swingArm(float progress, PoseStack pose, HumanoidArm arm) {
		if (!active) {
			return false;
		}
		HandAnimator.SwingTransform t = module().animator().swingTransform(progress, arm == HumanoidArm.RIGHT);
		pose.translate(t.tx(), t.ty(), t.tz());
		pose.mulPose(Axis.YP.rotationDegrees((float) t.yaw1()));
		pose.mulPose(Axis.ZP.rotationDegrees((float) t.roll()));
		pose.mulPose(Axis.XP.rotationDegrees((float) t.pitch()));
		pose.mulPose(Axis.YP.rotationDegrees((float) t.yaw2()));
		return true;
	}

	/** Records the held item's position in camera space while it swings. */
	public static void captureTrail(PoseStack pose) {
		if (!active || !module().trails.get()) {
			return;
		}
		TrailBuffer trail = currentMain ? MAIN_TRAIL : OFF_TRAIL;
		if (!currentSwinging) {
			return;
		}
		SCRATCH.set(RenderSystem.getModelViewStack()).mul(pose.last().pose());
		float side = currentRight ? 1 : -1;
		SCRATCH.transformPosition(NEAR.set(0.05f * side, 0.12f, -0.05f));
		SCRATCH.transformPosition(FAR.set(0.25f * side, 0.62f, -0.32f));
		trail.add(module().animator().time(), NEAR.x, NEAR.y, NEAR.z, FAR.x, FAR.y, FAR.z);
	}

	/** Submits the trail ribbons alongside the hands. */
	public static void submitTrails(SubmitNodeCollector collector) {
		if (!active) {
			return;
		}
		HollowHandsModule module = module();
		if (!module.trails.get()) {
			MAIN_TRAIL.clear();
			OFF_TRAIL.clear();
			return;
		}
		double now = module.animator().time();
		double lifetime = module.trailLength.get() / 1000.0;
		MAIN_TRAIL.prune(now, lifetime);
		OFF_TRAIL.prune(now, lifetime);
		if (MAIN_TRAIL.size() < 2 && OFF_TRAIL.size() < 2) {
			return;
		}
		int color = module.trailColor.argb();
		PoseStack pose = new PoseStack();
		// Samples are stored in camera space; undo the view rotation the hand pass applies.
		pose.last().pose().set(new Matrix4f(RenderSystem.getModelViewStack()).invert());
		collector.submitCustomGeometry(pose, HollowPipelines.HAND_TRAIL_TYPE, (p, consumer) -> {
			drawRibbon(p, consumer, MAIN_TRAIL, now, lifetime, color);
			drawRibbon(p, consumer, OFF_TRAIL, now, lifetime, color);
		});
	}

	private static void drawRibbon(PoseStack.Pose pose, VertexConsumer consumer, TrailBuffer trail, double now, double lifetime, int color) {
		for (int i = 0; i + 1 < trail.size(); i++) {
			float a0 = (float) Math.max(0, 1 - (now - trail.time(i)) / lifetime);
			float a1 = (float) Math.max(0, 1 - (now - trail.time(i + 1)) / lifetime);
			int c0 = Colors.fade(color, a0 * a0);
			int c1 = Colors.fade(color, a1 * a1);
			int tip0 = Colors.fade(color, a0 * a0 * 0.35f);
			int tip1 = Colors.fade(color, a1 * a1 * 0.35f);
			consumer.addVertex(pose, trail.near(i, 0), trail.near(i, 1), trail.near(i, 2)).setColor(tip0);
			consumer.addVertex(pose, trail.far(i, 0), trail.far(i, 1), trail.far(i, 2)).setColor(c0);
			consumer.addVertex(pose, trail.far(i + 1, 0), trail.far(i + 1, 1), trail.far(i + 1, 2)).setColor(c1);
			consumer.addVertex(pose, trail.near(i + 1, 0), trail.near(i + 1, 1), trail.near(i + 1, 2)).setColor(tip1);
		}
	}
}
