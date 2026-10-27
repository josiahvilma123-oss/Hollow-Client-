package dev.hollow.perf;

import dev.hollow.Hollow;
import dev.hollow.modules.FpsBoostModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

/** Decisions behind {@link FpsBoostModule}, called from the render hooks. Cheap: no allocation per call. */
public final class FpsBoost {
	private static int particleCount;

	private FpsBoost() {
	}

	private static FpsBoostModule module() {
		return Hollow.modules() == null ? null : Hollow.module(FpsBoostModule.class);
	}

	/** Whether an entity should be skipped by the renderer because it is too far away. */
	public static boolean cull(Entity entity, double cameraX, double cameraY, double cameraZ) {
		FpsBoostModule m = module();
		if (m == null || !m.isEnabled() || !m.entityCulling.get()) {
			return false;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (entity instanceof Player || entity == minecraft.getCameraEntity() || entity.isCurrentlyGlowing()
				|| minecraft.player != null && (entity == minecraft.player.getVehicle() || entity.hasPassenger(minecraft.player))) {
			return false;
		}
		if (m.keepNamed.get() && entity.hasCustomName()) {
			return false;
		}
		if (entity.getBbWidth() > 3 || entity.getBbHeight() > 4) {
			return false;
		}
		double limit;
		if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
			limit = m.itemDistance.get();
		} else if (entity instanceof LivingEntity) {
			limit = m.mobDistance.get();
		} else {
			limit = m.otherDistance.get();
		}
		return entity.distanceToSqr(cameraX, cameraY, cameraZ) > limit * limit;
	}

	/** Updated once per tick by the particle hook. */
	public static void particleCount(int count) {
		particleCount = count;
	}

	public static int particleCount() {
		return particleCount;
	}

	/** Whether a new particle should be skipped because the cap is reached. */
	public static boolean dropParticle(int pending) {
		FpsBoostModule m = module();
		return m != null && m.isEnabled() && m.particleCap.get() && particleCount + pending >= m.maxParticles.getInt();
	}

	/** Frame limit while the window is in the background, or -1 for no change. */
	public static int backgroundLimit() {
		FpsBoostModule m = module();
		if (m == null || !m.isEnabled() || !m.backgroundLimit.get() || Minecraft.getInstance().isWindowActive()) {
			return -1;
		}
		return m.backgroundFps.getInt();
	}
}
