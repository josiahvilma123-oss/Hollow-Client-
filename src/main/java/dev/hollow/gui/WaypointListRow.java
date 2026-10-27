package dev.hollow.gui;

import dev.hollow.core.util.Colors;
import dev.hollow.core.waypoints.Waypoint;
import dev.hollow.exploration.Waypoints;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** The current world's waypoints with show/hide, edit (load into the form) and delete. */
final class WaypointListRow extends Row {
	private static final int ENTRY = 26;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());
	private final Map<String, Confirm> deletes = new HashMap<>();

	private static List<Waypoint> waypoints() {
		return Waypoints.INSTANCE.store() == null ? List.of() : Waypoints.INSTANCE.store().all();
	}

	@Override
	boolean focusable() {
		return false;
	}

	@Override
	boolean matches(String query) {
		return "waypoints bases saved list".contains(query)
			|| waypoints().stream().anyMatch(w -> w.name().toLowerCase(Locale.ROOT).contains(query) || w.note().toLowerCase(Locale.ROOT).contains(query));
	}

	@Override
	int height(UiContext ui) {
		return 14 + Math.max(1, waypoints().size()) * ENTRY;
	}

	private String header() {
		String message = Waypoints.INSTANCE.message();
		if (!message.isEmpty()) {
			return message;
		}
		if (Minecraft.getInstance().level == null) {
			return "Join a world to see its waypoints.";
		}
		int count = waypoints().size();
		return count + (count == 1 ? " waypoint" : " waypoints") + " in this world";
	}

	private int[] buttons(UiContext ui, Waypoint waypoint, Confirm delete) {
		int del = Pill.width(ui, delete.label());
		int edit = Pill.width(ui, "Edit");
		int show = Pill.width(ui, waypoint.visible() ? "Hide" : "Show");
		int right = x + width - PAD;
		return new int[] {right - del - edit - show - 8, show, right - del - edit - 4, edit, right - del, del};
	}

	@Override
	void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
		Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, header(), width - PAD * 2), x + PAD, y + 3, ui.c(Theme.TEXT_DIM));
		List<Waypoint> list = waypoints();
		int ey = y + 14;
		if (list.isEmpty()) {
			Draw2D.text(g, ui.font, "No waypoints yet: add one above, or bind Add Waypoint Here.", x + PAD, ey + 8, ui.c(Theme.TEXT_FAINT));
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		String dimension = Waypoints.dimension();
		String selected = Waypoints.INSTANCE.selectedId();
		for (Waypoint waypoint : list) {
			boolean hovered = Pill.over(mouseX, mouseY, x + PAD, ey, width - PAD * 2, ENTRY - 2);
			int fill = waypoint.id().equals(selected) ? Colors.withAlpha(ui.accent, 50) : hovered ? Theme.CARD_HOVER : Theme.CARD;
			Draw2D.rounded(g, x + PAD, ey, width - PAD * 2, ENTRY - 2, 4, ui.c(fill));
			if (waypoint.id().equals(selected)) {
				Draw2D.roundedBorder(g, x + PAD, ey, width - PAD * 2, ENTRY - 2, 4, 1, ui.c(Colors.withAlpha(ui.accent, 180)));
			}
			Draw2D.circle(g, x + PAD + 7, ey + 7, 3, ui.c(waypoint.visible() ? waypoint.category().color() : Theme.OFF));
			Confirm delete = deletes.computeIfAbsent(waypoint.id(), id -> new Confirm("Delete", "Confirm"));
			int[] b = buttons(ui, waypoint, delete);
			int textWidth = b[0] - (x + PAD + 14) - 4;
			String distance = "";
			if (minecraft.player != null && waypoint.dimension().equals(dimension)) {
				distance = "  " + Math.round(waypoint.distanceTo(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ())) + " m";
			}
			String title = waypoint.name() + distance;
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, title, textWidth), x + PAD + 14, ey + 3,
				ui.c(waypoint.visible() ? Theme.TEXT : Theme.TEXT_DIM));
			String dim = waypoint.dimension().substring(waypoint.dimension().indexOf(':') + 1).replace('_', ' ');
			String detail = Waypoints.coords(waypoint) + " · " + dim + " · " + waypoint.category().displayName() + " · "
				+ DATE.format(Instant.ofEpochMilli(waypoint.created())) + (waypoint.note().isEmpty() ? "" : " · " + waypoint.note());
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, detail, textWidth), x + PAD + 14, ey + 13, ui.c(Theme.TEXT_FAINT));
			int by = ey + 5;
			Pill.button(g, ui, b[0], by, waypoint.visible() ? "Hide" : "Show", Pill.over(mouseX, mouseY, b[0], by, b[1], Pill.HEIGHT), Pill.Style.NORMAL);
			Pill.button(g, ui, b[2], by, "Edit", Pill.over(mouseX, mouseY, b[2], by, b[3], Pill.HEIGHT), Pill.Style.NORMAL);
			Pill.button(g, ui, b[4], by, delete.label(), Pill.over(mouseX, mouseY, b[4], by, b[5], Pill.HEIGHT),
				delete.armed() ? Pill.Style.DANGER : Pill.Style.NORMAL);
			ey += ENTRY;
		}
	}

	@Override
	boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) {
			return false;
		}
		int ey = y + 14;
		for (Waypoint waypoint : List.copyOf(waypoints())) {
			Confirm delete = deletes.computeIfAbsent(waypoint.id(), id -> new Confirm("Delete", "Confirm"));
			int[] b = buttons(ui, waypoint, delete);
			int by = ey + 5;
			if (Pill.over(mouseX, mouseY, b[0], by, b[1], Pill.HEIGHT)) {
				Waypoints.INSTANCE.toggleVisible(waypoint);
				return true;
			}
			if (Pill.over(mouseX, mouseY, b[2], by, b[3], Pill.HEIGHT) || Pill.over(mouseX, mouseY, x + PAD, ey, b[0] - x - PAD, ENTRY - 2)) {
				Waypoints.INSTANCE.select(waypoint.id());
				return true;
			}
			if (Pill.over(mouseX, mouseY, b[4], by, b[5], Pill.HEIGHT)) {
				if (delete.click()) {
					Waypoints.INSTANCE.delete(waypoint.id());
					deletes.remove(waypoint.id());
				}
				return true;
			}
			ey += ENTRY;
		}
		return false;
	}
}
