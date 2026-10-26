package dev.hollow.menu;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A 3x5 pixel font drawn with plain fills. The loading screen uses it because during the first
 * start-up no font or texture has been loaded yet.
 */
public final class PixelFont {
	private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789%.-:";
	private static final String[] GLYPHS = {
		"010101111101101", "110101110101110", "011100100100011", "110101101101110", "111100110100111", "111100110100100",
		"011100101101011", "101101111101101", "111010010010111", "001001001101010", "101101110101101", "100100100100111",
		"101111111101101", "110101101101101", "010101101101010", "110101110100100", "010101101110011", "110101110101101",
		"011100010001110", "111010010010010", "101101101101111", "101101101101010", "101101111111101", "101101010101101",
		"101101010010010", "111001010100111",
		"111101101101111", "010110010010111", "110001010100111", "110001010001110", "101101111001001", "111100110001110",
		"011100111101111", "111001010010010", "111101111101111", "111101111001110",
		"101001010100101", "000000000000010", "000000111000000", "000010000010000",
	};
	private static final int[] MASKS = new int[128];

	static {
		for (int i = 0; i < CHARS.length(); i++) {
			MASKS[CHARS.charAt(i)] = Integer.parseInt(GLYPHS[i], 2);
		}
	}

	private PixelFont() {
	}

	/** Width in GUI pixels of {@code text} at the given pixel size and letter spacing (in pixels). */
	public static int width(String text, int pixel, int spacing) {
		if (text.isEmpty()) {
			return 0;
		}
		return text.length() * (3 * pixel + spacing) - spacing;
	}

	public static void draw(GuiGraphics g, String text, int x, int y, int pixel, int spacing, int color) {
		int cursor = x;
		for (int i = 0; i < text.length(); i++) {
			char c = Character.toUpperCase(text.charAt(i));
			int mask = c < 128 ? MASKS[c] : 0;
			for (int row = 0; row < 5; row++) {
				int bits = (mask >> ((4 - row) * 3)) & 7;
				// Merge horizontal runs into one fill.
				int col = 0;
				while (col < 3) {
					if ((bits & (4 >> col)) == 0) {
						col++;
						continue;
					}
					int start = col;
					while (col < 3 && (bits & (4 >> col)) != 0) {
						col++;
					}
					g.fill(cursor + start * pixel, y + row * pixel, cursor + col * pixel, y + (row + 1) * pixel, color);
				}
			}
			cursor += 3 * pixel + spacing;
		}
	}

	public static void drawCentered(GuiGraphics g, String text, int centerX, int y, int pixel, int spacing, int color) {
		draw(g, text, centerX - width(text, pixel, spacing) / 2, y, pixel, spacing, color);
	}
}
