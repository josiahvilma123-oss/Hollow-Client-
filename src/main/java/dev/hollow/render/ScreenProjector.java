package dev.hollow.render;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Projects world positions to GUI coordinates using the exact view and projection matrices
 * of the last rendered frame, so HUD labels line up with what is drawn in the world.
 */
public final class ScreenProjector {
	public static final ScreenProjector INSTANCE = new ScreenProjector();

	private final Matrix4f viewProjection = new Matrix4f();
	private final Vector4f scratch = new Vector4f();
	private double cameraX;
	private double cameraY;
	private double cameraZ;
	private float partialTick;
	private boolean valid;

	private ScreenProjector() {
	}

	public void capture(Matrix4fc view, Matrix4fc projection, double cameraX, double cameraY, double cameraZ, float partialTick) {
		viewProjection.set(projection).mul(view);
		this.cameraX = cameraX;
		this.cameraY = cameraY;
		this.cameraZ = cameraZ;
		this.partialTick = partialTick;
		valid = true;
	}

	/** Inverse of this frame's projection * view, for reconstructing view rays in the sky shader. */
	public Matrix4f inverseViewProjection(Matrix4f dest) {
		return viewProjection.invert(dest);
	}

	public void invalidate() {
		valid = false;
	}

	public boolean valid() {
		return valid;
	}

	public float partialTick() {
		return partialTick;
	}

	public double cameraX() {
		return cameraX;
	}

	public double cameraY() {
		return cameraY;
	}

	public double cameraZ() {
		return cameraZ;
	}

	/**
	 * @param out receives GUI-space x and y
	 * @return false when the point is behind the camera
	 */
	public boolean project(double x, double y, double z, int guiWidth, int guiHeight, float[] out) {
		if (!valid) {
			return false;
		}
		scratch.set((float) (x - cameraX), (float) (y - cameraY), (float) (z - cameraZ), 1f);
		viewProjection.transform(scratch);
		if (scratch.w <= 0.05f) {
			return false;
		}
		float ndcX = scratch.x / scratch.w;
		float ndcY = scratch.y / scratch.w;
		out[0] = (ndcX * 0.5f + 0.5f) * guiWidth;
		out[1] = (1f - (ndcY * 0.5f + 0.5f)) * guiHeight;
		return true;
	}
}
