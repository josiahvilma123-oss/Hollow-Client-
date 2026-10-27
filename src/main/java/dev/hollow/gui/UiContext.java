package dev.hollow.gui;

import dev.hollow.core.util.Colors;
import net.minecraft.client.gui.Font;

/** Per-frame drawing state shared by all rows. */
final class UiContext {
	Font font;
	float alpha = 1f;
	double delta;
	double animationSpeed = 1;
	boolean descriptions = true;
	int accent;
	int accentLight;
	/** Ornament color (gold by default). */
	int detail;
	/** Animations jump straight to their end state. */
	boolean reducedMotion;

	int accentDarkOr() {
		return Colors.lerp(accent, 0xFF000000, 0.55f);
	}

	int c(int argb) {
		return alpha >= 1f ? argb : Colors.fade(argb, alpha);
	}

	/** Exponential approach helper scaled by the user's animation speed. */
	double approach(double current, double target, double timeConstant) {
		if (reducedMotion) {
			return target;
		}
		return dev.hollow.core.anim.Smooth.approach(current, target, delta * animationSpeed, timeConstant);
	}
}
