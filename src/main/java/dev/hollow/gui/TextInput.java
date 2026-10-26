package dev.hollow.gui;

import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** A minimal single-line text field styled for Hollow. */
final class TextInput {
	private final String placeholder;
	private final int maxLength;
	private String text = "";
	private int cursor;
	private boolean focused;
	private Predicate<String> validator = s -> true;

	TextInput(String placeholder, int maxLength) {
		this.placeholder = placeholder;
		this.maxLength = maxLength;
	}

	TextInput validator(Predicate<String> validator) {
		this.validator = validator;
		return this;
	}

	String text() {
		return text;
	}

	void setText(String value) {
		text = value.length() > maxLength ? value.substring(0, maxLength) : value;
		cursor = text.length();
	}

	boolean focused() {
		return focused;
	}

	void focus(boolean value) {
		focused = value;
		cursor = text.length();
	}

	boolean valid() {
		return text.isEmpty() || validator.test(text);
	}

	void render(GuiGraphics g, UiContext ui, int x, int y, int w, int h) {
		int border = !valid() ? Theme.DANGER : focused ? ui.accent : Theme.BORDER;
		Draw2D.panel(g, x, y, w, h, ui.c(0xFF0A080D));
		Draw2D.border(g, x, y, w, h, ui.c(border));
		int textY = y + (h - 8) / 2;
		if (text.isEmpty() && !focused) {
			Draw2D.text(g, ui.font, Draw2D.ellipsize(ui.font, placeholder, w - 8), x + 4, textY, ui.c(Theme.TEXT_FAINT));
			return;
		}
		String visible = text;
		int available = w - 8;
		int offset = 0;
		while (ui.font.width(visible.substring(0, Math.min(visible.length(), cursor - offset))) > available && offset < cursor) {
			offset++;
			visible = text.substring(offset);
		}
		visible = Draw2D.ellipsize(ui.font, visible, available + 6).replace("…", "");
		Draw2D.text(g, ui.font, visible, x + 4, textY, ui.c(Theme.TEXT));
		if (focused && System.currentTimeMillis() / 500 % 2 == 0) {
			int caretX = x + 4 + ui.font.width(text.substring(offset, Math.max(offset, cursor)));
			g.fill(caretX, textY - 1, caretX + 1, textY + 9, ui.c(ui.accentLight));
		}
	}

	boolean keyPressed(KeyEvent event) {
		if (!focused) {
			return false;
		}
		int key = event.key();
		if (event.isPaste()) {
			insert(Minecraft.getInstance().keyboardHandler.getClipboard());
			return true;
		}
		if (event.isSelectAll()) {
			cursor = text.length();
			return true;
		}
		switch (key) {
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (cursor > 0) {
					if (event.hasControlDown()) {
						text = text.substring(cursor);
						cursor = 0;
					} else {
						text = text.substring(0, cursor - 1) + text.substring(cursor);
						cursor--;
					}
				}
				return true;
			}
			case GLFW.GLFW_KEY_DELETE -> {
				if (cursor < text.length()) {
					text = text.substring(0, cursor) + text.substring(cursor + 1);
				}
				return true;
			}
			case GLFW.GLFW_KEY_LEFT -> {
				cursor = Math.max(0, cursor - 1);
				return true;
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				cursor = Math.min(text.length(), cursor + 1);
				return true;
			}
			case GLFW.GLFW_KEY_HOME -> {
				cursor = 0;
				return true;
			}
			case GLFW.GLFW_KEY_END -> {
				cursor = text.length();
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	boolean charTyped(CharacterEvent event) {
		if (!focused || !event.isAllowedChatCharacter()) {
			return false;
		}
		insert(event.codepointAsString());
		return true;
	}

	private void insert(String value) {
		String clean = value.replaceAll("[\\r\\n\\t]", "");
		int room = maxLength - text.length();
		if (room <= 0 || clean.isEmpty()) {
			return;
		}
		if (clean.length() > room) {
			clean = clean.substring(0, room);
		}
		text = text.substring(0, cursor) + clean + text.substring(cursor);
		cursor += clean.length();
	}
}
