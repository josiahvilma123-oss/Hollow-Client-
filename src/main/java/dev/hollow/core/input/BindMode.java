package dev.hollow.core.input;

import dev.hollow.core.setting.EnumSetting;

/** How a module's keybind acts. */
public enum BindMode implements EnumSetting.Displayable {
	TOGGLE("Toggle"),
	HOLD("Hold");

	private final String label;

	BindMode(String label) {
		this.label = label;
	}

	@Override
	public String displayName() {
		return label;
	}
}
