package dev.hollow.core.aim;

/**
 * Angle maths for aim assistance, independent of Minecraft. Yaw follows Minecraft's convention
 * (0 = +z, 90 = -x); pitch is positive looking down.
 */
public final class AimMath {
	private AimMath() {
	}

	public static float wrapDegrees(float degrees) {
		float wrapped = degrees % 360f;
		if (wrapped >= 180f) {
			wrapped -= 360f;
		}
		if (wrapped < -180f) {
			wrapped += 360f;
		}
		return wrapped;
	}

	/** Yaw that looks along (dx, dz). */
	public static float yawTo(double dx, double dz) {
		return (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
	}

	/** Pitch that looks along (dx, dy, dz). */
	public static float pitchTo(double dx, double dy, double dz) {
		return (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
	}

	/** Angle in degrees between the current view and a target direction. */
	public static float angleBetween(float yaw, float pitch, float targetYaw, float targetPitch) {
		float dy = wrapDegrees(targetYaw - yaw);
		float dp = targetPitch - pitch;
		return (float) Math.sqrt(dy * dy + dp * dp);
	}

	/**
	 * The rotation to apply this frame towards a target, in degrees.
	 *
	 * @param error           remaining angle to the target (degrees, wrapped)
	 * @param seconds         frame time
	 * @param smoothing       time constant in seconds; larger is gentler
	 * @param strength        0-1 share of the error the assist is allowed to close
	 * @param maxDegreesPerSecond speed cap, so the camera never snaps
	 */
	public static float step(float error, double seconds, double smoothing, double strength, double maxDegreesPerSecond) {
		if (seconds <= 0 || strength <= 0 || error == 0f) {
			return 0f;
		}
		double fraction = 1.0 - Math.exp(-seconds / Math.max(0.01, smoothing));
		double move = error * strength * fraction;
		double cap = maxDegreesPerSecond * seconds;
		return (float) Math.max(-cap, Math.min(cap, move));
	}

	/**
	 * Minecraft's mouse multiplier for a sensitivity option value (0-1). The default option
	 * value (0.5) gives exactly 1, so assist speeds can scale with the player's own sensitivity.
	 */
	public static double sensitivityFactor(double option) {
		double f = option * 0.6 + 0.2;
		return f * f * f * 8.0;
	}
}
