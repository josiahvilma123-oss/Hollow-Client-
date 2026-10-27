package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hollow.core.hud.HudSnap;
import java.util.List;
import org.junit.jupiter.api.Test;

class HudSnapTest {
	@Test
	void snapsToScreenEdgesCentreAndOtherElements() {
		HudSnap.Result edge = HudSnap.snap(new HudSnap.Box(7, 300, 50, 20), 800, 600, 4, 5, List.of());
		assertEquals(4, edge.x(), 1e-4);
		assertTrue(edge.guides().stream().anyMatch(g -> g.vertical() && g.position() == 4));

		HudSnap.Result centre = HudSnap.snap(new HudSnap.Box(373, 100, 50, 20), 800, 600, 4, 5, List.of());
		assertEquals(375, centre.x(), 1e-4, "box centre snaps to the screen centre");

		HudSnap.Result beside = HudSnap.snap(new HudSnap.Box(200, 152, 40, 10), 800, 600, 4, 5, List.of(new HudSnap.Box(100, 150, 60, 10)));
		assertEquals(150, beside.y(), 1e-4, "top edge snaps to another element's top edge");
	}

	@Test
	void leavesFarBoxesAloneButKeepsThemOnScreen() {
		HudSnap.Result free = HudSnap.snap(new HudSnap.Box(200, 200, 50, 20), 800, 600, 4, 5, List.of());
		assertEquals(200, free.x(), 1e-4);
		assertTrue(free.guides().isEmpty());
		HudSnap.Result outside = HudSnap.snap(new HudSnap.Box(900, -40, 50, 20), 800, 600, 4, 5, List.of());
		assertEquals(750, outside.x(), 1e-4);
		assertEquals(0, outside.y(), 1e-4);
	}

	@Test
	void fractionsKeepRelativePlaceAcrossResolutions() {
		float fraction = HudSnap.toFraction(750, 50, 800);
		assertEquals(1f, fraction, 1e-4);
		assertEquals(1870, HudSnap.fromFraction(fraction, 50, 1920), 1e-3, "right-aligned stays right-aligned");
		assertEquals(0.5f, HudSnap.toFraction(375, 50, 800), 1e-4);
		assertEquals(0f, HudSnap.toFraction(10, 900, 800), "an element wider than the screen pins to 0");
	}
}
