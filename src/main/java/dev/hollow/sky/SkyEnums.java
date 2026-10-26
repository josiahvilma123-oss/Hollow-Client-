package dev.hollow.sky;

import dev.hollow.core.setting.EnumSetting;

/** Option types used by Sky Studio settings. */
public final class SkyEnums {
	private SkyEnums() {
	}

	public enum TimeMode implements EnumSetting.Displayable {
		FOLLOW_GAME("Follow Day Cycle"),
		FIXED("Always Theme");

		private final String label;

		TimeMode(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum Motion implements EnumSetting.Displayable {
		FOLLOW_GAME("Follow Game Time"),
		PINNED("Pinned in Place");

		private final String label;

		Motion(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum BodyStyle implements EnumSetting.Displayable {
		HIDDEN("Hidden"),
		DISC("Disc"),
		HALO("Halo"),
		CRESCENT("Crescent (Moon Phase)"),
		ECLIPSE("Eclipse");

		private final String label;

		BodyStyle(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum Silhouettes implements EnumSetting.Displayable {
		NONE("None"),
		PYRAMIDS("Pyramids"),
		OBELISKS("Obelisks & Temples"),
		SPIRES("Ice Spires");

		private final String label;

		Silhouettes(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum CloudMode implements EnumSetting.Displayable {
		VANILLA("Vanilla"),
		TINTED("Tinted"),
		HIDDEN("Hidden");

		private final String label;

		CloudMode(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum Detail implements EnumSetting.Displayable {
		LOW("Low"),
		MEDIUM("Medium"),
		HIGH("High");

		private final String label;

		Detail(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}
}
