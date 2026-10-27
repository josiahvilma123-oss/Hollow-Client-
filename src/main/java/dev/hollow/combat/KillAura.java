package dev.hollow.combat;

import dev.hollow.Hollow;
import dev.hollow.core.aim.AimMath;
import dev.hollow.core.util.Colors;
import dev.hollow.input.KeyInput;
import dev.hollow.modules.KillAuraModule;
import dev.hollow.policy.Automation;
import dev.hollow.render.OverlayProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Runtime of {@link KillAuraModule}: target choice and attacks per tick, view turning per frame. */
public final class KillAura implements OverlayProvider {
	public static final KillAura INSTANCE = new KillAura();
	/** Within this angle of the target's closest hitbox point, the player counts as looking at it. */
	private static final float ON_TARGET_DEGREES = 4f;

	private LivingEntity target;
	private int attacks;

	private KillAura() {
	}

	public LivingEntity target() {
		return target;
	}

	public int attacks() {
		return attacks;
	}

	public void reset() {
		target = null;
	}

	private static KillAuraModule module() {
		return Hollow.module(KillAuraModule.class);
	}

	private static boolean mayAct(KillAuraModule m, LocalPlayer player) {
		return m.isEnabled() && player != null && Automation.allowed() && KeyInput.gameplayInputActive() && player.isAlive()
			&& !player.isSpectator() && !(m.pauseWhileUsing.get() && player.isUsingItem())
			&& !(m.weaponOnly.get() && !Attacks.holdingWeapon(player));
	}

	public void tick(KillAuraModule m) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!mayAct(m, player) || minecraft.gameMode == null) {
			target = null;
			return;
		}
		double range = Math.min(m.range.get(), player.entityInteractionRange());
		target = Targeting.select(player, m.targets, range, m.fov.getFloat(), false, target);
		if (target == null || !Attacks.charged(player, m.charge.get()) || !Attacks.inReach(player, target)) {
			return;
		}
		if (m.turnToTarget.get() && Targeting.angleTo(player, target) > ON_TARGET_DEGREES) {
			return;
		}
		Attacks.attack(minecraft, player, target);
		attacks++;
	}

	/** Per frame: turns the view towards the target at the configured speed. */
	public void onFrame(double seconds) {
		KillAuraModule m = module();
		LocalPlayer player = Minecraft.getInstance().player;
		if (target == null || !m.turnToTarget.get() || !mayAct(m, player) || !target.isAlive()) {
			return;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 point = Targeting.closestPointOnLook(player, target.getBoundingBox(), 1f);
		float yawError = AimMath.wrapDegrees(AimMath.yawTo(point.x - eye.x, point.z - eye.z) - player.getYRot());
		float pitchError = AimMath.pitchTo(point.x - eye.x, point.y - eye.y, point.z - eye.z) - player.getXRot();
		float yaw = AimMath.step(yawError, seconds, 0.06, 1.0, m.turnSpeed.get());
		float pitch = AimMath.step(pitchError, seconds, 0.06, 1.0, m.turnSpeed.get());
		if (yaw != 0f || pitch != 0f) {
			player.turn(yaw / 0.15, pitch / 0.15);
		}
	}

	@Override
	public void collect(Sink sink, float partialTick) {
		if (target == null || Hollow.modules() == null || !module().isEnabled() || !module().marker.get() || target.isRemoved()) {
			return;
		}
		Vec3 offset = target.getPosition(partialTick).subtract(target.position());
		sink.outline(target.getBoundingBox().move(offset).inflate(0.05), Colors.withAlpha(0xFFE0405E, 220), 2f, false);
	}
}
