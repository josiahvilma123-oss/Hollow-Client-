package dev.hollow.render;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertFeed;
import dev.hollow.core.anim.Easing;
import dev.hollow.core.anim.Smooth;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.HollowAlertsModule;
import dev.hollow.modules.MotionBlurModule;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

/** Screen-space parts of Hollow: world labels and the alert toast stack. */
public final class HudOverlay {
	public static final HudOverlay INSTANCE = new HudOverlay();
	public static final int TOAST_WIDTH = 196;
	private static final int TOAST_HEIGHT = 30;
	private static final int TOAST_GAP = 5;
	private static final long ENTER_MILLIS = 260;
	private static final long EXIT_MILLIS = 320;

	private final float[] projected = new float[2];
	private final Map<AlertFeed.Toast, Smooth> toastY = new IdentityHashMap<>();
	private long lastFrame = -1;

	private HudOverlay() {
	}

	public void renderLabels(GuiGraphics g, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		List<OverlayLabel> labels = WorldOverlayRenderer.INSTANCE.labels();
		if (labels.isEmpty() || minecraft.options.hideGui) {
			return;
		}
		Font font = minecraft.font;
		int width = g.guiWidth();
		int height = g.guiHeight();
		// Farthest first so nearer labels end up on top.
		for (int i = labels.size() - 1; i >= 0; i--) {
			OverlayLabel label = labels.get(i);
			if (!ScreenProjector.INSTANCE.project(label.x(), label.y(), label.z(), width, height, projected)) {
				continue;
			}
			drawLabel(g, font, label, projected[0], projected[1]);
		}
	}

	private static void drawLabel(GuiGraphics g, Font font, OverlayLabel label, float x, float y) {
		String title = label.title();
		String detail = label.detail();
		int titleWidth = title.isEmpty() ? 0 : font.width(title);
		int detailWidth = detail.isEmpty() ? 0 : font.width(detail);
		int gap = titleWidth > 0 && detailWidth > 0 ? 5 : 0;
		int contentWidth = titleWidth + gap + detailWidth;
		if (contentWidth == 0) {
			return;
		}
		int w = contentWidth + 8;
		int h = 11;
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(label.scale(), label.scale());
		int left = -w / 2;
		int top = -h;
		Draw2D.panel(g, left, top, w, h, 0xB80B0910);
		g.fill(left, top + 1, left + 1, top + h - 1, Colors.withAlpha(label.color(), 230));
		int textX = left + 4;
		if (titleWidth > 0) {
			Draw2D.text(g, font, title, textX, top + 2, Colors.withAlpha(Colors.lerp(label.color(), 0xFFFFFFFF, 0.55f), 255));
		}
		if (detailWidth > 0) {
			Draw2D.text(g, font, detail, textX + titleWidth + gap, top + 2, Theme.TEXT_DIM);
		}
		pose.popMatrix();
	}

	public void renderAlerts(GuiGraphics g, DeltaTracker delta) {
		if (Hollow.alerts() == null) {
			return;
		}
		HollowAlertsModule settings = Hollow.module(HollowAlertsModule.class);
		List<AlertFeed.Toast> toasts = Hollow.alerts().visible();
		long now = Hollow.alerts().now();
		double frameSeconds = lastFrame < 0 ? 0 : Math.min(0.1, (now - lastFrame) / 1000.0);
		lastFrame = now;
		toastY.keySet().retainAll(toasts);
		if (toasts.isEmpty()) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		float scale = settings.scale.getFloat();
		HollowAlertsModule.Placement placement = settings.placement.get();
		int screenW = Math.round(g.guiWidth() / scale);
		int screenH = Math.round(g.guiHeight() / scale);
		double trail = Hollow.module(MotionBlurModule.class).guiTrail();
		long lifetime = Hollow.alerts().lifetimeMillis();

		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		int slot = 0;
		for (AlertFeed.Toast toast : toasts) {
			long age = toast.age(now);
			double enter = Easing.CUBIC_OUT.ease(age / (double) ENTER_MILLIS);
			double exit = Easing.CUBIC_IN_OUT.ease((age - (lifetime - EXIT_MILLIS)) / (double) EXIT_MILLIS);
			double visibility = Math.min(enter, 1 - exit);
			if (visibility <= 0.01) {
				slot++;
				continue;
			}
			double targetY = slot * (TOAST_HEIGHT + TOAST_GAP);
			Smooth smooth = toastY.computeIfAbsent(toast, t -> new Smooth(targetY));
			smooth.target(targetY);
			double y = smooth.update(frameSeconds, 0.08);
			double slide = (1 - visibility) * 26;
			int baseX = switch (placement) {
				case TOP_LEFT -> 8;
				case TOP_CENTER -> (screenW - TOAST_WIDTH) / 2;
				default -> screenW - TOAST_WIDTH - 8;
			};
			// Direction the toast slides in from: sideways for corner placements, from above when centered.
			double dirX = switch (placement) {
				case TOP_LEFT -> -1;
				case TOP_CENTER -> 0;
				default -> 1;
			};
			double dirY = placement == HollowAlertsModule.Placement.TOP_CENTER ? -0.5 : 0;
			double restY = placement == HollowAlertsModule.Placement.BOTTOM_RIGHT ? screenH - 56 - TOAST_HEIGHT - y : 8 + y;
			double x = baseX + dirX * slide;
			double drawY = restY + dirY * slide;
			// GUI motion blur: faint ghosts trailing behind the toast while it slides.
			if (trail > 0 && visibility < 0.98) {
				for (int ghost = 2; ghost >= 1; ghost--) {
					double offset = slide * 0.35 * ghost;
					drawToast(g, font, toast, x + dirX * offset, drawY + dirY * offset, (float) (visibility * trail * 0.18 / ghost), false);
				}
			}
			drawToast(g, font, toast, x, drawY, (float) visibility, true);
			slot++;
		}
		pose.popMatrix();
	}

	/** Draws a static example toast, used by the settings preview. */
	public static void drawSample(GuiGraphics g, Font font, Alert alert, int x, int y, float alpha) {
		drawToast(g, font, alert, 1, x, y, alpha, true);
	}

	private static void drawToast(GuiGraphics g, Font font, AlertFeed.Toast toast, double x, double y, float alpha, boolean full) {
		drawToast(g, font, toast.alert(), toast.count(), x, y, alpha, full);
	}

	private static void drawToast(GuiGraphics g, Font font, Alert alert, int count, double x, double y, float alpha, boolean full) {
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate((float) x, (float) y);
		int accent = Theme.accent();
		int w = TOAST_WIDTH;
		int h = TOAST_HEIGHT;
		if (full) {
			Draw2D.glow(g, 0, 0, w, h, Colors.fade(accent, alpha), 4, 0.9f);
		}
		Draw2D.panel(g, 0, 0, w, h, Colors.fade(0xF20C0A10, alpha));
		Draw2D.border(g, 0, 0, w, h, Colors.fade(Colors.withAlpha(accent, 120), alpha));
		g.fill(1, 1, 3, h - 1, Colors.fade(accent, alpha));
		if (full) {
			Draw2D.scythe(g, 7, 4, 11, Colors.fade(Theme.accentLight(), alpha));
			Draw2D.spaced(g, font, Alert.PREFIX, 22, 5, Colors.fade(Theme.accentLight(), alpha), 1);
			String source = alert.source().displayName().toUpperCase(java.util.Locale.ROOT);
			String detail = alert.detail().isEmpty() ? "" : alert.detail();
			String chip = count > 1 ? "×" + count : "";
			int rightX = w - 6;
			if (!chip.isEmpty()) {
				int chipW = font.width(chip) + 6;
				Draw2D.panel(g, rightX - chipW, 3, chipW, 10, Colors.fade(Colors.withAlpha(accent, 90), alpha));
				Draw2D.text(g, font, chip, rightX - chipW + 3, 4, Colors.fade(Theme.TEXT, alpha));
				rightX -= chipW + 4;
			}
			String tag = Draw2D.ellipsize(font, detail.isEmpty() ? source : detail, Math.max(10, rightX - 100));
			Draw2D.text(g, font, tag, rightX - font.width(tag), 5, Colors.fade(Theme.TEXT_FAINT, alpha));
			String body = Draw2D.ellipsize(font, alert.body(), w - 28);
			Draw2D.text(g, font, body, 22, 17, Colors.fade(Theme.TEXT, alpha));
		}
		pose.popMatrix();
	}
}
