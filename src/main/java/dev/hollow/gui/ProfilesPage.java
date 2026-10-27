package dev.hollow.gui;

import dev.hollow.Hollow;
import dev.hollow.core.config.ProfileStore;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

/**
 * Profile management: save the current setup under a name, load, rename, export to the clipboard,
 * import, delete, and reset everything to defaults.
 */
final class ProfilesPage {
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d yyyy, HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());
	final RowList list;
	private final Consumer<String> notify;

	ProfilesPage(Consumer<String> notify) {
		this.notify = notify;
		this.list = new RowList(List.of(), "");
		refresh();
	}

	private static ProfileStore store() {
		return Hollow.profiles();
	}

	/** Re-reads the profile folder and rebuilds the rows. */
	void refresh() {
		List<Row> rows = new ArrayList<>();
		rows.add(new InfoRow("A profile stores every module's on/off state, settings, keybinds, theme and HUD layout. "
			+ "Loading one replaces your current setup, so save it first if you want to keep it."));
		rows.add(new InfoRow(() -> {
			String active = Hollow.config().activeProfile();
			return active.isEmpty() ? "Active profile: none (unsaved setup)" : "Active profile: " + active;
		}, Theme.TEXT));
		rows.add(new HeaderRow("profiles", "Save Current Setup", false));
		rows.add(new CreateRow());
		rows.add(new HeaderRow("profiles", "Saved Profiles", false));
		List<ProfileStore.Profile> profiles = store() == null ? List.of() : store().list();
		if (profiles.isEmpty()) {
			rows.add(new InfoRow("No profiles saved yet."));
		}
		for (ProfileStore.Profile profile : profiles) {
			rows.add(new EntryRow(profile));
		}
		rows.add(new HeaderRow("profiles", "Share & Reset", false));
		rows.add(new ButtonsRow());
		rows.add(new ConfirmRow("Reset Everything", "Returns every module, setting, keybind and the HUD layout to defaults. "
			+ "Saved profiles, waypoints and sky presets are kept.", "Reset all", () -> {
				Hollow.config().resetAll();
				Hollow.config().activeProfile("");
				notify.accept("Everything was reset to defaults");
			}));
		list.replace(rows);
	}

	private void run(String what, IoAction action) {
		try {
			action.run();
		} catch (IOException | RuntimeException e) {
			notify.accept(what + " failed: " + e.getMessage());
			Hollow.LOGGER.warn("{} failed", what, e);
		}
	}

	private interface IoAction {
		void run() throws IOException;
	}

	// ---------------------------------------------------------------- rows

	/** Name field plus Save; saving over an existing name needs a second click. */
	private final class CreateRow extends Row {
		private final TextInput input = new TextInput("Profile name", ProfileStore.MAX_NAME_LENGTH)
			.validator(s -> !ProfileStore.sanitizeName(s).isEmpty());
		private final Confirm overwrite = new Confirm("Save", "Overwrite?");

		@Override
		int height(UiContext ui) {
			return 22;
		}

		private String checkedName;
		private boolean nameTaken;

		/** "Save", or the overwrite confirmation when the name is taken (checked on disk only when the text changes). */
		private String buttonLabel() {
			String name = ProfileStore.sanitizeName(input.text());
			if (!name.equals(checkedName)) {
				checkedName = name;
				nameTaken = !name.isEmpty() && store() != null && store().exists(name);
				overwrite.disarm();
			}
			return nameTaken ? overwrite.label() : "Save";
		}

		@Override
		void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
			int bw = Pill.width(ui, buttonLabel());
			input.render(g, ui, x + PAD, y + 3, width - PAD * 3 - bw, 15);
			int bx = x + width - PAD - bw;
			Pill.button(g, ui, bx, y + 3, buttonLabel(), Pill.over(mouseX, mouseY, bx, y + 3, bw, Pill.HEIGHT),
				overwrite.armed() ? Pill.Style.DANGER : Pill.Style.PRIMARY);
		}

		private void save() {
			String name = ProfileStore.sanitizeName(input.text());
			if (name.isEmpty()) {
				notify.accept("Enter a profile name (letters, numbers, spaces, - and _)");
				return;
			}
			if (store().exists(name) && !overwrite.click()) {
				return;
			}
			overwrite.disarm();
			run("Saving profile", () -> {
				String saved = store().saveCurrent(name, System.currentTimeMillis());
				notify.accept("Saved profile " + saved);
				input.setText("");
				input.focus(false);
				checkedName = null;
				refresh();
			});
		}

		@Override
		boolean mouseClicked(double mouseX, double mouseY, int button) {
			int bw = Pill.width(ui, buttonLabel());
			if (Pill.over(mouseX, mouseY, x + width - PAD - bw, y + 3, bw, Pill.HEIGHT)) {
				input.focus(false);
				save();
				return true;
			}
			input.focus(true);
			return true;
		}

		@Override
		boolean activate() {
			input.focus(true);
			return true;
		}

		@Override
		boolean capturesKeyboard() {
			return input.focused();
		}

		@Override
		boolean cancelInput() {
			boolean was = input.focused();
			input.focus(false);
			return was;
		}

		@Override
		boolean keyPressed(KeyEvent event) {
			if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
				save();
				return true;
			}
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				input.focus(false);
				return true;
			}
			return input.keyPressed(event);
		}

		@Override
		boolean charTyped(CharacterEvent event) {
			return input.charTyped(event);
		}
	}

	/** One saved profile: Load, Rename, Export and Delete. */
	private final class EntryRow extends Row {
		private final ProfileStore.Profile profile;
		private final Confirm load = new Confirm("Load", "Replace setup?");
		private final Confirm delete = new Confirm("Delete", "Confirm");
		private final TextInput rename = new TextInput("New name", ProfileStore.MAX_NAME_LENGTH)
			.validator(s -> !ProfileStore.sanitizeName(s).isEmpty());
		private boolean renaming;

		EntryRow(ProfileStore.Profile profile) {
			this.profile = profile;
		}

		@Override
		boolean matches(String query) {
			return profile.name().toLowerCase(Locale.ROOT).contains(query);
		}

		@Override
		int height(UiContext ui) {
			return 28;
		}

		private List<String> labels() {
			return renaming ? List.of("Save", "Cancel") : List.of(load.label(), "Rename", "Export", delete.label());
		}

		/** Left edges of the buttons, right-aligned. */
		private int[] buttonXs(UiContext ui) {
			List<String> labels = labels();
			int[] xs = new int[labels.size()];
			int right = x + width - PAD;
			for (int i = labels.size() - 1; i >= 0; i--) {
				right -= Pill.width(ui, labels.get(i));
				xs[i] = right;
				right -= 4;
			}
			return xs;
		}

		@Override
		void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
			boolean active = profile.name().equalsIgnoreCase(Hollow.config().activeProfile());
			boolean hovered = Pill.over(mouseX, mouseY, x + PAD, y, width - PAD * 2, 26);
			Draw2D.rounded(g, x + PAD, y, width - PAD * 2, 26, 4, ui.c(hovered ? Theme.CARD_HOVER : Theme.CARD));
			if (active) {
				Draw2D.roundedBorder(g, x + PAD, y, width - PAD * 2, 26, 4, 1, ui.c(ui.detail));
			}
			int[] xs = buttonXs(ui);
			List<String> labels = labels();
			int textRight = xs[0] - 6;
			if (renaming) {
				rename.render(g, ui, x + PAD + 5, y + 6, textRight - x - PAD - 5, 14);
			} else {
				int nameWidth = Math.min(ui.font.width(profile.name()), textRight - x - PAD - 50);
				Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, profile.name(), textRight - x - PAD - 50), x + PAD + 6, y + 4, ui.c(Theme.TEXT));
				if (active) {
					Pill.status(g, ui, x + PAD + 10 + nameWidth, y + 3, "ACTIVE", ui.detail);
				}
				String saved = profile.savedAt() > 0 ? "Saved " + DATE.format(Instant.ofEpochMilli(profile.savedAt())) : "";
				Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, saved, textRight - x - PAD - 6), x + PAD + 6, y + 15, ui.c(Theme.TEXT_FAINT));
			}
			for (int i = 0; i < labels.size(); i++) {
				String label = labels.get(i);
				boolean danger = label.equals(delete.label()) && delete.armed() || label.equals(load.label()) && load.armed();
				Pill.button(g, ui, xs[i], y + 6, label, Pill.over(mouseX, mouseY, xs[i], y + 6, Pill.width(ui, label), Pill.HEIGHT),
					danger ? Pill.Style.DANGER : i == 0 ? Pill.Style.PRIMARY : Pill.Style.NORMAL);
			}
		}

		@Override
		boolean mouseClicked(double mouseX, double mouseY, int button) {
			if (button != 0) {
				return false;
			}
			int[] xs = buttonXs(ui);
			List<String> labels = labels();
			for (int i = 0; i < labels.size(); i++) {
				if (Pill.over(mouseX, mouseY, xs[i], y + 6, Pill.width(ui, labels.get(i)), Pill.HEIGHT)) {
					press(i);
					return true;
				}
			}
			if (renaming) {
				rename.focus(Pill.over(mouseX, mouseY, x + PAD, y, xs[0] - x - PAD, 26));
			}
			return true;
		}

		private void press(int index) {
			if (renaming) {
				if (index == 0) {
					commitRename();
				} else {
					renaming = false;
					rename.focus(false);
				}
				return;
			}
			switch (index) {
				case 0 -> {
					if (load.click()) {
						run("Loading profile", () -> {
							store().load(profile.name());
							notify.accept("Loaded profile " + profile.name());
						});
					}
				}
				case 1 -> {
					renaming = true;
					rename.setText(profile.name());
					rename.focus(true);
				}
				case 2 -> run("Export", () -> {
					Minecraft.getInstance().keyboardHandler.setClipboard(store().export(profile.name()));
					notify.accept("Copied profile " + profile.name() + " to the clipboard");
				});
				default -> {
					if (delete.click()) {
						run("Deleting profile", () -> {
							store().delete(profile.name());
							notify.accept("Deleted profile " + profile.name());
							refresh();
						});
					}
				}
			}
		}

		private void commitRename() {
			run("Renaming profile", () -> {
				String name = store().rename(profile.name(), rename.text());
				notify.accept("Renamed to " + name);
				refresh();
			});
		}

		@Override
		boolean activate() {
			press(0);
			return true;
		}

		@Override
		boolean capturesKeyboard() {
			return renaming && rename.focused();
		}

		@Override
		boolean cancelInput() {
			boolean was = renaming;
			renaming = false;
			rename.focus(false);
			return was;
		}

		@Override
		boolean keyPressed(KeyEvent event) {
			if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
				commitRename();
				return true;
			}
			if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
				cancelInput();
				return true;
			}
			return rename.keyPressed(event);
		}

		@Override
		boolean charTyped(CharacterEvent event) {
			return rename.charTyped(event);
		}
	}

	/** Import from the clipboard and open the profile folder. */
	private final class ButtonsRow extends Row {
		private static final String IMPORT = "Import from clipboard";
		private static final String FOLDER = "Open profile folder";

		@Override
		int height(UiContext ui) {
			return 20;
		}

		@Override
		boolean matches(String query) {
			return "import export folder share".contains(query);
		}

		@Override
		void render(GuiGraphics g, UiContext ui, int mouseX, int mouseY) {
			int ix = x + PAD;
			int iw = Pill.button(g, ui, ix, y + 3, IMPORT, Pill.over(mouseX, mouseY, ix, y + 3, Pill.width(ui, IMPORT), Pill.HEIGHT), Pill.Style.NORMAL);
			int fx = ix + iw + 4;
			Pill.button(g, ui, fx, y + 3, FOLDER, Pill.over(mouseX, mouseY, fx, y + 3, Pill.width(ui, FOLDER), Pill.HEIGHT), Pill.Style.NORMAL);
		}

		@Override
		boolean mouseClicked(double mouseX, double mouseY, int button) {
			int ix = x + PAD;
			int iw = Pill.width(ui, IMPORT);
			if (Pill.over(mouseX, mouseY, ix, y + 3, iw, Pill.HEIGHT)) {
				return activate();
			}
			if (Pill.over(mouseX, mouseY, ix + iw + 4, y + 3, Pill.width(ui, FOLDER), Pill.HEIGHT)) {
				run("Opening folder", () -> {
					Files.createDirectories(store().directory());
					Util.getPlatform().openPath(store().directory());
				});
				return true;
			}
			return false;
		}

		@Override
		boolean activate() {
			run("Import", () -> {
				String name = store().importJson(Minecraft.getInstance().keyboardHandler.getClipboard());
				notify.accept("Imported profile " + name + ". Load it to apply.");
				refresh();
			});
			return true;
		}
	}
}
