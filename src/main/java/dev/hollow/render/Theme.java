package dev.hollow.render;

import dev.hollow.Hollow;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.InterfaceModule;

/** Holo's dark violet palette. The accent and detail colors follow the Interface module's theme. */
public final class Theme {
	public static final int VOID = 0xFF07060A;
	public static final int BACKGROUND = 0xFF0C0A10;
	public static final int PANEL = 0xFF121017;
	public static final int CARD = 0xFF19151F;
	public static final int CARD_HOVER = 0xFF211B29;
	public static final int BORDER = 0xFF2A2234;
	public static final int BORDER_SOFT = 0xFF1F1A27;
	public static final int TEXT = 0xFFECE6F5;
	public static final int TEXT_DIM = 0xFFA398B3;
	public static final int TEXT_FAINT = 0xFF6E6480;
	public static final int OFF = 0xFF3A3244;
	public static final int DANGER = 0xFFE0405E;
	public static final int DEFAULT_ACCENT = 0xFF9D4EDD;
	public static final int DEFAULT_DETAIL = 0xFFE6B54A;
	public static final int WARNING = 0xFFE8A03A;

	private Theme() {
	}

	public static int accent() {
		try {
			return Hollow.module(InterfaceModule.class).accent.argb();
		} catch (RuntimeException e) {
			return DEFAULT_ACCENT;
		}
	}

	/** The ornament color (gold by default): wordmark, focus outlines and guides. */
	public static int detail() {
		try {
			return Hollow.module(InterfaceModule.class).detail.argb();
		} catch (RuntimeException e) {
			return DEFAULT_DETAIL;
		}
	}

	/** A lighter variant of the accent for text and highlights. */
	public static int accentLight() {
		return Colors.lerp(accent(), 0xFFFFFFFF, 0.35f);
	}

	public static int accentDark() {
		return Colors.lerp(accent(), 0xFF000000, 0.55f);
	}

	public static int accent(float alpha) {
		return Colors.withAlpha(accent(), Math.round(alpha * 255));
	}
}
