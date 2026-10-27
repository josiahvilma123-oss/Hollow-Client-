package dev.hollow.combat;

import dev.hollow.Hollow;
import dev.hollow.core.aim.AimMath;
import dev.hollow.input.KeyInput;
import dev.hollow.modules.AimAssistModule;
import dev.hollow.modules.AimAssistModule.Activation;
import dev.hollow.policy.Automation;
import dev.hollow.practice.LocalServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Runtime of {@link AimAssistModule}. Targets are chosen each tick; the correction is applied
 * each frame right after Minecraft applies the mouse, scaled by the frame time so it is equally
 * smooth at any frame rate.
 */
public final class AimAssist {
	public static final AimAssist INSTANCE = new AimAssist();
	/** Mouse movement within this window counts as "the player is aiming". */
	private static final long MOUSE_GRACE_NANOS = 150_000_000L;

	public enum State {
		OFF("Off"),
		BLOCKED("Blocked here"),
		WAITING("Waiting"),
		NO_TARGET("No target"),
		ACTIVE("Assisting");

		private final String label;

		State(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	private LivingEntity target;
	private boolean toggledOn;
	private boolean activationWasDown;
	private boolean attackWasDown;
	private long lastMouseMove;
	private State state = State.OFF;
	private int practiceSwings;
	private int practiceHits;

	private AimAssist() {
	}

	private static AimAssistModule module() {
		return Hollow.module(AimAssistModule.class);
	}

	public LivingEntity target() {
		return target;
	}

	public State state() {
		return state;
	}

	public int practiceSwings() {
		return practiceSwings;
	}

	public int practiceHits() {
		return practiceHits;
	}

	public void reset() {
		target = null;
		toggledOn = false;
		state = State.OFF;
	}

	public void resetStats() {
		practiceSwings = 0;
		practiceHits = 0;
	}

	/** Each client tick: activation keys, practice counting and target choice. */
	public void tick(AimAssistModule m) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!m.isEnabled() || player == null) {
			reset();
			return;
		}
		boolean input = KeyInput.gameplayInputActive();
		boolean keyDown = input && KeyInput.isDown(m.activationKey.get());
		if (m.activation.get() == Activation.TOGGLE_KEY && keyDown && !activationWasDown) {
			toggledOn = !toggledOn;
		}
		activationWasDown = keyDown;
		boolean attackDown = input && minecraft.options.keyAttack.isDown();
		if (m.practiceMode.get() && attackDown && !attackWasDown) {
			practiceSwings++;
		}
		attackWasDown = attackDown;

		if (!Automation.allowed()) {
			state = State.BLOCKED;
			target = null;
			return;
		}
		boolean activated = switch (m.activation.get()) {
			case ALWAYS -> true;
			case WHILE_ATTACKING -> attackDown;
			case HOLD_KEY -> keyDown;
			case TOGGLE_KEY -> toggledOn;
		};
		if (!activated || !input || !player.isAlive() || player.isSpectator()) {
			state = State.WAITING;
			target = null;
			return;
		}
		target = Targeting.select(player, m.targets, m.range.get(), m.fov.getFloat(), m.practiceMode.get(), target);
		state = target == null ? State.NO_TARGET : State.ACTIVE;
	}

	/** Called by the practice-mode attack hook. */
	public void onAttack(Entity entity) {
		if (module().isEnabled() && module().practiceMode.get() && Targeting.isPracticeTarget(entity)) {
			practiceHits++;
		}
	}

	/**
	 * Called every frame right after the mouse turned the player.
	 *
	 * @param seconds   time since the previous frame
	 * @param mouseMoved whether the player moved the mouse this frame
	 */
	public void onMouseFrame(double seconds, boolean mouseMoved) {
		long now = System.nanoTime();
		if (mouseMoved) {
			lastMouseMove = now;
		}
		AimAssistModule m = module();
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!m.isEnabled() || state != State.ACTIVE || target == null || player == null || !KeyInput.gameplayInputActive()) {
			return;
		}
		if (m.requireMouseMovement.get() && now - lastMouseMove > MOUSE_GRACE_NANOS) {
			return;
		}
		if (m.pauseWhileUsing.get() && player.isUsingItem()) {
			return;
		}
		if (!target.isAlive() || target.isRemoved()) {
			target = null;
			return;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 point = aimPoint(m, player, target, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true));
		float targetYaw = AimMath.yawTo(point.x - eye.x, point.z - eye.z);
		float targetPitch = AimMath.pitchTo(point.x - eye.x, point.y - eye.y, point.z - eye.z);
		double speed = m.maxSpeed.get();
		if (m.sensitivityAware.get()) {
			speed *= AimMath.sensitivityFactor(minecraft.options.sensitivity().get());
		}
		float yawStep = 0f;
		float pitchStep = 0f;
		if (m.horizontal.get()) {
			yawStep = AimMath.step(AimMath.wrapDegrees(targetYaw - player.getYRot()), seconds, m.smoothing.get(),
				m.strength.get() * m.horizontalStrength.get(), speed);
		}
		if (m.vertical.get()) {
			pitchStep = AimMath.step(targetPitch - player.getXRot(), seconds, m.smoothing.get(), m.strength.get() * m.verticalStrength.get(),
				speed);
		}
		if (yawStep != 0f || pitchStep != 0f) {
			// Entity.turn works in mouse units of 0.15 degrees and applies the usual pitch clamp.
			player.turn(yawStep / 0.15, pitchStep / 0.15);
		}
	}

	private static Vec3 aimPoint(AimAssistModule m, LocalPlayer player, LivingEntity entity, float partialTick) {
		Vec3 offset = entity.getPosition(partialTick).subtract(entity.position());
		AABB box = entity.getBoundingBox().move(offset);
		return switch (m.aimPoint.get()) {
			case CLOSEST -> Targeting.closestPointOnLook(player, box, 1f);
			case BODY -> box.getCenter();
			case HEAD -> new Vec3(box.getCenter().x, box.minY + entity.getEyeHeight() - 0.1, box.getCenter().z);
		};
	}

	public void spawnPracticeTargets() {
		String name = Targeting.PRACTICE_NAME;
		String still = "{NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,CustomName:\"" + name + "\"}";
		String wander = "{Invulnerable:1b,PersistenceRequired:1b,CustomName:\"" + name + "\"}";
		boolean ran = LocalServer.runAsPlayer(
			"summon minecraft:zombie ^-3 ^ ^6 " + still,
			"summon minecraft:zombie ^3 ^ ^7 " + still,
			"summon minecraft:zombie ^ ^2 ^9 " + still,
			"summon minecraft:pig ^-2 ^ ^4 " + wander,
			"summon minecraft:pig ^2 ^ ^5 " + wander,
			"summon minecraft:chicken ^ ^ ^6 " + wander);
		report(ran ? "Spawned practice targets" : "Practice targets are for single-player worlds");
		if (ran) {
			module().practiceMode.set(true);
		}
	}

	public void clearPracticeTargets() {
		boolean ran = LocalServer.runAsPlayer("kill @e[name=\"" + Targeting.PRACTICE_NAME + "\"]");
		report(ran ? "Cleared practice targets" : "Practice targets are for single-player worlds");
	}

	private static void report(String message) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.literal("[Holo] " + message), true);
		}
	}
}
