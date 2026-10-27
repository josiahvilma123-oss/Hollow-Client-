package dev.hollow.perf;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.core.perf.AdaptiveQuality;
import dev.hollow.core.perf.FrameStats;
import dev.hollow.modules.PerformanceModule;

/** Frame measurements and the current adaptive effect level, shared by the monitor and the effects. */
public final class PerformanceState {
	public static final PerformanceState INSTANCE = new PerformanceState();
	private static final String[] LEVEL_EFFECTS = {
		"full effects",
		"sky detail reduced",
		"sky detail low, motion blur and swing trails paused",
		"minimal effects: menu particles off as well"
	};

	private final FrameStats stats = new FrameStats(4096);
	private final AdaptiveQuality adaptive = new AdaptiveQuality(5, 20, 10, 1.3);
	private long lastSample = -1;

	private PerformanceState() {
	}

	public FrameStats stats() {
		return stats;
	}

	/** Called once per rendered frame. */
	public void onFrame() {
		long now = System.nanoTime();
		stats.frame(now);
		if (lastSample < 0) {
			lastSample = now;
		} else if (now - lastSample >= 1_000_000_000L) {
			lastSample = now;
			sampleAdaptive(now);
		}
	}

	private void sampleAdaptive(long now) {
		if (Hollow.modules() == null) {
			return;
		}
		PerformanceModule module = Hollow.module(PerformanceModule.class);
		if (!module.adaptive.get()) {
			if (adaptive.level() != 0) {
				adaptive.reset();
			}
			return;
		}
		int change = adaptive.sample(stats.fps(now, 1_000_000_000L), module.targetFps.get());
		if (change != 0 && module.notify.get() && Hollow.alerts() != null) {
			Hollow.alerts().report(new Alert(AlertSource.SYSTEM, "Adaptive quality", change > 0 ? "lowered" : "raised", Double.NaN,
				"adaptive:" + adaptive.level(), LEVEL_EFFECTS[adaptive.level()]));
		}
	}

	/** 0 = full effects ... 3 = minimal. Decorative effects consult this; it is 0 unless Adaptive Quality is on. */
	public int effectLevel() {
		return adaptive.level();
	}

	public String effectDescription() {
		return LEVEL_EFFECTS[adaptive.level()];
	}
}
