package dev.hollow.render;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Contributes world-space shapes and labels to the shared overlay pass (drawn once per frame). */
public interface OverlayProvider {
	void collect(Sink sink, float partialTick);

	/** Receives shapes in world coordinates. */
	interface Sink {
		void outline(AABB box, int color, float width, boolean seeThrough);

		void fill(AABB box, int color, boolean seeThrough);

		void line(Vec3 from, Vec3 to, int color, float width);

		void label(OverlayLabel label);

		/** Position the camera is drawing from (for distances). */
		Vec3 camera();
	}
}
