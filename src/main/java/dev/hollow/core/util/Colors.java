package dev.hollow.core.util;

import java.util.Locale;

/** ARGB color helpers shared by rendering and the GUI. */
public final class Colors {
	private Colors() {
	}

	public static int alpha(int argb) {
		return argb >>> 24;
	}

	public static int red(int argb) {
		return argb >> 16 & 0xFF;
	}

	public static int green(int argb) {
		return argb >> 8 & 0xFF;
	}

	public static int blue(int argb) {
		return argb & 0xFF;
	}

	public static int argb(int a, int r, int g, int b) {
		return clamp255(a) << 24 | clamp255(r) << 16 | clamp255(g) << 8 | clamp255(b);
	}

	public static int withAlpha(int argb, int alpha) {
		return clamp255(alpha) << 24 | argb & 0x00FFFFFF;
	}

	/** Multiplies the existing alpha channel by {@code factor} (0..1). */
	public static int fade(int argb, float factor) {
		return withAlpha(argb, Math.round(alpha(argb) * clamp01(factor)));
	}

	public static int lerp(int from, int to, float t) {
		t = clamp01(t);
		return argb(
			Math.round(alpha(from) + (alpha(to) - alpha(from)) * t),
			Math.round(red(from) + (red(to) - red(from)) * t),
			Math.round(green(from) + (green(to) - green(from)) * t),
			Math.round(blue(from) + (blue(to) - blue(from)) * t));
	}

	/** Converts HSV (all components 0..1) plus alpha (0..255) to ARGB. */
	public static int hsv(float hue, float saturation, float value, int alpha) {
		float h = (hue - (float) Math.floor(hue)) * 6f;
		float s = clamp01(saturation);
		float v = clamp01(value);
		int sector = (int) Math.floor(h) % 6;
		float f = h - (float) Math.floor(h);
		float p = v * (1 - s);
		float q = v * (1 - f * s);
		float t = v * (1 - (1 - f) * s);
		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return argb(alpha, Math.round(r * 255), Math.round(g * 255), Math.round(b * 255));
	}

	/** Returns {hue, saturation, value}, each in 0..1. */
	public static float[] toHsv(int argb) {
		float r = red(argb) / 255f;
		float g = green(argb) / 255f;
		float b = blue(argb) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float hue;
		if (delta == 0) {
			hue = 0;
		} else if (max == r) {
			hue = ((g - b) / delta % 6 + 6) % 6 / 6f;
		} else if (max == g) {
			hue = ((b - r) / delta + 2) / 6f;
		} else {
			hue = ((r - g) / delta + 4) / 6f;
		}
		float saturation = max == 0 ? 0 : delta / max;
		return new float[] {hue, saturation, max};
	}

	public static String toHex(int argb) {
		return String.format(Locale.ROOT, "#%08X", argb);
	}

	/** Parses "#RRGGBB", "#AARRGGBB" (leading '#' optional). Returns null when invalid. */
	public static Integer parseHex(String text) {
		if (text == null) {
			return null;
		}
		String hex = text.trim();
		if (hex.startsWith("#")) {
			hex = hex.substring(1);
		}
		if (hex.length() != 6 && hex.length() != 8) {
			return null;
		}
		try {
			long parsed = Long.parseLong(hex, 16);
			return hex.length() == 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int clamp255(int value) {
		return Math.max(0, Math.min(255, value));
	}

	private static float clamp01(float value) {
		return Math.max(0f, Math.min(1f, value));
	}
}
