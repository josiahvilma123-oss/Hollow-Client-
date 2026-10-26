package dev.hollow.core.anim;

/**
 * A reversible timed transition between 0 (closed) and 1 (open), used for panel
 * slides, toasts and hand state changes. Time is supplied by the caller.
 */
public final class Transition {
	private double progress;
	private boolean forward;
	private double durationSeconds;

	public Transition(double durationSeconds, boolean startOpen) {
		this.durationSeconds = Math.max(1e-4, durationSeconds);
		this.progress = startOpen ? 1 : 0;
		this.forward = startOpen;
	}

	public void duration(double seconds) {
		this.durationSeconds = Math.max(1e-4, seconds);
	}

	public void open() {
		forward = true;
	}

	public void close() {
		forward = false;
	}

	public void set(boolean open) {
		forward = open;
	}

	public void snap(boolean open) {
		forward = open;
		progress = open ? 1 : 0;
	}

	public boolean isOpening() {
		return forward;
	}

	public void update(double deltaSeconds) {
		double step = deltaSeconds / durationSeconds;
		progress = forward ? Math.min(1, progress + step) : Math.max(0, progress - step);
	}

	public double raw() {
		return progress;
	}

	public double eased(Easing easing) {
		return easing.ease(progress);
	}

	public boolean finished() {
		return forward ? progress >= 1 : progress <= 0;
	}

	public boolean fullyClosed() {
		return !forward && progress <= 0;
	}
}
