package dev.hollow.blur;

/** Pure math behind the camera motion blur so it can be tuned and tested without a GPU. */
public final class MotionBlurMath {
	/**
	 * Hard ceiling on history weight. Above ~0.9 the 8-bit color buffer cannot converge
	 * (per-frame changes round to zero) and leaves ghosts, so very high frame rates get a
	 * slightly shorter trail instead.
	 */
	public static final double MAX_BLEND = 0.88;

	private MotionBlurMath() {
	}

	/** Wraps a yaw difference into [-180, 180). */
	public static double wrapDegrees(double degrees) {
		double wrapped = degrees % 360.0;
		if (wrapped >= 180.0) {
			wrapped -= 360.0;
		}
		if (wrapped < -180.0) {
			wrapped += 360.0;
		}
		return wrapped;
	}

	/** Camera angular speed in degrees per second. */
	public static double angularSpeed(double yawDelta, double pitchDelta, double deltaSeconds) {
		if (deltaSeconds <= 0) {
			return 0;
		}
		double yaw = wrapDegrees(yawDelta);
		return Math.sqrt(yaw * yaw + pitchDelta * pitchDelta) / deltaSeconds;
	}

	/**
	 * Converts angular speed to a 0..1 motion factor. Sensitivity 0..1 lowers the speed at
	 * which blur starts: 0 starts at 360°/s, 1 starts at 30°/s.
	 */
	public static double motionFactor(double degreesPerSecond, double sensitivity) {
		double s = clamp(sensitivity, 0, 1);
		double threshold = 360.0 + (30.0 - 360.0) * s;
		double full = threshold * 3.0;
		if (degreesPerSecond <= threshold) {
			return 0;
		}
		double t = clamp((degreesPerSecond - threshold) / (full - threshold), 0, 1);
		return t * t * (3 - 2 * t);
	}

	/**
	 * Per-frame weight of the previous frame. The trail lasts roughly
	 * {@code durationMillis * intensity * motion} regardless of frame rate, until the
	 * per-frame weight reaches {@link #MAX_BLEND}.
	 */
	public static double blendFactor(double motion, double intensity, double durationMillis, double deltaSeconds) {
		double persistence = durationMillis / 1000.0 * clamp(intensity, 0, 1) * clamp(motion, 0, 1);
		if (persistence <= 1e-4 || deltaSeconds <= 0) {
			return 0;
		}
		return Math.min(MAX_BLEND, Math.exp(-deltaSeconds / persistence));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
