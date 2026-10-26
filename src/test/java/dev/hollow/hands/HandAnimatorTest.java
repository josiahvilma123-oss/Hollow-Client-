package dev.hollow.hands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hollow.core.anim.Easing;
import org.junit.jupiter.api.Test;

class HandAnimatorTest {
	@Test
	void slowerSwingSpeedStretchesTheSwing() {
		HandAnimator animator = new HandAnimator();
		animator.config().swingSpeed = 0.5;
		animator.config().swingEasing = Easing.LINEAR;
		HandAnimator.Hand hand = animator.hand(true);
		animator.beginFrame(10.0, false, false, 0);
		animator.swing(hand, 0.1, 6);
		// Vanilla swing is 0.3s; at half speed the hand is still mid-swing after 0.45s.
		animator.beginFrame(10.45, false, false, 0);
		double progress = animator.swing(hand, 0, 6);
		assertEquals(0.75, progress, 1e-6);
		animator.beginFrame(10.7, false, false, 0);
		assertEquals(0, animator.swing(hand, 0, 6));
	}

	@Test
	void newVanillaSwingRestartsTheClock() {
		HandAnimator animator = new HandAnimator();
		animator.config().swingEasing = Easing.LINEAR;
		HandAnimator.Hand hand = animator.hand(true);
		animator.beginFrame(1.0, false, false, 0);
		animator.swing(hand, 0.2, 6);
		animator.beginFrame(1.15, false, false, 0);
		animator.swing(hand, 0.7, 6);
		animator.beginFrame(1.16, false, false, 0);
		double restarted = animator.swing(hand, 0.05, 6);
		assertEquals(0, restarted, 1e-6);
		assertTrue(hand.swinging());
	}

	@Test
	void instantEquipStyleRemovesTheDip() {
		HandAnimator animator = new HandAnimator();
		animator.config().equipStyle = EquipStyle.NONE;
		animator.beginFrame(0, false, false, 0);
		assertEquals(0, animator.equip(animator.hand(true), 0.8));
		animator.config().equipStyle = EquipStyle.VANILLA;
		assertEquals(0.8, animator.equip(animator.hand(true), 0.8));
	}

	@Test
	void swingTransformMatchesVanillaAtFullIntensity() {
		HandAnimator animator = new HandAnimator();
		double p = 0.37;
		HandAnimator.SwingTransform t = animator.swingTransform(p, true);
		assertEquals(-0.4 * Math.sin(Math.sqrt(p) * Math.PI), t.tx(), 1e-9);
		assertEquals(0.2 * Math.sin(Math.sqrt(p) * Math.PI * 2), t.ty(), 1e-9);
		assertEquals(-0.2 * Math.sin(p * Math.PI), t.tz(), 1e-9);
		assertEquals(-80 * Math.sin(Math.sqrt(p) * Math.PI), t.pitch(), 1e-9);
	}

	@Test
	void restPoseIsNeutralWithDefaultOffsets() {
		HandAnimator animator = new HandAnimator();
		animator.config().idleSway = 0;
		animator.beginFrame(0, false, false, 0);
		HandPose pose = animator.pose(animator.hand(true), true, false);
		assertEquals(0, pose.tx(), 1e-9);
		assertEquals(0, pose.ty(), 1e-9);
		assertEquals(1, pose.scale(), 1e-9);
	}

	@Test
	void offHandMirrorsHorizontalOffsets() {
		HandAnimator animator = new HandAnimator();
		animator.config().idleSway = 0;
		animator.config().offsetX = 0.1;
		animator.config().rotY = 15;
		animator.beginFrame(0, false, false, 0);
		HandPose right = animator.pose(animator.hand(true), true, false);
		HandPose left = animator.pose(animator.hand(false), false, false);
		assertEquals(-right.tx(), left.tx(), 1e-9);
		assertEquals(-right.rotY(), left.rotY(), 1e-9);
	}

	@Test
	void trailBufferPrunesOldSamples() {
		TrailBuffer trail = new TrailBuffer(4);
		for (int i = 0; i < 6; i++) {
			trail.add(i * 0.1, i, 0, 0, i, 1, 0);
		}
		assertEquals(4, trail.size());
		assertEquals(2f, trail.near(0, 0));
		trail.prune(0.55, 0.2);
		assertEquals(2, trail.size());
		assertEquals(4f, trail.near(0, 0));
	}
}
