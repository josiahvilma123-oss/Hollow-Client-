package dev.hollow.camera;

import dev.hollow.Hollow;
import dev.hollow.input.KeyInput;
import dev.hollow.modules.FreecamModule;
import dev.hollow.policy.Automation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;

/**
 * State of the detached camera. Movement is integrated per frame (not per tick) so it is smooth
 * at any frame rate. The player's own movement input is withheld while active.
 */
public final class Freecam {
	public static final Freecam INSTANCE = new Freecam();

	private boolean active;
	private double x;
	private double y;
	private double z;
	private double vx;
	private double vy;
	private double vz;
	private float yaw;
	private float pitch;
	private float shownYaw;
	private float shownPitch;
	private Input input = Input.EMPTY;
	private long lastFrame = -1;
	private ClientLevel level;
	private boolean resetWasDown;

	private Freecam() {
	}

	public boolean active() {
		return active;
	}

	public Vec3 position() {
		return new Vec3(x, y, z);
	}

	public double speed() {
		return Math.sqrt(vx * vx + vy * vy + vz * vz);
	}

	private static FreecamModule module() {
		return Hollow.module(FreecamModule.class);
	}

	public void start() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null) {
			module().setEnabled(false);
			return;
		}
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 from = camera.isInitialized() ? camera.position() : player.getEyePosition();
		x = from.x;
		y = from.y;
		z = from.z;
		vx = vy = vz = 0;
		yaw = shownYaw = player.getYRot();
		pitch = shownPitch = player.getXRot();
		level = minecraft.level;
		lastFrame = -1;
		active = true;
	}

	public void stop() {
		active = false;
		input = Input.EMPTY;
		level = null;
	}

	/** Each tick: auto-exit on world change, death or a restricted server; handle the return key; block interactions. */
	public void tick(FreecamModule m) {
		if (!active) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level != level || player.isDeadOrDying() || Automation.detectionBlockedReason().isPresent()) {
			m.setEnabled(false);
			return;
		}
		boolean resetDown = KeyInput.gameplayInputActive() && KeyInput.isDown(m.resetKey.get());
		if (resetDown && !resetWasDown) {
			Vec3 eye = player.getEyePosition();
			x = eye.x;
			y = eye.y;
			z = eye.z;
			vx = vy = vz = 0;
		}
		resetWasDown = resetDown;
		if (m.blockInteractions.get()) {
			while (minecraft.options.keyAttack.consumeClick()) {
				// Swallowed: the click would act from the player, not the camera.
			}
			while (minecraft.options.keyUse.consumeClick()) {
			}
			while (minecraft.options.keyPickItem.consumeClick()) {
			}
			minecraft.options.keyAttack.setDown(false);
			minecraft.options.keyUse.setDown(false);
		}
	}

	/** Movement keys the player pressed this tick; the player itself receives none. */
	public void captureInput(Input keys) {
		input = keys;
	}

	public void turn(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = Math.max(-90f, Math.min(90f, pitch + (float) dy * 0.15f));
	}

	public void scroll(double amount) {
		FreecamModule m = module();
		m.speed.set(m.speed.get() * Math.pow(1.15, amount));
	}

	/** Called while the camera is set up each frame; moves the camera and returns its pose. */
	public void applyTo(CameraAccess camera) {
		FreecamModule m = module();
		long now = System.nanoTime();
		double dt = lastFrame < 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1e9);
		lastFrame = now;

		double forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
		double strafe = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
		double up = (input.jump() ? 1 : 0) - (input.shift() ? 1 : 0);
		double speed = m.speed.get() * (input.sprint() ? m.boost.get() : 1);
		double rad = Math.toRadians(yaw);
		double sin = Math.sin(rad);
		double cos = Math.cos(rad);
		double length = Math.sqrt(forward * forward + strafe * strafe);
		double norm = length > 1 ? 1 / length : 1;
		double tx = (-sin * forward + cos * strafe) * norm * speed;
		double tz = (cos * forward + sin * strafe) * norm * speed;
		double ty = up * speed;
		double blend = 1 - Math.exp(-dt * m.acceleration.get());
		vx += (tx - vx) * blend;
		vy += (ty - vy) * blend;
		vz += (tz - vz) * blend;
		x += vx * dt;
		y += vy * dt;
		z += vz * dt;

		LocalPlayer player = Minecraft.getInstance().player;
		if (player != null) {
			Vec3 eye = player.getEyePosition();
			Vec3 offset = new Vec3(x - eye.x, y - eye.y, z - eye.z);
			double max = m.maxDistance.get();
			if (offset.length() > max) {
				Vec3 clamped = eye.add(offset.normalize().scale(max));
				x = clamped.x;
				y = clamped.y;
				z = clamped.z;
			}
		}

		double smooth = m.smoothRotation.get();
		float follow = smooth <= 0 ? 1f : (float) (1 - Math.pow(smooth, dt * 60));
		shownYaw += (yaw - shownYaw) * follow;
		shownPitch += (pitch - shownPitch) * follow;
		camera.hollow$place(x, y, z, shownYaw, shownPitch);
	}

	/** Implemented by the camera mixin. */
	public interface CameraAccess {
		void hollow$place(double x, double y, double z, float yaw, float pitch);
	}
}
