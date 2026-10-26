package dev.hollow.hands;

import dev.hollow.core.anim.Easing;
import dev.hollow.core.anim.Smooth;

/**
 * Minecraft-independent state machine for first-person hand animation. The render hook feeds
 * it vanilla values each frame and applies the returned {@link HandPose}.
 */
public final class HandAnimator {
	/** Values read from the module settings each frame. */
	public static final class Config {
		public double swingSpeed = 1.0;
		public double swingIntensity = 1.0;
		public Easing swingEasing = Easing.SINE;
		public double bob = 0.5;
		public double sprint = 0.5;
		public double idleSway = 0.5;
		public double stateSmoothing = 0.15;
		public EquipStyle equipStyle = EquipStyle.SMOOTH;
		public double useTransitionMillis = 150;
		public double offsetX;
		public double offsetY;
		public double offsetZ;
		public double scale = 1.0;
		public double rotX;
		public double rotY;
		public double rotZ;
	}

	/** Per-hand state. */
	public static final class Hand {
		private double lastVanillaSwing;
		private double swingStart = -1;
		private double swingDuration = 0.3;
		private double swingProgress;
		private final Smooth equip = new Smooth(0);
		private final Smooth use = new Smooth(0);
		private boolean equipInitialized;

		public double swingProgress() {
			return swingProgress;
		}

		public boolean swinging() {
			return swingStart >= 0;
		}
	}

	private final Config config = new Config();
	private final Hand main = new Hand();
	private final Hand off = new Hand();
	private final Smooth walkWeight = new Smooth(0);
	private final Smooth sprintWeight = new Smooth(0);
	private double time;
	private double lastFrame = -1;
	private double delta;
	private double walkDistance;

	public Config config() {
		return config;
	}

	public Hand hand(boolean mainHand) {
		return mainHand ? main : off;
	}

	public double delta() {
		return delta;
	}

	public double time() {
		return time;
	}

	/**
	 * Advances shared state once per rendered frame.
	 *
	 * @param nowSeconds monotonic time in seconds
	 * @param moving     the player is walking on the ground
	 * @param sprinting  the player is sprinting
	 * @param walkDistance the game's interpolated walk distance, so hand bob stays in step with view bob
	 */
	public void beginFrame(double nowSeconds, boolean moving, boolean sprinting, double walkDistance) {
		delta = lastFrame < 0 ? 0 : Math.min(0.1, Math.max(0, nowSeconds - lastFrame));
		lastFrame = nowSeconds;
		time = nowSeconds;
		this.walkDistance = walkDistance;
		double smoothing = Math.max(0.02, config.stateSmoothing);
		walkWeight.target(moving ? 1 : 0);
		sprintWeight.target(moving && sprinting ? 1 : 0);
		walkWeight.update(delta, smoothing);
		sprintWeight.update(delta, smoothing * 1.4);
	}

	public double walkWeight() {
		return walkWeight.value();
	}

	public double sprintWeight() {
		return sprintWeight.value();
	}

	/**
	 * Re-times a swing on Hollow's own clock so speed and easing are configurable while the
	 * game's actual attack timing stays untouched.
	 *
	 * @param vanillaProgress the game's swing progress for this hand (0 when idle)
	 * @param vanillaDurationTicks how long the game's swing lasts
	 */
	public double swing(Hand hand, double vanillaProgress, int vanillaDurationTicks) {
		boolean started = vanillaProgress > 0 && hand.lastVanillaSwing == 0
			|| vanillaProgress < hand.lastVanillaSwing - 0.25;
		hand.lastVanillaSwing = vanillaProgress;
		if (started) {
			hand.swingStart = time;
			hand.swingDuration = Math.max(0.05, vanillaDurationTicks * 0.05 / Math.max(0.1, config.swingSpeed));
		}
		if (hand.swingStart < 0) {
			hand.swingProgress = 0;
			return 0;
		}
		double linear = (time - hand.swingStart) / hand.swingDuration;
		if (linear >= 1) {
			hand.swingStart = -1;
			hand.swingProgress = 0;
			return 0;
		}
		hand.swingProgress = config.swingEasing.ease(linear);
		// Vanilla's swing curves expect values in [0, 1); overshooting easings are clamped here
		// and expressed through intensity instead.
		return Math.max(0, Math.min(0.999, hand.swingProgress));
	}

	/** Equip progress: 0 = held up, 1 = fully lowered. */
	public double equip(Hand hand, double vanillaEquip) {
		if (!hand.equipInitialized) {
			hand.equip.snap(vanillaEquip);
			hand.equipInitialized = true;
		}
		hand.equip.target(vanillaEquip);
		hand.equip.update(delta, 0.045);
		return switch (config.equipStyle) {
			case VANILLA -> vanillaEquip;
			case SMOOTH -> Easing.SINE.ease(hand.equip.value());
			case SLIDE -> hand.equip.value() * 0.2;
			case NONE -> 0;
		};
	}

	/** Builds the extra transform applied around vanilla's own hand transforms. */
	public HandPose pose(Hand hand, boolean rightSide, boolean usingItem) {
		int side = rightSide ? 1 : -1;
		double tx = config.offsetX * side;
		double ty = config.offsetY;
		double tz = config.offsetZ;
		double rx = config.rotX;
		double ry = config.rotY * side;
		double rz = config.rotZ * side;

		double walk = walkWeight.value();
		double sprint = sprintWeight.value();

		// Walking bob: a gentle figure-eight in step with the game's view bob.
		double phase = walkDistance * Math.PI;
		double bobAmount = config.bob * walk * (1 + 0.6 * sprint);
		tx += Math.sin(phase) * 0.012 * bobAmount;
		ty += -Math.abs(Math.cos(phase)) * 0.016 * bobAmount + 0.008 * bobAmount;
		rz += Math.sin(phase) * 1.6 * bobAmount;

		// Sprinting lowers and tilts the item forward.
		ty -= 0.035 * sprint * config.sprint;
		tz += 0.02 * sprint * config.sprint;
		rx -= 6.0 * sprint * config.sprint;

		// Idle breathing sway.
		double idle = (1 - walk) * config.idleSway;
		tx += Math.sin(time * 1.1) * 0.004 * idle * side;
		ty += Math.sin(time * 1.7) * 0.003 * idle;

		// Smooth item-use transitions: a small dip that peaks mid-transition, masking the
		// instant snap between idle and use poses.
		double useTau = Math.max(0.01, config.useTransitionMillis / 1000.0 / 3.0);
		hand.use.target(usingItem ? 1 : 0);
		hand.use.update(delta, useTau);
		double b = hand.use.value();
		ty -= 0.06 * 4 * b * (1 - b);

		// Side slide equip style moves the item out sideways instead of down.
		if (config.equipStyle == EquipStyle.SLIDE) {
			tx += side * 0.45 * hand.equip.value();
		}

		return new HandPose(tx, ty, tz, rx, ry, rz, config.scale);
	}

	/**
	 * Swing transform with configurable intensity. Mirrors vanilla's arc at intensity 1 and
	 * returns translation then rotation components in the order vanilla applies them.
	 */
	public SwingTransform swingTransform(double progress, boolean rightSide) {
		int side = rightSide ? 1 : -1;
		double intensity = config.swingIntensity;
		double sqrt = Math.sqrt(progress);
		double tx = side * -0.4 * Math.sin(sqrt * Math.PI) * intensity;
		double ty = 0.2 * Math.sin(sqrt * Math.PI * 2) * intensity;
		double tz = -0.2 * Math.sin(progress * Math.PI) * intensity;
		double g = Math.sin(progress * progress * Math.PI);
		double h = Math.sin(sqrt * Math.PI);
		double yaw1 = side * (45.0 + g * -20.0 * intensity);
		double roll = side * h * -20.0 * intensity;
		double pitch = h * -80.0 * intensity;
		double yaw2 = side * -45.0;
		return new SwingTransform(tx, ty, tz, yaw1, roll, pitch, yaw2);
	}

	public record SwingTransform(double tx, double ty, double tz, double yaw1, double roll, double pitch, double yaw2) {
	}
}
