package dev.hollow.core.anim;

/**
 * Frame-rate independent exponential smoothing. The value covers ~63% of the remaining
 * distance to its target every {@code timeConstant} seconds, regardless of FPS.
 */
public final class Smooth {
	private double value;
	private double target;

	public Smooth(double initial) {
		this.value = initial;
		this.target = initial;
	}

	public double value() {
		return value;
	}

	public double target() {
		return target;
	}

	public void target(double target) {
		this.target = target;
	}

	public void snap(double value) {
		this.value = value;
		this.target = value;
	}

	public double update(double deltaSeconds, double timeConstant) {
		value = approach(value, target, deltaSeconds, timeConstant);
		return value;
	}

	public boolean settled(double epsilon) {
		return Math.abs(value - target) <= epsilon;
	}

	public static double approach(double current, double target, double deltaSeconds, double timeConstant) {
		if (timeConstant <= 0 || deltaSeconds <= 0) {
			return timeConstant <= 0 ? target : current;
		}
		double factor = 1 - Math.exp(-deltaSeconds / timeConstant);
		return current + (target - current) * factor;
	}
}
