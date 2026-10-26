package dev.hollow.blur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MotionBlurMathTest {
	@Test
	void yawWrapsAcrossTheSeam() {
		assertEquals(-20, MotionBlurMath.wrapDegrees(340), 1e-9);
		assertEquals(10, MotionBlurMath.angularSpeed(-350, 0, 1), 1e-9);
	}

	@Test
	void slowTurnsProduceNoBlur() {
		assertEquals(0, MotionBlurMath.motionFactor(100, 0.5));
		assertEquals(1, MotionBlurMath.motionFactor(5000, 0.5), 1e-9);
	}

	@Test
	void higherSensitivityStartsEarlier() {
		assertTrue(MotionBlurMath.motionFactor(150, 1.0) > MotionBlurMath.motionFactor(150, 0.2));
	}

	@Test
	void blendIsZeroWhenStillOrDisabled() {
		assertEquals(0, MotionBlurMath.blendFactor(0, 0.5, 80, 1 / 60.0));
		assertEquals(0, MotionBlurMath.blendFactor(1, 0, 80, 1 / 60.0));
	}

	@Test
	void trailLengthIsFrameRateIndependent() {
		// After 50ms the remaining weight of an old frame should match at 60 and 120 FPS.
		double at60 = Math.pow(MotionBlurMath.blendFactor(1, 0.5, 80, 1 / 60.0), 3);
		double at120 = Math.pow(MotionBlurMath.blendFactor(1, 0.5, 80, 1 / 120.0), 6);
		assertEquals(at60, at120, 1e-9);
	}

	@Test
	void blendNeverExceedsCeiling() {
		assertTrue(MotionBlurMath.blendFactor(1, 1, 1000, 1 / 1000.0) <= MotionBlurMath.MAX_BLEND);
	}
}
