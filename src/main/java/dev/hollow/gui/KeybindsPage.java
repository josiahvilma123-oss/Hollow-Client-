package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.HollowClient;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.input.KeybindConflicts;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Every Holo keybind in one list, grouped by module, with conflict warnings and a reset. */
final class KeybindsPage {
	private static final long CONFLICT_REFRESH_MS = 1000;
	final RowList list;
	private String conflictSummary = "";
	private long conflictsCheckedAt;

	KeybindsPage(Consumer<String> notify) {
		List<Row> rows = new ArrayList<>();
		rows.add(new InfoRow("Holo keys only work in-game, never while a menu, chat or text field is open. Click a bind, then press a key, "
			+ "a mouse button or a combination with Ctrl, Shift or Alt. Backspace clears it, Esc cancels and right-click restores the default."));
		rows.add(new InfoRow(() -> "Open this menu: " + openKey() + " (change it in Options → Controls → Key Binds → Holo).", Theme.TEXT_DIM));
		rows.add(new InfoRow(this::conflictSummary, Theme.WARNING));
		rows.add(new ConfirmRow("Reset All Keybinds", "Restores every Holo keybind and key mode to its default. Other settings are kept.",
			"Reset all", () -> {
				for (Module module : Hollow.modules().all()) {
					for (Setting<?> setting : module.settings()) {
						if (setting instanceof KeySetting || setting == module.bindMode()) {
							setting.reset();
						}
					}
				}
				notify.accept("All keybinds restored to their defaults");
			}));
		for (Module module : Hollow.modules().all()) {
			List<Row> binds = new ArrayList<>();
			for (Setting<?> setting : module.settings()) {
				if (setting instanceof KeySetting key) {
					binds.add(new KeyRow(key));
				}
			}
			if (binds.isEmpty()) {
				continue;
			}
			if (module.isToggleable()) {
				binds.add(new EnumRow(module.bindMode()));
			}
			rows.add(new HeaderRow("keybinds", module.category().displayName() + " · " + module.name(), false));
			rows.addAll(binds);
		}
		list = new RowList(rows, "");
	}

	private static String openKey() {
		return HollowClient.openMenuKey() == null ? "Right Shift" : HollowClient.openMenuKey().getTranslatedKeyMessage().getString();
	}

	private String conflictSummary() {
		long now = System.currentTimeMillis();
		if (now - conflictsCheckedAt > CONFLICT_REFRESH_MS) {
			conflictsCheckedAt = now;
			int count = 0;
			for (Module module : Hollow.modules().all()) {
				for (Setting<?> setting : module.settings()) {
					if (setting instanceof KeySetting key && !KeybindConflicts.of(key, key.get()).isEmpty()) {
						count++;
					}
				}
			}
			conflictSummary = count == 0 ? "" : "⚠ " + count + (count == 1 ? " bind shares" : " binds share")
				+ " a key with another bind. Both actions run when that key is pressed; look for the warnings below.";
		}
		return conflictSummary;
	}
}
