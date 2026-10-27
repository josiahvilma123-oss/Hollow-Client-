package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionBarSetting;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.setting.StringListSetting;
import dev.hollow.core.setting.TextSetting;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.EntityVisibilityModule;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.modules.WaypointsModule;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/** The expanded settings of one module: header with state, toggle and reset, then its setting rows. */
final class SettingsPanel {
	static final int HEADER = 30;

	final Module module;
	final RowList list;
	private final Toggle toggle;
	private final Confirm reset = new Confirm("Reset module", "Click again to reset");
	private int x;
	private int y;
	private int width;
	private int height;
	private int bodyTop;

	SettingsPanel(Module module) {
		this.module = module;
		this.list = new RowList(buildRows(module), module.name());
		this.toggle = new Toggle(module.isEnabled());
	}

	static List<Row> buildRows(Module module) {
		List<Row> result = new ArrayList<>();
		PreviewRow preview = PreviewRow.forModule(module);
		if (preview != null) {
			result.add(preview);
		}
		String group = null;
		boolean manyGroups = module.settings().stream().map(Setting::group).distinct().count() > 8;
		for (Setting<?> setting : module.settings()) {
			if (!setting.group().equals(group)) {
				group = setting.group();
				if (!group.isEmpty()) {
					result.add(new HeaderRow(module.id(), group, manyGroups && group.contains("·")));
				}
			}
			Row row = rowFor(setting);
			if (row != null) {
				result.add(row);
			}
			if (module instanceof WaypointsModule waypoints && setting == waypoints.share) {
				result.add(new WaypointListRow());
			} else if (module instanceof EntityVisibilityModule visibility && setting == visibility.clear) {
				result.add(new VisibilityLogRow());
			}
		}
		return result;
	}

	static Row rowFor(Setting<?> setting) {
		if (setting instanceof BoolSetting bool) {
			return new BoolRow(bool);
		}
		if (setting instanceof NumberSetting number) {
			return new NumberRow(number);
		}
		if (setting instanceof ColorSetting color) {
			return new ColorRow(color);
		}
		if (setting instanceof EnumSetting<?> enumSetting) {
			return new EnumRow(enumSetting);
		}
		if (setting instanceof KeySetting key) {
			return new KeyRow(key);
		}
		if (setting instanceof StringListSetting list) {
			return new ListRow(list);
		}
		if (setting instanceof ActionSetting action) {
			return new ActionRow(action);
		}
		if (setting instanceof ActionBarSetting bar) {
			return new ActionBarRow(bar);
		}
		if (setting instanceof TextSetting text) {
			SkyStudioModule sky = Hollow.module(SkyStudioModule.class);
			if (sky != null && setting == sky.preset) {
				return new SkyLibraryRow(sky);
			}
			return new TextRow(text);
		}
		return null;
	}

	/** Status shown on cards and in the header: label and color. */
	static String status(Module module) {
		if (!module.isAvailable()) {
			return "UNAVAILABLE";
		}
		if (!module.isToggleable()) {
			return "SETTINGS";
		}
		if (!module.isEnabled()) {
			return "OFF";
		}
		return module.blockedReason().isPresent() ? "BLOCKED" : "ON";
	}

	static int statusColor(Module module, UiContext ui) {
		return switch (status(module)) {
			case "UNAVAILABLE" -> Theme.DANGER;
			case "BLOCKED" -> Theme.WARNING;
			case "ON" -> ui.accent;
			default -> Theme.TEXT_FAINT;
		};
	}

	private List<FormattedCharSequence> descriptionLines(UiContext ui) {
		List<FormattedCharSequence> lines = new ArrayList<>(ui.font.split(FormattedText.of(module.description()), width - 16));
		return lines.size() > 3 ? lines.subList(0, 3) : lines;
	}

	private String notice() {
		if (!module.isAvailable()) {
			return "Unavailable: " + module.unavailableReason().orElse("");
		}
		if (module.isEnabled()) {
			return module.blockedReason().map(r -> "Paused: " + r).orElse("");
		}
		return "";
	}

	// ---------------------------------------------------------------- geometry

	private int backWidth(UiContext ui) {
		return ui.font.width("‹ Back") + 12;
	}

	boolean overBack(double mx, double my, UiContext ui) {
		return Pill.over(mx, my, x, y + 6, backWidth(ui), Pill.HEIGHT + 2);
	}

	private int toggleX() {
		return x + width - Toggle.WIDTH - 4;
	}

	boolean overToggle(double mx, double my) {
		return module.isToggleable() && Pill.over(mx, my, toggleX() - 4, y + 6, Toggle.WIDTH + 8, 18);
	}

	private int resetX(UiContext ui) {
		int right = module.isToggleable() ? toggleX() - 10 : x + width;
		return right - Pill.width(ui, reset.label());
	}

	boolean overReset(double mx, double my, UiContext ui) {
		return Pill.over(mx, my, resetX(ui), y + 8, Pill.width(ui, reset.label()), Pill.HEIGHT);
	}

	/** Toggles the module or runs the reset (second click), depending on where the click landed. */
	boolean clickHeader(double mx, double my, UiContext ui) {
		if (overToggle(mx, my)) {
			module.toggle();
			return true;
		}
		if (overReset(mx, my, ui)) {
			if (reset.click()) {
				module.resetSettings();
			}
			return true;
		}
		return false;
	}

	boolean overBody(double mx, double my) {
		return list.contains(mx, my);
	}

	// ---------------------------------------------------------------- rendering

	void render(GuiGraphics g, UiContext ui, int x, int y, int width, int height, int mouseX, int mouseY, String query) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;

		boolean backHovered = overBack(mouseX, mouseY, ui);
		Pill.button(g, ui, x, y + 8, "‹ Back", backHovered, Pill.Style.NORMAL);
		int nameX = x + backWidth(ui) + 8;
		g.pose().pushMatrix();
		g.pose().translate(nameX, y + 7);
		g.pose().scale(1.35f, 1.35f);
		int titleRoom = (int) ((resetX(ui) - nameX - 60) / 1.35f);
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, module.name(), Math.max(30, titleRoom)), 0, 0, ui.c(Theme.TEXT));
		g.pose().popMatrix();
		int afterName = nameX + (int) (Math.min(ui.font.width(module.name()), Math.max(30, titleRoom)) * 1.35f) + 6;
		Pill.status(g, ui, afterName, y + 9, status(module), statusColor(module, ui));
		Draw2D.text(g, ui.font, module.category().displayName(), nameX, y + 21, ui.c(Theme.TEXT_FAINT));

		int rx = resetX(ui);
		Pill.button(g, ui, rx, y + 8, reset.label(), overReset(mouseX, mouseY, ui), reset.armed() ? Pill.Style.DANGER : Pill.Style.NORMAL);
		if (module.isToggleable()) {
			toggle.render(g, ui, toggleX(), y + 10, module.isEnabled(), overToggle(mouseX, mouseY) ? 1 : 0);
		}

		int ty = y + HEADER + 4;
		if (ui.descriptions) {
			for (FormattedCharSequence line : descriptionLines(ui)) {
				g.drawString(ui.font, line, x + 2, ty, ui.c(Theme.TEXT_DIM), false);
				ty += 10;
			}
		}
		String notice = notice();
		if (!notice.isEmpty()) {
			int color = module.isAvailable() ? Theme.WARNING : Theme.DANGER;
			Draw2D.rounded(g, x, ty, width, 14, 4, ui.c(Colors.withAlpha(color, 40)));
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, notice, width - 12), x + 6, ty + 3, ui.c(color));
			ty += 16;
		}
		g.fill(x, ty + 2, x + width, ty + 3, ui.c(Theme.BORDER_SOFT));
		bodyTop = ty + 5;
		list.x = x - 4;
		list.y = bodyTop;
		list.width = width + 8;
		list.height = y + height - bodyTop;
		list.render(g, ui, mouseX, mouseY, query, Theme.BACKGROUND);
	}

	/** Header area tooltip (reset explanation), or null. */
	String tooltip(double mx, double my, UiContext ui) {
		if (overReset(mx, my, ui)) {
			return "Restores every setting of " + module.name() + ", including its keybind, to the default.";
		}
		if (overToggle(mx, my)) {
			return module.isEnabled() ? "Turn off" : "Turn on";
		}
		return null;
	}
}
