package dev.hollow.modules;

import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.Setting;

/** Hides dependent settings while their parent toggle is off, keeping long pages tidy. */
final class BooleanVisibility {
	private BooleanVisibility() {
	}

	static void when(BoolSetting parent, Setting<?>... children) {
		for (Setting<?> child : children) {
			child.visibleWhen(parent::get);
		}
	}
}
