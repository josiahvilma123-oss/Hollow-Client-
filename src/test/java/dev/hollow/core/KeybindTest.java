package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import dev.hollow.core.input.Keybind;
import org.junit.jupiter.api.Test;

class KeybindTest {
	@Test
	void plainKeysKeepTheNumericJsonForm() {
		assertTrue(Keybind.key(71).toJson().isJsonPrimitive());
		assertEquals(Keybind.key(71), Keybind.fromJson(JsonParser.parseString("71")));
		assertEquals(Keybind.NONE, Keybind.fromJson(JsonParser.parseString("-1")));
	}

	@Test
	void mouseButtonsAndCombinationsRoundTrip() {
		Keybind combo = Keybind.key(71, Keybind.CTRL | Keybind.SHIFT);
		assertEquals(combo, Keybind.fromJson(combo.toJson()));
		Keybind mouse = Keybind.mouse(4);
		assertEquals(mouse, Keybind.fromJson(mouse.toJson()));
		assertTrue(Keybind.fromJson(mouse.toJson()).mouse());
	}

	@Test
	void invalidInputIsRejectedOrNormalised() {
		assertNull(Keybind.fromJson(JsonParser.parseString("\"G\"")));
		assertNull(Keybind.fromJson(JsonParser.parseString("{\"mouse\":true}")));
		assertFalse(new Keybind(42, true, 0).isBound(), "mouse buttons above 7 do not exist");
		assertEquals(0, new Keybind(-5, false, Keybind.CTRL).modifiers(), "an unbound key has no modifiers");
		assertEquals(Keybind.ALT, new Keybind(10, false, Keybind.ALT | 64).modifiers(), "unknown modifier bits are dropped");
	}

	@Test
	void describesModifiersInAFixedOrder() {
		Keybind combo = Keybind.key(71, Keybind.ALT | Keybind.CTRL);
		assertEquals("Ctrl + Alt + G", combo.describe(code -> "G", button -> "Mouse " + (button + 1)));
		assertEquals("Mouse 5", Keybind.mouse(4).describe(code -> "?", button -> "Mouse " + (button + 1)));
		assertEquals("None", Keybind.NONE.describe(code -> "?", button -> "?"));
	}
}
