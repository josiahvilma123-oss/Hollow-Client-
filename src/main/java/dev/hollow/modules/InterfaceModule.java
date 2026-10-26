package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.NumberSetting;

/** Look and feel of the Hollow menu. Always on. */
public final class InterfaceModule extends Module {
	public final ColorSetting accent = add("Theme", new ColorSetting("accent", "Accent", "Highlight color used across Hollow.", 0xFF9D4EDD, false));
	public final NumberSetting panelOpacity = add("Theme", new NumberSetting("panel_opacity", "Panel Opacity", "", 0.92, 0.6, 1, 0.01));
	public final NumberSetting backdrop = add("Theme", new NumberSetting("backdrop", "Backdrop Dim", "How much the world behind the menu is darkened.", 0.55, 0, 0.9, 0.05));
	public final BoolSetting descriptions = add("Theme", new BoolSetting("descriptions", "Show Descriptions", "Show help text under settings.", true));
	public final NumberSetting animationSpeed = add("Theme", new NumberSetting("animation_speed", "Animation Speed",
		"Speed of hover and toggle animations.", 1.0, 0.25, 3, 0.05).unit("x"));

	public InterfaceModule() {
		super("interface", "Interface", "Theme and behaviour of the Hollow menu. Open it with Right Shift (rebind in Controls).",
			Category.CLIENT, true);
	}

	@Override
	public boolean isToggleable() {
		return false;
	}
}
