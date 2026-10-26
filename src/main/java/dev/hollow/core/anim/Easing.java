package dev.hollow.core.anim;

import dev.hollow.core.setting.EnumSetting;

/** Standard easing curves mapping linear progress in [0, 1] to eased progress. */
public enum Easing implements EnumSetting.Displayable {
	LINEAR("Linear") {
		@Override
		public double apply(double t) {
			return t;
		}
	},
	SINE("Sine") {
		@Override
		public double apply(double t) {
			return -(Math.cos(Math.PI * t) - 1) / 2;
		}
	},
	CUBIC_OUT("Cubic Out") {
		@Override
		public double apply(double t) {
			return 1 - Math.pow(1 - t, 3);
		}
	},
	CUBIC_IN_OUT("Cubic In-Out") {
		@Override
		public double apply(double t) {
			return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
		}
	},
	EXPO_OUT("Expo Out") {
		@Override
		public double apply(double t) {
			return t >= 1 ? 1 : 1 - Math.pow(2, -10 * t);
		}
	},
	BACK_OUT("Back Out") {
		@Override
		public double apply(double t) {
			double c1 = 1.70158;
			double c3 = c1 + 1;
			return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
		}
	};

	private final String displayName;

	Easing(String displayName) {
		this.displayName = displayName;
	}

	public abstract double apply(double t);

	/** Applies the curve after clamping the input to [0, 1]. */
	public double ease(double t) {
		return apply(Math.max(0, Math.min(1, t)));
	}

	@Override
	public String displayName() {
		return displayName;
	}
}
