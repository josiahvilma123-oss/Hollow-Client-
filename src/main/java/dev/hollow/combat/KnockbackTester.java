package dev.hollow.combat;

import dev.hollow.Hollow;
import dev.hollow.HollowClient;
import dev.hollow.modules.KnockbackTesterModule;
import dev.hollow.practice.LocalServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Runtime of {@link KnockbackTesterModule}. Measurements are client-side; test controls run on the single-player server. */
public final class KnockbackTester {
	public static final KnockbackTester INSTANCE = new KnockbackTester();

	private int lastHurtTime;
	private boolean tracking;
	private Vec3 start = Vec3.ZERO;
	private int airborneTicks;
	private double horizontalSpeed = Double.NaN;
	private double verticalSpeed = Double.NaN;
	private double distance = Double.NaN;
	private int samples;
	private int pendingTestTicks;

	private KnockbackTester() {
	}

	public double horizontalSpeed() {
		return horizontalSpeed;
	}

	public double verticalSpeed() {
		return verticalSpeed;
	}

	public double distance() {
		return distance;
	}

	public int samples() {
		return samples;
	}

	public void clear() {
		horizontalSpeed = verticalSpeed = distance = Double.NaN;
		samples = 0;
		tracking = false;
	}

	public void tick() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		// A new hurt starts the animation at hurtDuration; the knockback velocity arrives the same tick.
		// Test hits don't play the hurt animation, so for those the velocity jump is the signal.
		Vec3 now = player.getDeltaMovement();
		boolean testArrived = pendingTestTicks > 0 && Math.hypot(now.x, now.z) > 0.2;
		if (pendingTestTicks > 0) {
			pendingTestTicks--;
		}
		if (testArrived) {
			pendingTestTicks = 0;
		}
		if (testArrived || player.hurtTime > lastHurtTime && player.hurtTime > 0) {
			Vec3 velocity = player.getDeltaMovement();
			horizontalSpeed = Math.hypot(velocity.x, velocity.z) * 20;
			verticalSpeed = velocity.y * 20;
			start = player.position();
			tracking = true;
			airborneTicks = 0;
			samples++;
		}
		lastHurtTime = player.hurtTime;
		if (tracking) {
			airborneTicks++;
			if (player.onGround() && airborneTicks > 2 || airborneTicks > 100) {
				Vec3 end = player.position();
				distance = Math.hypot(end.x - start.x, end.z - start.z);
				tracking = false;
			}
		}
	}

	/** Sets the local player's knockback resistance on the single-player server (0 removes the modifier). */
	public void applyResistance(double percent) {
		LocalServer.withPlayer(player -> {
			AttributeInstance attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (attribute == null) {
				return;
			}
			attribute.removeModifier(HollowClient.id("knockback_tester"));
			if (percent > 0) {
				attribute.addOrUpdateTransientModifier(new AttributeModifier(HollowClient.id("knockback_tester"), percent / 100.0,
					AttributeModifier.Operation.ADD_VALUE));
			}
		});
	}

	/** Knocks the local player back as if hit from the front (single-player only). */
	public void testHit() {
		double strength = Hollow.module(KnockbackTesterModule.class).testStrength.get();
		boolean ran = LocalServer.withPlayer(player -> {
			float yaw = player.getYRot() * (float) (Math.PI / 180);
			player.knockback(strength, -Math.sin(yaw), Math.cos(yaw));
			player.hurtMarked = true;
		});
		if (ran) {
			pendingTestTicks = 20;
		}
		if (!ran && Minecraft.getInstance().player != null) {
			Minecraft.getInstance().player.displayClientMessage(Component.literal("[Holo] Test hits are for single-player worlds"), true);
		}
	}
}
