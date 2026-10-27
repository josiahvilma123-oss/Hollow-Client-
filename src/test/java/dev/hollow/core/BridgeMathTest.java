package dev.hollow.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.hollow.core.bridge.BridgeMath;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BridgeMathTest {
	/** Solid blocks in a set; everything else is air. */
	private static final class Blocks implements BridgeMath.World {
		final Set<String> solid = new HashSet<>();

		Blocks add(int x, int y, int z) {
			solid.add(x + "," + y + "," + z);
			return this;
		}

		@Override
		public boolean solid(int x, int y, int z) {
			return solid.contains(x + "," + y + "," + z);
		}

		@Override
		public boolean replaceable(int x, int y, int z) {
			return !solid(x, y, z);
		}
	}

	@Test
	void placesUnderThePlayerAgainstTheBlockBehind() {
		Blocks world = new Blocks().add(0, 63, 0).add(1, 63, 1);
		// Walking +x off block (0,63,0): the target is (1,63,0) and the click is on (0,63,0)'s +x face.
		BridgeMath.Placement p = BridgeMath.plan(world, 1.2, 64, 0.5, 1, 0);
		assertNotNull(p);
		assertEquals(1, p.targetX());
		assertEquals(63, p.targetY());
		assertEquals(0, p.neighborX());
		assertEquals(1, p.faceX());
		assertEquals(0, p.faceY());
	}

	@Test
	void prefersBehindThenOtherSidesThenBelow() {
		Blocks sideOnly = new Blocks().add(1, 63, 1);
		BridgeMath.Placement side = BridgeMath.plan(sideOnly, 1.5, 64, 0.5, 1, 0);
		assertNotNull(side);
		assertEquals(1, side.neighborZ());
		assertEquals(-1, side.faceZ());

		Blocks belowOnly = new Blocks().add(1, 62, 0);
		BridgeMath.Placement below = BridgeMath.plan(belowOnly, 1.5, 64, 0.5, 1, 0);
		assertNotNull(below);
		assertEquals(1, below.faceY());
	}

	@Test
	void noPlanWhenFilledOrNothingToClick() {
		assertNull(BridgeMath.plan(new Blocks().add(1, 63, 0), 1.5, 64, 0.5, 1, 0), "already filled");
		assertNull(BridgeMath.plan(new Blocks(), 1.5, 64, 0.5, 1, 0), "would be an invalid floating placement");
	}

	@Test
	void edgeDetectionLooksAheadInTheMovementDirection() {
		Blocks world = new Blocks().add(0, 63, 0);
		assertTrue(BridgeMath.atEdge(world, 0.8, 64, 0.5, 1, 0, 0.3));
		assertFalse(BridgeMath.atEdge(world, 0.5, 64, 0.5, 1, 0, 0.3));
		assertFalse(BridgeMath.atEdge(world, 0.8, 64, 0.5, 0, 0, 0.3), "standing still is never an edge");
		assertEquals(0.2, BridgeMath.distanceToEdge(0.8, 0.5, 1, 0), 1e-9);
		assertEquals(0.8, BridgeMath.distanceToEdge(0.8, 0.5, -1, 0), 1e-9);
	}

	@Test
	void supportBlockIsBelowTheFeet() {
		assertEquals(63, BridgeMath.supportY(64.0));
		assertEquals(63, BridgeMath.supportY(64.4));
		assertEquals(64, BridgeMath.supportY(64.5), "half slabs and above count as the block itself");
	}
}
