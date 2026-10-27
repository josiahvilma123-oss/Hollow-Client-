package dev.hollow.combat;

import dev.hollow.core.aim.AimMath;
import dev.hollow.detection.EntityCategory;
import dev.hollow.detection.EntityClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Finds eligible targets for the combat modules. Only uses entities the client can see. */
public final class Targeting {
	/** Custom name given to practice targets, so they can be recognised client-side. */
	public static final String PRACTICE_NAME = "Holo Practice Target";

	private Targeting() {
	}

	public static boolean isPracticeTarget(Entity entity) {
		return entity.hasCustomName() && PRACTICE_NAME.equals(entity.getCustomName().getString());
	}

	/** Whether an entity passes the filters and is within range (measured to its hitbox). */
	public static boolean eligible(LocalPlayer player, Entity entity, TargetSettings settings, double range, boolean practiceOnly) {
		if (!(entity instanceof LivingEntity living) || entity == player || !living.isAlive() || living.isDeadOrDying()
				|| entity.isInvisible() || entity.isSpectator() || player.isPassengerOfSameVehicle(entity)) {
			return false;
		}
		if (practiceOnly) {
			if (!isPracticeTarget(entity)) {
				return false;
			}
		} else if (!isPracticeTarget(entity)) {
			EntityCategory category = EntityClassifier.classify(entity);
			boolean allowed = switch (category) {
				case PLAYERS -> settings.players.get();
				case HOSTILE -> settings.hostile.get();
				case NEUTRAL -> settings.neutral.get();
				case PASSIVE -> settings.passive.get();
				default -> false;
			};
			if (!allowed) {
				return false;
			}
			if (settings.ignoreTeammates.get() && player.isAlliedTo(entity)) {
				return false;
			}
		}
		if (distanceToBox(player.getEyePosition(), entity.getBoundingBox()) > range) {
			return false;
		}
		return !settings.requireSight.get() || player.hasLineOfSight(entity);
	}

	public static double distanceToBox(Vec3 point, AABB box) {
		double dx = Math.max(Math.max(box.minX - point.x, 0), point.x - box.maxX);
		double dy = Math.max(Math.max(box.minY - point.y, 0), point.y - box.maxY);
		double dz = Math.max(Math.max(box.minZ - point.z, 0), point.z - box.maxZ);
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** Angle in degrees from the player's view to the closest point of the entity's hitbox. */
	public static float angleTo(LocalPlayer player, Entity entity) {
		Vec3 eye = player.getEyePosition();
		Vec3 point = closestPointOnLook(player, entity.getBoundingBox(), 1f);
		return AimMath.angleBetween(player.getYRot(), player.getXRot(), AimMath.yawTo(point.x - eye.x, point.z - eye.z),
			AimMath.pitchTo(point.x - eye.x, point.y - eye.y, point.z - eye.z));
	}

	/**
	 * The best eligible target, or null. A still-valid current target is kept (so the choice
	 * doesn't flicker between two targets), unless another one is a lot better.
	 */
	public static LivingEntity select(LocalPlayer player, TargetSettings settings, double range, float fovDegrees, boolean practiceOnly,
			LivingEntity current) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return null;
		}
		float halfFov = fovDegrees / 2f;
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		double currentScore = Double.MAX_VALUE;
		for (Entity entity : level.entitiesForRendering()) {
			if (!eligible(player, entity, settings, range, practiceOnly)) {
				continue;
			}
			float angle = angleTo(player, entity);
			if (angle > halfFov) {
				continue;
			}
			LivingEntity living = (LivingEntity) entity;
			double score = switch (settings.priority.get()) {
				case CROSSHAIR -> angle;
				case NEAREST -> player.distanceTo(entity);
				case LOWEST_HEALTH -> living.getHealth();
			};
			if (living == current) {
				currentScore = score;
			}
			if (score < bestScore) {
				bestScore = score;
				best = living;
			}
		}
		if (current != null && currentScore != Double.MAX_VALUE && currentScore <= bestScore * 1.35 + 1) {
			return current;
		}
		return best;
	}

	/** Point of a (shrunken) box closest to the player's line of sight: the least camera movement needed. */
	public static Vec3 closestPointOnLook(LocalPlayer player, AABB box, float shrink) {
		AABB inner = box.deflate(box.getXsize() * 0.15 * shrink, box.getYsize() * 0.12 * shrink, box.getZsize() * 0.15 * shrink);
		Vec3 eye = player.getEyePosition();
		Vec3 center = inner.getCenter();
		Vec3 along = eye.add(player.getLookAngle().scale(eye.distanceTo(center)));
		return new Vec3(clamp(along.x, inner.minX, inner.maxX), clamp(along.y, inner.minY, inner.maxY), clamp(along.z, inner.minZ, inner.maxZ));
	}

	private static double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}
}
