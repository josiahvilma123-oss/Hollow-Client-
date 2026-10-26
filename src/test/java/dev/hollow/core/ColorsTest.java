package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.hollow.core.util.Colors;
import org.junit.jupiter.api.Test;

class ColorsTest {
	@Test
	void hsvRoundTripPreservesColor() {
		int[] samples = {0xFF9D4EDD, 0xFF000000, 0xFFFFFFFF, 0xFFE0405E, 0xFF5FD68A, 0xFF123456};
		for (int argb : samples) {
			float[] hsv = Colors.toHsv(argb);
			int back = Colors.hsv(hsv[0], hsv[1], hsv[2], 255);
			assertEquals(Colors.red(argb), Colors.red(back), 1);
			assertEquals(Colors.green(argb), Colors.green(back), 1);
			assertEquals(Colors.blue(argb), Colors.blue(back), 1);
		}
	}

	@Test
	void parseHexAcceptsSixAndEightDigits() {
		assertEquals(0xFF9D4EDD, Colors.parseHex("#9D4EDD"));
		assertEquals(0x409D4EDD, Colors.parseHex("409d4edd"));
		assertNull(Colors.parseHex("#12345"));
		assertNull(Colors.parseHex("#GGGGGG"));
		assertNull(Colors.parseHex(null));
	}

	@Test
	void fadeAndLerp() {
		assertEquals(0x80FFFFFF, Colors.fade(0xFFFFFFFF, 128 / 255f));
		assertEquals(0xFF7F7F7F, Colors.lerp(0xFF000000, 0xFFFEFEFE, 0.5f));
	}
}
