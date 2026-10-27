package dev.hollow.combat;

import dev.hollow.Hollow;
import dev.hollow.modules.ReachDisplayModule;
import dev.hollow.render.OverlayProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Measures distances along the view (further than the game's own pick range) and draws reach rings. */
public final class ReachMeter implements OverlayProvider {
	public static final ReachMeter INSTANCE = new ReachMeter();
	private static final int RING_SEGMENTS = 48;

	private double targetDistance = Double.NaN;
	private boolean targetIsEntity;
	private String targetName = "";
	private double lastHitDistance = Double.NaN;

	private ReachMeter() {
	}

	public double targetDistance() {
		return targetDistance;
	}

	public boolean targetIsEntity() {
		return targetIsEntity;
	}

	public String targetName() {
		return targetName;
	}

	public double lastHitDistance() {
		return lastHitDistance;
	}

	/** Called when the local player attacks an entity. */
	public void onAttack(LocalPlayer player, Entity entity) {
		lastHitDistance = Targeting.distanceToBox(player.getEyePosition(), entity.getBoundingBox());
	}

	/** Each tick: what is under the crosshair up to the measuring range, and how far away. */
	public void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (Hollow.modules() == null || !Hollow.module(ReachDisplayModule.class).isEnabled() || player == null || minecraft.level == null) {
			targetDistance = Double.NaN;
			return;
		}
		double range = Hollow.module(ReachDisplayModule.class).measureRange.get();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		BlockHitResult block = minecraft.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		double blockDistance = block.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : eye.distanceTo(block.getLocation());
		Vec3 entityEnd = blockDistance < Double.MAX_VALUE ? block.getLocation() : end;
		AABB search = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1);
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, entityEnd, search,
			e -> !e.isSpectator() && e.isPickable() && !e.isInvisible(), Math.min(blockDistance, range) * Math.min(blockDistance, range));
		if (entity != null) {
			targetDistance = eye.distanceTo(entity.getLocation());
			targetIsEntity = true;
			targetName = entity.getEntity().getDisplayName().getString();
		} else if (blockDistance < Double.MAX_VALUE) {
			targetDistance = blockDistance;
			targetIsEntity = false;
			targetName = minecraft.level.getBlockState(block.getBlockPos()).getBlock().getName().getString();
		} else {
			targetDistance = Double.NaN;
			targetName = "";
		}
	}

	@Override
	public void collect(Sink sink, float partialTick) {
		if (Hollow.modules() == null) {
			return;
		}
		ReachDisplayModule m = Hollow.module(ReachDisplayModule.class);
		LocalPlayer player = Minecraft.getInstance().player;
		if (!m.isEnabled() || player == null) {
			return;
		}
		Vec3 feet = player.getPosition(partialTick).add(0, 0.05, 0);
		if (m.entityRing.get()) {
			ring(sink, feet, player.entityInteractionRange(), m.entityRingColor.argb());
		}
		if (m.blockRing.get()) {
			ring(sink, feet, player.blockInteractionRange(), m.blockRingColor.argb());
		}
	}

	private static void ring(Sink sink, Vec3 center, double radius, int color) {
		Vec3 previous = null;
		for (int i = 0; i <= RING_SEGMENTS; i++) {
			double angle = i * Math.PI * 2 / RING_SEGMENTS;
			Vec3 point = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
			if (previous != null) {
				sink.line(previous, point, color, 2f);
			}
			previous = point;
		}
	}
}
