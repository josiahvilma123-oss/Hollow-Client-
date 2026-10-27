package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import java.util.List;

/** Look and feel of the Holo menu. Always on. */
public final class InterfaceModule extends Module {
	public enum ThemePreset implements EnumSetting.Displayable {
		HOLO_VIOLET("Holo Violet", 0xFF9D4EDD, 0xFFE6B54A),
		PHARAOH_GOLD("Pharaoh Gold", 0xFFD9A93A, 0xFF4FA3D9),
		CRIMSON("Crimson", 0xFFD6334F, 0xFFF0B45A),
		FROST("Frost", 0xFF5BB8F0, 0xFFDCEBFF),
		EMERALD("Emerald", 0xFF2FBF71, 0xFFE6B54A),
		CUSTOM("Custom", 0, 0);

		private final String label;
		private final int accent;
		private final int detail;

		ThemePreset(String label, int accent, int detail) {
			this.label = label;
			this.accent = accent;
			this.detail = detail;
		}

		public int accent() {
			return accent;
		}

		public int detail() {
			return detail;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	private boolean applyingTheme;

	public final EnumSetting<ThemePreset> theme = add("Theme", new EnumSetting<>("theme", "Theme",
		"Color scheme of the menu, HUD and overlays. Changing a color by hand switches to Custom.", ThemePreset.HOLO_VIOLET));
	public final ColorSetting accent = add("Theme", new ColorSetting("accent", "Accent", "Main highlight color: switches, selections, glows.",
		0xFF9D4EDD, false));
	public final ColorSetting detail = add("Theme", new ColorSetting("detail", "Detail", "Ornament color: borders of focused items, "
		+ "the wordmark and HUD editor guides.", 0xFFE6B54A, false));
	public final NumberSetting backdrop = add("Theme", new NumberSetting("backdrop", "Backdrop Dim",
		"How much the world behind the menu is darkened.", 0.55, 0, 0.9, 0.05));

	public final BoolSetting descriptions = add("Behaviour", new BoolSetting("descriptions", "Show Descriptions",
		"Show help text under settings and on module cards.", true));
	public final NumberSetting animationSpeed = add("Behaviour", new NumberSetting("animation_speed", "Animation Speed",
		"Speed of hover, page and panel animations.", 1.0, 0.25, 3, 0.05).unit("x"));
	public final BoolSetting reducedMotion = add("Behaviour", new BoolSetting("reduced_motion", "Reduced Motion",
		"Turn off sliding, scaling and fading in the menu. Changes happen instantly.", false));
	public final BoolSetting sidebarCollapsed = add("Behaviour", new BoolSetting("sidebar_collapsed", "Compact Sidebar",
		"Show only icons in the sidebar. Also toggled by the button at the top of the sidebar.", false));

	public InterfaceModule() {
		super("interface", "Interface", "Theme and behaviour of the Holo menu. Open it with Right Shift (rebind in Controls).",
			Category.SETTINGS, true);
		theme.onChange(this::applyTheme);
		for (ColorSetting color : List.of(accent, detail)) {
			color.onChange(value -> {
				if (!applyingTheme && theme.get() != ThemePreset.CUSTOM) {
					theme.set(ThemePreset.CUSTOM);
				}
			});
		}
	}

	private void applyTheme(ThemePreset value) {
		if (value == ThemePreset.CUSTOM) {
			return;
		}
		applyingTheme = true;
		try {
			accent.set(value.accent());
			detail.set(value.detail());
		} finally {
			applyingTheme = false;
		}
	}

	@Override
	public boolean isToggleable() {
		return false;
	}
}
