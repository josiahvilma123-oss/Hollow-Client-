package dev.hollow.core.perf;

/**
 * Optional automatic quality steps. Fed once per second with the measured FPS: after a sustained
 * stretch below the target it lowers the effect level by one; after a longer stretch comfortably
 * above it raises it again. A cooldown separates changes so the level never flickers.
 */
public final class AdaptiveQuality {
	public static final int MAX_LEVEL = 3;

	private final int lowerAfterSeconds;
	private final int raiseAfterSeconds;
	private final int cooldownSeconds;
	private final double headroom;
	private int level;
	private int belowSeconds;
	private int aboveSeconds;
	private int sinceChange = Integer.MAX_VALUE / 2;

	public AdaptiveQuality(int lowerAfterSeconds, int raiseAfterSeconds, int cooldownSeconds, double headroom) {
		this.lowerAfterSeconds = lowerAfterSeconds;
		this.raiseAfterSeconds = raiseAfterSeconds;
		this.cooldownSeconds = cooldownSeconds;
		this.headroom = headroom;
	}

	/** 0 = full effects, {@link #MAX_LEVEL} = minimal effects. */
	public int level() {
		return level;
	}

	public void reset() {
		level = 0;
		belowSeconds = aboveSeconds = 0;
		sinceChange = Integer.MAX_VALUE / 2;
	}

	/**
	 * One second of measurement. Returns +1 when the level was lowered (effects reduced), -1 when
	 * raised, 0 otherwise.
	 */
	public int sample(double fps, double targetFps) {
		sinceChange++;
		if (fps <= 0) {
			return 0;
		}
		if (fps < targetFps) {
			belowSeconds++;
			aboveSeconds = 0;
		} else if (fps > targetFps * headroom) {
			aboveSeconds++;
			belowSeconds = 0;
		} else {
			belowSeconds = 0;
			aboveSeconds = 0;
		}
		if (sinceChange < cooldownSeconds) {
			return 0;
		}
		if (belowSeconds >= lowerAfterSeconds && level < MAX_LEVEL) {
			level++;
			belowSeconds = 0;
			sinceChange = 0;
			return 1;
		}
		if (aboveSeconds >= raiseAfterSeconds && level > 0) {
			level--;
			aboveSeconds = 0;
			sinceChange = 0;
			return -1;
		}
		return 0;
	}
}
