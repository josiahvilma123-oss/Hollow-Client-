package dev.hollow.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;

/** Immediate geometry helpers for camera-relative world overlays. */
public final class WorldDraw {
	private WorldDraw() {
	}

	public static void line(PoseStack.Pose pose, VertexConsumer lines, float x1, float y1, float z1, float x2, float y2, float z2,
			int argb, float width) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float dz = z2 - z1;
		float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (length < 1e-6f) {
			return;
		}
		dx /= length;
		dy /= length;
		dz /= length;
		lines.addVertex(pose, x1, y1, z1).setColor(argb).setNormal(pose, dx, dy, dz).setLineWidth(width);
		lines.addVertex(pose, x2, y2, z2).setColor(argb).setNormal(pose, dx, dy, dz).setLineWidth(width);
	}

	/** Outline of a camera-relative box. */
	public static void boxOutline(PoseStack.Pose pose, VertexConsumer lines, AABB box, int argb, float width) {
		float x1 = (float) box.minX;
		float y1 = (float) box.minY;
		float z1 = (float) box.minZ;
		float x2 = (float) box.maxX;
		float y2 = (float) box.maxY;
		float z2 = (float) box.maxZ;
		line(pose, lines, x1, y1, z1, x2, y1, z1, argb, width);
		line(pose, lines, x1, y1, z2, x2, y1, z2, argb, width);
		line(pose, lines, x1, y2, z1, x2, y2, z1, argb, width);
		line(pose, lines, x1, y2, z2, x2, y2, z2, argb, width);
		line(pose, lines, x1, y1, z1, x1, y2, z1, argb, width);
		line(pose, lines, x2, y1, z1, x2, y2, z1, argb, width);
		line(pose, lines, x1, y1, z2, x1, y2, z2, argb, width);
		line(pose, lines, x2, y1, z2, x2, y2, z2, argb, width);
		line(pose, lines, x1, y1, z1, x1, y1, z2, argb, width);
		line(pose, lines, x2, y1, z1, x2, y1, z2, argb, width);
		line(pose, lines, x1, y2, z1, x1, y2, z2, argb, width);
		line(pose, lines, x2, y2, z1, x2, y2, z2, argb, width);
	}

	/** Translucent faces of a camera-relative box. */
	public static void boxFill(PoseStack.Pose pose, VertexConsumer quads, AABB box, int argb) {
		float x1 = (float) box.minX;
		float y1 = (float) box.minY;
		float z1 = (float) box.minZ;
		float x2 = (float) box.maxX;
		float y2 = (float) box.maxY;
		float z2 = (float) box.maxZ;
		quad(pose, quads, argb, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
		quad(pose, quads, argb, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
		quad(pose, quads, argb, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
		quad(pose, quads, argb, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
		quad(pose, quads, argb, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
		quad(pose, quads, argb, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer quads, int argb, float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		quads.addVertex(pose, ax, ay, az).setColor(argb);
		quads.addVertex(pose, bx, by, bz).setColor(argb);
		quads.addVertex(pose, cx, cy, cz).setColor(argb);
		quads.addVertex(pose, dx, dy, dz).setColor(argb);
	}
}
