package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hollow.core.aim.AimMath;
import org.junit.jupiter.api.Test;

class AimMathTest {
	@Test
	void wrapsIntoHalfOpenRange() {
		assertEquals(0f, AimMath.wrapDegrees(360f), 1e-4);
		assertEquals(-170f, AimMath.wrapDegrees(190f), 1e-4);
		assertEquals(170f, AimMath.wrapDegrees(-190f), 1e-4);
		assertEquals(-180f, AimMath.wrapDegrees(180f), 1e-4);
	}

	@Test
	void yawAndPitchFollowMinecraftConventions() {
		assertEquals(0f, AimMath.wrapDegrees(AimMath.yawTo(0, 1)), 1e-3, "+z is yaw 0");
		assertEquals(90f, AimMath.wrapDegrees(AimMath.yawTo(-1, 0)), 1e-3, "-x is yaw 90");
		assertEquals(-90f, AimMath.wrapDegrees(AimMath.yawTo(1, 0)), 1e-3, "+x is yaw -90");
		assertEquals(45f, AimMath.pitchTo(1, -1, 0), 1e-3, "looking down is positive pitch");
		assertEquals(-45f, AimMath.pitchTo(0, 1, 1), 1e-3);
	}

	@Test
	void angleBetweenUsesTheShortWayRound() {
		assertEquals(20f, AimMath.angleBetween(170f, 0f, -170f, 0f), 1e-3);
		assertEquals(5f, AimMath.angleBetween(0f, 0f, 3f, 4f), 1e-3);
	}

	@Test
	void stepIsCappedAndFrameRateIndependent() {
		// Never faster than the speed limit.
		float step = AimMath.step(90f, 0.05, 0.01, 1.0, 100);
		assertEquals(5f, step, 1e-4);
		// Two half frames move about as far as one full frame (exponential smoothing).
		float whole = AimMath.step(10f, 0.02, 0.2, 0.5, 1000);
		float first = AimMath.step(10f, 0.01, 0.2, 0.5, 1000);
		float second = AimMath.step(10f - first, 0.01, 0.2, 0.5, 1000);
		assertEquals(whole, first + second, 0.01);
		// Direction follows the error, and nothing moves without strength or time.
		assertTrue(AimMath.step(-10f, 0.02, 0.2, 0.5, 1000) < 0);
		assertEquals(0f, AimMath.step(10f, 0.02, 0.2, 0, 1000));
		assertEquals(0f, AimMath.step(10f, 0, 0.2, 1, 1000));
	}

	@Test
	void strengthLimitsHowMuchOfTheErrorIsClosed() {
		float total = 0;
		float error = 10f;
		for (int i = 0; i < 1000; i++) {
			float step = AimMath.step(error - total, 0.016, 0.2, 0.5, 1000);
			total += step;
		}
		assertTrue(total > 9.9f && total <= 10f, "converges on the target over time, got " + total);
		assertTrue(AimMath.step(10f, 0.016, 0.2, 0.25, 1000) < AimMath.step(10f, 0.016, 0.2, 0.5, 1000));
	}

	@Test
	void defaultSensitivityIsNeutral() {
		assertEquals(1.0, AimMath.sensitivityFactor(0.5), 1e-9);
		assertTrue(AimMath.sensitivityFactor(1.0) > 1.0);
		assertTrue(AimMath.sensitivityFactor(0.0) < 1.0);
	}
}
