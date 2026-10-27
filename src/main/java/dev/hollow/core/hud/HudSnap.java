package dev.hollow.core.hud;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapping for the HUD editor: a dragged box snaps its edges or centre to the screen edges
 * (with a margin), the screen's centre lines, and other boxes' edges and centres.
 */
public final class HudSnap {
	public record Box(float x, float y, float w, float h) {
	}

	/** A guide line to draw: vertical lines have a fixed x, horizontal ones a fixed y. */
	public record Guide(boolean vertical, float position) {
	}

	public record Result(float x, float y, List<Guide> guides) {
	}

	private HudSnap() {
	}

	public static Result snap(Box dragged, float screenW, float screenH, float margin, float threshold, List<Box> others) {
		List<Float> xTargets = new ArrayList<>(List.of(margin, screenW / 2f, screenW - margin));
		List<Float> yTargets = new ArrayList<>(List.of(margin, screenH / 2f, screenH - margin));
		for (Box other : others) {
			xTargets.add(other.x());
			xTargets.add(other.x() + other.w() / 2f);
			xTargets.add(other.x() + other.w());
			yTargets.add(other.y());
			yTargets.add(other.y() + other.h() / 2f);
			yTargets.add(other.y() + other.h());
		}
		List<Guide> guides = new ArrayList<>();
		float[] xs = axis(dragged.x(), dragged.w(), xTargets, threshold);
		float[] ys = axis(dragged.y(), dragged.h(), yTargets, threshold);
		if (xs[1] >= 0) {
			guides.add(new Guide(true, xs[1]));
		}
		if (ys[1] >= 0) {
			guides.add(new Guide(false, ys[1]));
		}
		float x = Math.max(0, Math.min(screenW - dragged.w(), xs[0]));
		float y = Math.max(0, Math.min(screenH - dragged.h(), ys[0]));
		return new Result(x, y, guides);
	}

	/** Snaps one axis: the box's start, centre or end to the nearest target. Returns {position, guideOrMinusOne}. */
	private static float[] axis(float start, float size, List<Float> targets, float threshold) {
		float best = threshold + 1;
		float snapped = start;
		float guide = -1;
		float[] anchors = {0, size / 2f, size};
		for (float target : targets) {
			for (float anchor : anchors) {
				float distance = Math.abs(start + anchor - target);
				if (distance <= threshold && distance < best) {
					best = distance;
					snapped = target - anchor;
					guide = target;
				}
			}
		}
		return new float[] {snapped, guide};
	}

	/** Position as a fraction of the free space, so elements keep their relative place at any resolution. */
	public static float toFraction(float position, float size, float screen) {
		float free = screen - size;
		return free <= 0 ? 0 : Math.max(0, Math.min(1, position / free));
	}

	public static float fromFraction(float fraction, float size, float screen) {
		return Math.max(0, fraction * (screen - size));
	}
}
