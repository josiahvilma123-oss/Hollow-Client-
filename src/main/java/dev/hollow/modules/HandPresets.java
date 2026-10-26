package dev.hollow.modules;

import dev.hollow.hands.HandPreset;

/** Copies a preset's values into the Hollow Hands settings. */
final class HandPresets {
	private HandPresets() {
	}

	static void apply(HandPreset preset, HollowHandsModule module) {
		HandPreset v = preset;
		module.swingSpeed.set(v.swingSpeed());
		module.swingIntensity.set(v.swingIntensity());
		module.swingEasing.set(v.swingEasing());
		module.bob.set(v.bob());
		module.sprint.set(v.sprint());
		module.idleSway.set(v.idleSway());
		module.equipStyle.set(v.equipStyle());
		module.useTransition.set(v.useTransitionMillis());
		module.trails.set(v.trails());
	}
}
