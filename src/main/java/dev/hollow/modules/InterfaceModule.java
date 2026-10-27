package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.StringListSetting;
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

	/** Saved window positions: "window_id|x|y|collapsed". Edited by dragging, not shown as a row. */
	public final StringListSetting layout = add("Layout", new StringListSetting("layout", "Window Layout", "",
		java.util.List.of(), entry -> entry.matches("[a-z0-9_]+\\|-?\\d+\\|-?\\d+\\|[01]"), ""));
	public final ActionSetting resetLayout = add("Layout", new ActionSetting("reset_layout", "Reset Window Layout",
		"Move every Hollow window back to its default position.", "Reset", () -> layout.set(java.util.List.of())));

	public InterfaceModule() {
		super("interface", "Interface", "Theme and behaviour of the Hollow menu. Open it with Right Shift (rebind in Controls).",
			Category.SETTINGS, true);
		layout.visibleWhen(() -> false);
	}

	/** Saved position of a window, or null when it has never been moved. */
	public int[] windowPosition(String id) {
		for (String entry : layout.get()) {
			String[] parts = entry.split("\\|");
			if (parts[0].equals(id)) {
				return new int[] {Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])};
			}
		}
		return null;
	}

	public void saveWindowPosition(String id, int x, int y, boolean collapsed) {
		java.util.List<String> entries = new java.util.ArrayList<>();
		for (String entry : layout.get()) {
			if (!entry.startsWith(id + "|")) {
				entries.add(entry);
			}
		}
		entries.add(id + "|" + x + "|" + y + "|" + (collapsed ? 1 : 0));
		layout.set(entries);
	}

	@Override
	public boolean isToggleable() {
		return false;
	}
}
