package dev.hollow.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hollow.Hollow;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import dev.hollow.sky.SkyPresetStore;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Sky Studio's preset library: a search box and a list of matching presets, each with a small
 * swatch of its sky. Clicking a preset applies it.
 */
final class SkyLibraryRow extends Row {
	private static final int ENTRY = 20;
	private static final int MAX_ENTRIES = 8;
	private static final long REFRESH_MS = 500;
	private final SkyStudioModule module;
	private final Setting<?> setting;
	private final TextInput search = new TextInput("Search skies…", 32);
	private final Map<SkyPresetStore.Preset, int[]> swatches = new IdentityHashMap<>();
	private List<SkyPresetStore.Preset> matches = List.of();
	private String lastQuery;
	private long lastRefresh;

	SkyLibraryRow(SkyStudioModule module) {
		this.module = module;
		this.setting = module.preset;
	}

	@Override
	Setting<?> setting() {
		return setting;
	}

	private List<SkyPresetStore.Preset> matches() {
		long now = System.currentTimeMillis();
		if (!search.text().equals(lastQuery) || now - lastRefresh > REFRESH_MS) {
			lastQuery = search.text();
			lastRefresh = now;
			matches = Hollow.skyPresets().search(lastQuery);
			if (swatches.size() > 64) {
				swatches.clear();
			}
		}
		return matches;
	}

	private int listHeight() {
		int shown = Math.min(MAX_ENTRIES, matches().size());
		return Math.max(1, shown) * ENTRY + (matches().size() > MAX_ENTRIES ? 12 : 0);
	}

	@Override
	int height(UiContext ui) {
		return 6 + 16 + 4 + listHeight() + 16;
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		int left = x + PAD;
		int w = width - PAD * 2;
		int top = y + 6;
		search.render(g, ui, left, top, w, 16);
		int cy = top + 20;
		List<SkyPresetStore.Preset> list = matches();
		if (list.isEmpty()) {
			Draw2D.text(g, ui.font, "No skies match", left + 4, cy + 6, ui.c(Theme.TEXT_FAINT));
		}
		String active = module.preset.get();
		for (int i = 0; i < Math.min(MAX_ENTRIES, list.size()); i++) {
			SkyPresetStore.Preset preset = list.get(i);
			boolean selected = preset.name().equals(active);
			boolean hovered = over(mouseX, mouseY, left, cy, w, ENTRY - 2);
			int fill = selected ? Colors.withAlpha(ui.accent, 70) : hovered ? Theme.CARD_HOVER : Theme.CARD;
			Draw2D.rounded(g, left, cy, w, ENTRY - 2, 4, ui.c(fill));
			if (selected) {
				Draw2D.roundedBorder(g, left, cy, w, ENTRY - 2, 4, 1, ui.c(Colors.withAlpha(ui.accentLight, 200)));
			}
			drawSwatch(g, ui, swatch(preset), left + 3, cy + 3, 30, ENTRY - 8);
			String tag = preset.builtIn() ? "Built-in" : "Custom";
			int tagWidth = ui.font.width(tag);
			String name = preset.name() + (selected && module.modified() ? " •" : "");
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, name, w - 48 - tagWidth), left + 38, cy + 5, ui.c(Theme.TEXT));
			Draw2D.text(g, ui.font, tag, left + w - tagWidth - 5, cy + 5, ui.c(preset.builtIn() ? Theme.TEXT_FAINT : ui.accentLight));
			cy += ENTRY;
		}
		if (list.size() > MAX_ENTRIES) {
			Draw2D.text(g, ui.font, "+" + (list.size() - MAX_ENTRIES) + " more — refine the search", left + 4, cy + 1, ui.c(Theme.TEXT_FAINT));
		}
		String status = module.message();
		if (status.isEmpty()) {
			status = module.modified() ? "Unsaved changes to " + active + " — Save keeps them" : "Click a sky to apply it";
		}
		if (!module.isEnabled()) {
			status = "Sky Studio is off — turn it on to see skies in game";
		}
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, status, w), left, y + height(ui) - 12, ui.c(Theme.TEXT_DIM));
	}

	private void drawSwatch(GuiGraphics g, UiContext ui, int[] colors, int sx, int sy, int sw, int sh) {
		g.fillGradient(sx, sy, sx + sw, sy + sh, ui.c(colors[0]), ui.c(colors[1]));
		// The moon (or sun) as a dot in its own color.
		Draw2D.circle(g, sx + sw * 0.7f, sy + sh * 0.38f, 2.2f, ui.c(colors[2]));
		Draw2D.border(g, sx, sy, sw, sh, ui.c(Theme.BORDER_SOFT));
	}

	private int[] swatch(SkyPresetStore.Preset preset) {
		return swatches.computeIfAbsent(preset, p -> {
			JsonObject v = p.values();
			return new int[] {color(v, "zenith", module.zenith.defaultValue()), color(v, "horizon", module.horizon.defaultValue()),
				color(v, "moon_color", module.moonColor.defaultValue())};
		});
	}

	private static int color(JsonObject values, String key, int fallback) {
		JsonElement element = values.get(key);
		if (element == null || !element.isJsonPrimitive()) {
			return fallback;
		}
		if (element.getAsJsonPrimitive().isNumber()) {
			return element.getAsInt();
		}
		Integer parsed = Colors.parseHex(element.getAsString());
		return parsed == null ? fallback : parsed;
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		int left = x + PAD;
		int w = width - PAD * 2;
		int top = y + 6;
		boolean onSearch = over(mouseX, mouseY, left, top, w, 16);
		search.focus(onSearch);
		if (onSearch) {
			if (button == 1) {
				search.setText("");
			}
			return true;
		}
		int cy = top + 20;
		List<SkyPresetStore.Preset> list = matches();
		for (int i = 0; i < Math.min(MAX_ENTRIES, list.size()); i++) {
			if (over(mouseX, mouseY, left, cy, w, ENTRY - 2)) {
				module.applyPreset(list.get(i).name());
				return true;
			}
			cy += ENTRY;
		}
		return true;
	}

	@Override
	boolean capturesKeyboard() {
		return search.focused();
	}

	@Override
	boolean cancelInput() {
		if (search.focused()) {
			search.focus(false);
			return true;
		}
		return false;
	}

	@Override
	boolean keyPressed(KeyEvent event) {
		if (!search.focused()) {
			return false;
		}
		if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
			// Enter applies the first match.
			if (!matches().isEmpty()) {
				module.applyPreset(matches().get(0).name());
			}
			search.focus(false);
			return true;
		}
		return search.keyPressed(event);
	}

	@Override
	boolean charTyped(CharacterEvent event) {
		return search.charTyped(event);
	}
}
