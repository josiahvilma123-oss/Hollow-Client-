package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.StringListSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Positions and scale of Holo's HUD elements. Positions are edited in the HUD editor. */
public final class HudLayoutModule extends Module {
	/** Entries "element_id|x|y" with x and y as fractions of the free screen space. */
	public final StringListSetting positions = add("Layout", new StringListSetting("positions", "Positions", "",
		List.of(), entry -> entry.matches("[a-z0-9_]+\\|[01](\\.\\d{1,4})?\\|[01](\\.\\d{1,4})?"), ""));
	public final NumberSetting scale = add("Layout", new NumberSetting("scale", "HUD Scale", "Size of every Holo HUD element.", 1.0, 0.5, 2.0, 0.05).unit("x"));
	public final BoolSetting snap = add("Layout", new BoolSetting("snap", "Snap to Guides",
		"In the HUD editor, snap elements to screen edges, centre lines and each other.", true));
	public final ActionSetting openEditor = add("Layout", new ActionSetting("open_editor", "HUD Editor",
		"Drag elements to place them. Right-click an element to reset it; arrow keys nudge the selected one.", "Open editor",
		() -> dev.hollow.hud.HudEditorScreen.open()));
	public final ActionSetting resetLayout = add("Layout", new ActionSetting("reset_layout", "Reset Layout",
		"Put every HUD element back in its default place.", "Reset", () -> positions.set(List.of())));

	public HudLayoutModule() {
		super("hud_layout", "HUD Layout", "Place and scale every Holo HUD element with a drag-and-drop editor.", Category.HUD, true);
		positions.visibleWhen(() -> false);
	}

	@Override
	public boolean isToggleable() {
		return false;
	}

	/** Saved position as {x, y} fractions, or null when the element has never been moved. */
	public float[] position(String id) {
		for (String entry : positions.get()) {
			String[] parts = entry.split("\\|");
			if (parts[0].equals(id)) {
				return new float[] {Float.parseFloat(parts[1]), Float.parseFloat(parts[2])};
			}
		}
		return null;
	}

	public void setPosition(String id, float x, float y) {
		List<String> entries = new ArrayList<>();
		for (String entry : positions.get()) {
			if (!entry.startsWith(id + "|")) {
				entries.add(entry);
			}
		}
		entries.add(String.format(Locale.ROOT, "%s|%.4f|%.4f", id, clamp(x), clamp(y)));
		positions.set(entries);
	}

	public void resetPosition(String id) {
		positions.set(positions.get().stream().filter(entry -> !entry.startsWith(id + "|")).toList());
	}

	private static float clamp(float v) {
		return Math.max(0, Math.min(1, v));
	}
}
