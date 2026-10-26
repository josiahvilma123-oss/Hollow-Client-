package dev.hollow.hands;

/**
 * Extra hand transform: a view-space translation followed by a rotation and scale
 * around the held item's anchor point.
 */
public record HandPose(double tx, double ty, double tz, double rotX, double rotY, double rotZ, double scale) {
	public static final HandPose IDENTITY = new HandPose(0, 0, 0, 0, 0, 0, 1);

	/** Where vanilla holds items before swing/use transforms, in view space. */
	public static double anchorX(boolean rightSide) {
		return rightSide ? 0.56 : -0.56;
	}

	public static final double ANCHOR_Y = -0.52;
	public static final double ANCHOR_Z = -0.72;
}
