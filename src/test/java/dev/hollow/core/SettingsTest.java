package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.hollow.core.anim.Easing;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.StringListSetting;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingsTest {
	@Test
	void numberSettingClampsAndSnapsToStep() {
		NumberSetting setting = new NumberSetting("range", "Range", "", 32, 8, 128, 4);
		setting.set(130.0);
		assertEquals(128.0, setting.get());
		setting.set(33.9);
		assertEquals(32.0, setting.get());
		setting.set(-5.0);
		assertEquals(8.0, setting.get());
	}

	@Test
	void numberSettingTrimsFloatingPointNoise() {
		NumberSetting setting = new NumberSetting("i", "Intensity", "", 0.5, 0, 1, 0.05);
		setting.set(0.3);
		assertEquals(0.3, setting.get());
		assertEquals("0.30", setting.formatted());
	}

	@Test
	void numberSettingRejectsInvalidDefaults() {
		assertThrows(IllegalArgumentException.class, () -> new NumberSetting("x", "X", "", 5, 10, 1, 1));
		assertThrows(IllegalArgumentException.class, () -> new NumberSetting("x", "X", "", 50, 0, 10, 1));
	}

	@Test
	void numberSettingIgnoresWrongJsonTypes() {
		NumberSetting setting = new NumberSetting("x", "X", "", 5, 0, 10, 1);
		setting.fromJson(new JsonPrimitive("not a number"));
		assertEquals(5.0, setting.get());
		setting.fromJson(new JsonPrimitive(7));
		assertEquals(7.0, setting.get());
	}

	@Test
	void listenersFireOnlyOnRealChanges() {
		BoolSetting setting = new BoolSetting("b", "B", "", false);
		List<Boolean> seen = new ArrayList<>();
		setting.onChange(seen::add);
		setting.set(false);
		setting.set(true);
		setting.set(true);
		setting.toggle();
		assertEquals(List.of(true, false), seen);
	}

	@Test
	void colorSettingRoundTripsThroughHex() {
		ColorSetting color = new ColorSetting("c", "Color", "", 0x809D4EDD);
		assertEquals("\"#809D4EDD\"", color.toJson().toString());
		color.fromJson(JsonParser.parseString("\"#FF00FF\""));
		assertEquals(0xFFFF00FF, color.argb());
		color.fromJson(JsonParser.parseString("\"garbage\""));
		assertEquals(0xFFFF00FF, color.argb());
	}

	@Test
	void opaqueColorSettingForcesFullAlpha() {
		ColorSetting color = new ColorSetting("c", "Color", "", 0x009D4EDD, false);
		assertEquals(0xFF9D4EDD, color.argb());
		color.set(0x10123456);
		assertEquals(0xFF123456, color.argb());
	}

	@Test
	void enumSettingCyclesAndLoadsCaseInsensitively() {
		EnumSetting<Easing> setting = new EnumSetting<>("e", "Easing", "", Easing.LINEAR);
		setting.cycle(-1);
		assertEquals(Easing.BACK_OUT, setting.get());
		setting.fromJson(new JsonPrimitive("cubic_out"));
		assertEquals(Easing.CUBIC_OUT, setting.get());
		assertEquals("Cubic Out", setting.displayValue());
	}

	@Test
	void stringListNormalizesAndValidates() {
		StringListSetting list = new StringListSetting("l", "List", "", List.of("Minecraft:Diamond_Ore", " minecraft:diamond_ore "),
			s -> s.contains(":"), "");
		assertEquals(List.of("minecraft:diamond_ore"), list.get());
		assertFalse(list.add("no_namespace"));
		assertTrue(list.add("minecraft:ancient_debris"));
		assertFalse(list.add("MINECRAFT:ANCIENT_DEBRIS"));
		assertTrue(list.remove("minecraft:diamond_ore"));
		assertEquals(List.of("minecraft:ancient_debris"), list.get());
	}

	@Test
	void settingSearchMatchesNameDescriptionAndGroup() {
		BoolSetting setting = new BoolSetting("t", "Tracers", "Draw lines to blocks", true);
		setting.group("Diamond Ore");
		assertTrue(setting.matches("trac"));
		assertTrue(setting.matches("LINES"));
		assertTrue(setting.matches("diamond"));
		assertFalse(setting.matches("emerald"));
		assertTrue(setting.matches("  "));
	}
}
