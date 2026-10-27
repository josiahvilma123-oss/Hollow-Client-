package dev.hollow.core.bridge;

/**
 * Bridging geometry, independent of Minecraft: where the next block goes and which existing
 * block face to click so the placement is an ordinary, valid one.
 */
public final class BridgeMath {
	/** Read-only view of the blocks around the player. */
	public interface World {
		/** A block that can be clicked to place against (solid, with a collision box). */
		boolean solid(int x, int y, int z);

		/** A block a placement may replace (air, grass, water...). */
		boolean replaceable(int x, int y, int z);
	}

	/**
	 * Place a block at the target by clicking the neighbour's face that touches it.
	 *
	 * @param faceX direction from the neighbour towards the target (the clicked face's normal)
	 */
	public record Placement(int targetX, int targetY, int targetZ, int neighborX, int neighborY, int neighborZ, int faceX, int faceY,
			int faceZ) {
	}

	private static final int[][] HORIZONTAL = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

	private BridgeMath() {
	}

	/** Y of the block a player at this feet height stands on. */
	public static int supportY(double feetY) {
		return (int) Math.floor(feetY - 0.5);
	}

	/**
	 * Placement that fills the block under (x, z) at the support level, or null when it is
	 * already filled or nothing adjacent can be clicked. The block the player came from (opposite
	 * the movement direction) is preferred, then the other sides, then the block below.
	 */
	public static Placement plan(World world, double x, double feetY, double z, double moveX, double moveZ) {
		int tx = (int) Math.floor(x);
		int ty = supportY(feetY);
		int tz = (int) Math.floor(z);
		if (!world.replaceable(tx, ty, tz)) {
			return null;
		}
		int[][] order = new int[5][];
		int i = 0;
		int[] behind = behind(moveX, moveZ);
		if (behind != null) {
			order[i++] = behind;
		}
		for (int[] dir : HORIZONTAL) {
			if (behind == null || dir[0] != behind[0] || dir[2] != behind[2]) {
				order[i++] = dir;
			}
		}
		order[i] = new int[] {0, -1, 0};
		for (int[] dir : order) {
			if (dir == null) {
				continue;
			}
			int nx = tx + dir[0];
			int ny = ty + dir[1];
			int nz = tz + dir[2];
			if (world.solid(nx, ny, nz)) {
				return new Placement(tx, ty, tz, nx, ny, nz, -dir[0], -dir[1], -dir[2]);
			}
		}
		return null;
	}

	/** The horizontal direction opposite the dominant movement axis, or null when standing still. */
	static int[] behind(double moveX, double moveZ) {
		if (Math.abs(moveX) < 1e-4 && Math.abs(moveZ) < 1e-4) {
			return null;
		}
		if (Math.abs(moveX) >= Math.abs(moveZ)) {
			return new int[] {moveX > 0 ? -1 : 1, 0, 0};
		}
		return new int[] {0, 0, moveZ > 0 ? -1 : 1};
	}

	/**
	 * Whether the player is at an edge: the support block a short distance ahead in the movement
	 * direction is open, while the one underfoot is still there.
	 *
	 * @param lookahead distance ahead in blocks (about half the player's width is a good value)
	 */
	public static boolean atEdge(World world, double x, double feetY, double z, double moveX, double moveZ, double lookahead) {
		double length = Math.sqrt(moveX * moveX + moveZ * moveZ);
		if (length < 1e-4) {
			return false;
		}
		int y = supportY(feetY);
		double ax = x + moveX / length * lookahead;
		double az = z + moveZ / length * lookahead;
		return world.replaceable((int) Math.floor(ax), y, (int) Math.floor(az));
	}

	/** Distance from (x, z) to the far edge of the current support block in the movement direction. */
	public static double distanceToEdge(double x, double z, double moveX, double moveZ) {
		if (Math.abs(moveX) >= Math.abs(moveZ)) {
			double frac = x - Math.floor(x);
			return moveX > 0 ? 1 - frac : frac;
		}
		double frac = z - Math.floor(z);
		return moveZ > 0 ? 1 - frac : frac;
	}
}
