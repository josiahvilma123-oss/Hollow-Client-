package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hollow.core.perf.AdaptiveQuality;
import dev.hollow.core.perf.FrameStats;
import org.junit.jupiter.api.Test;

class PerformanceMathTest {
	private static final long MS = 1_000_000L;

	@Test
	void frameStatsOverAWindow() {
		FrameStats stats = new FrameStats(256);
		long t = 0;
		stats.frame(t);
		for (int i = 0; i < 100; i++) {
			t += 10 * MS;
			stats.frame(t);
		}
		assertEquals(100, stats.fps(t, 1000 * MS), 0.5);
		assertEquals(10, stats.averageMillis(t, 1000 * MS), 1e-6);
		t += 50 * MS;
		stats.frame(t);
		assertEquals(50, stats.worstMillis(t, 1000 * MS), 1e-6);
		assertEquals(50, stats.worstMillis(t, 20 * MS), 1e-6, "only the newest frame is inside a 20 ms window");
		stats.reset();
		assertEquals(0, stats.fps(t, 1000 * MS));
	}

	@Test
	void frameStatsRingBufferOverwritesOldest() {
		FrameStats stats = new FrameStats(8);
		long t = 0;
		stats.frame(t);
		for (int i = 0; i < 20; i++) {
			t += (i < 12 ? 100 : 5) * MS;
			stats.frame(t);
		}
		assertEquals(5, stats.worstMillis(t, 10_000 * MS), 1e-6, "slow frames older than the buffer are forgotten");
	}

	@Test
	void adaptiveQualityLowersAfterSustainedLowFpsWithCooldown() {
		AdaptiveQuality quality = new AdaptiveQuality(5, 20, 10, 1.3);
		for (int i = 0; i < 4; i++) {
			assertEquals(0, quality.sample(30, 60));
		}
		assertEquals(1, quality.sample(30, 60), "fifth low second lowers effects");
		assertEquals(1, quality.level());
		for (int i = 0; i < 8; i++) {
			assertEquals(0, quality.sample(30, 60), "cooldown blocks a second step");
		}
		int changed = 0;
		for (int i = 0; i < 5; i++) {
			changed += quality.sample(30, 60);
		}
		assertEquals(1, changed);
		assertEquals(2, quality.level());
	}

	@Test
	void adaptiveQualityRaisesOnlyWithHeadroomAndNeverPastLimits() {
		AdaptiveQuality quality = new AdaptiveQuality(1, 3, 0, 1.3);
		for (int i = 0; i < 10; i++) {
			quality.sample(10, 60);
		}
		assertEquals(AdaptiveQuality.MAX_LEVEL, quality.level());
		for (int i = 0; i < 30; i++) {
			quality.sample(70, 60);
		}
		assertEquals(AdaptiveQuality.MAX_LEVEL, quality.level(), "70 FPS is not 1.3x headroom over 60");
		for (int i = 0; i < 30; i++) {
			quality.sample(100, 60);
		}
		assertEquals(0, quality.level());
		assertEquals(0, quality.sample(0, 60), "no measurement, no change");
	}
}
