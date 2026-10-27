package dev.hollow.hud;

import dev.hollow.Hollow;
import dev.hollow.core.hud.HudSnap;
import dev.hollow.modules.HudLayoutModule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

/** Registry and renderer of Holo's movable HUD elements. */
public final class HudElements {
	private static final List<HudElement> ELEMENTS = new ArrayList<>();
	private static final java.util.Set<HudElement> FAILED = new java.util.HashSet<>();

	private HudElements() {
	}

	public static <E extends HudElement> E register(E element) {
		ELEMENTS.add(element);
		return element;
	}

	public static List<HudElement> all() {
		return Collections.unmodifiableList(ELEMENTS);
	}

	public static float scale() {
		return Hollow.module(HudLayoutModule.class).scale.getFloat();
	}

	/** Top-left corner of an element in GUI pixels for the given screen size. */
	public static float[] screenPosition(HudElement element, int screenW, int screenH) {
		float scale = element.scale();
		float[] saved = Hollow.module(HudLayoutModule.class).position(element.id());
		float fx = saved != null ? saved[0] : element.defaultX();
		float fy = saved != null ? saved[1] : element.defaultY();
		float w = element.width() * scale;
		float h = element.height() * scale;
		return new float[] {HudSnap.fromFraction(fx, w, screenW), HudSnap.fromFraction(fy, h, screenH)};
	}

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui || minecraft.screen instanceof HudEditorScreen) {
			return;
		}
		for (HudElement element : ELEMENTS) {
			if (FAILED.contains(element) || element.drawnElsewhere() || !element.visible()) {
				continue;
			}
			float[] pos = screenPosition(element, g.guiWidth(), g.guiHeight());
			Matrix3x2fStack pose = g.pose();
			pose.pushMatrix();
			pose.translate(pos[0], pos[1]);
			pose.scale(element.scale(), element.scale());
			try {
				element.render(g, false);
			} catch (RuntimeException e) {
				// One broken element must not take the HUD (or the game) down with it.
				Hollow.LOGGER.error("HUD element {} failed and is hidden for this session", element.name(), e);
				FAILED.add(element);
			} finally {
				pose.popMatrix();
			}
		}
	}
}
