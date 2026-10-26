package dev.hollow.render;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertFeed;
import dev.hollow.core.anim.Easing;
import dev.hollow.core.anim.Smooth;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.HollowAlertsModule;
import dev.hollow.modules.MotionBlurModule;
import java.util.ArrayList;
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
	private static final int MIN_TOAST_WIDTH = 196;
	private static final int MAX_TOAST_WIDTH = 300;
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
		// Place nearest labels first at their anchor; farther ones are nudged upward until they
		// no longer overlap anything already placed, keeping dense scenes readable.
		List<float[]> placed = new ArrayList<>();
		List<PlacedLabel> draws = new ArrayList<>();
		for (OverlayLabel label : labels) {
			if (!ScreenProjector.INSTANCE.project(label.x(), label.y(), label.z(), width, height, projected)) {
				continue;
			}
			float w = (labelWidth(font, label) + 14) * label.scale();
			float h = 11 * label.scale();
			float x = projected[0] - w / 2;
			float y = projected[1] - h;
			for (int attempt = 0; attempt < 8; attempt++) {
				float[] hit = overlap(placed, x, y, w, h);
				if (hit == null) {
					break;
				}
				y = hit[1] - h - 1;
			}
			placed.add(new float[] {x, y, w, h});
			draws.add(new PlacedLabel(label, projected[0], y + h));
		}
		for (int i = draws.size() - 1; i >= 0; i--) {
			PlacedLabel draw = draws.get(i);
			drawLabel(g, font, draw.label(), draw.centerX(), draw.bottom());
		}
	}

	private record PlacedLabel(OverlayLabel label, float centerX, float bottom) {
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
		int w = contentWidth + 10;
		int h = 11;
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(label.scale(), label.scale());
		int left = -w / 2;
		int top = -h;
		Draw2D.rounded(g, left - 2, top, w + 4, h, h / 2f, 0xC00B0910);
		Draw2D.circle(g, left + 2.5f, top + h / 2f, 1.75f, Colors.withAlpha(label.color(), 240));
		int textX = left + 6;
		if (titleWidth > 0) {
			Draw2D.text(g, font, title, textX, top + 2, Colors.withAlpha(Colors.lerp(label.color(), 0xFFFFFFFF, 0.55f), 255));
		}
		if (detailWidth > 0) {
			Draw2D.text(g, font, detail, textX + titleWidth + gap, top + 2, Theme.TEXT_DIM);
		}
		pose.popMatrix();
	}

	private static float[] overlap(List<float[]> placed, float x, float y, float w, float h) {
		for (float[] r : placed) {
			if (x < r[0] + r[2] && x + w > r[0] && y < r[1] + r[3] && y + h > r[1]) {
				return r;
			}
		}
		return null;
	}

	private static int labelWidth(Font font, OverlayLabel label) {
		int titleWidth = label.title().isEmpty() ? 0 : font.width(label.title());
		int detailWidth = label.detail().isEmpty() ? 0 : font.width(label.detail());
		return titleWidth + (titleWidth > 0 && detailWidth > 0 ? 5 : 0) + detailWidth;
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
			int toastWidth = toastWidth(font, toast.alert(), toast.count());
			int baseX = switch (placement) {
				case TOP_LEFT -> 8;
				case TOP_CENTER -> (screenW - toastWidth) / 2;
				default -> screenW - toastWidth - 8;
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

	/** Width that fits the toast's text, within sensible bounds. */
	public static int toastWidth(Font font, Alert alert, int count) {
		String tag = alert.detail().isEmpty() ? alert.source().displayName().toUpperCase(java.util.Locale.ROOT) : alert.detail();
		int header = 22 + Draw2D.spacedWidth(font, Alert.PREFIX, 1) + 12 + font.width(tag) + (count > 1 ? font.width("×" + count) + 10 : 0) + 6;
		int body = 22 + font.width(alert.body()) + 8;
		return Math.max(MIN_TOAST_WIDTH, Math.min(MAX_TOAST_WIDTH, Math.max(header, body)));
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
		int w = toastWidth(font, alert, count);
		int h = TOAST_HEIGHT;
		float radius = 7f;
		if (full) {
			Draw2D.glow(g, 0, 0, w, h, radius, Colors.fade(accent, alpha), 5, 0.9f);
		}
		Draw2D.rounded(g, 0, 0, w, h, radius, Colors.fade(0xF20C0A10, alpha));
		Draw2D.roundedBorder(g, 0, 0, w, h, radius, 1, Colors.fade(Colors.withAlpha(accent, 120), alpha));
		Draw2D.rounded(g, 4, 5, 2.5f, h - 10, 1.25f, Colors.fade(accent, alpha));
		if (full) {
			Draw2D.scythe(g, 7, 4, 11, Colors.fade(Theme.accentLight(), alpha));
			Draw2D.spaced(g, font, Alert.PREFIX, 22, 5, Colors.fade(Theme.accentLight(), alpha), 1);
			String source = alert.source().displayName().toUpperCase(java.util.Locale.ROOT);
			String detail = alert.detail().isEmpty() ? "" : alert.detail();
			String chip = count > 1 ? "×" + count : "";
			int rightX = w - 6;
			if (!chip.isEmpty()) {
				int chipW = font.width(chip) + 6;
				Draw2D.rounded(g, rightX - chipW, 3, chipW, 10, 5, Colors.fade(Colors.withAlpha(accent, 90), alpha));
				Draw2D.text(g, font, chip, rightX - chipW + 3, 4, Colors.fade(Theme.TEXT, alpha));
				rightX -= chipW + 4;
			}
			int prefixEnd = 22 + Draw2D.spacedWidth(font, Alert.PREFIX, 1) + 10;
			String tag = Draw2D.ellipsize(font, detail.isEmpty() ? source : detail, Math.max(10, rightX - prefixEnd));
			Draw2D.text(g, font, tag, rightX - font.width(tag), 5, Colors.fade(Theme.TEXT_FAINT, alpha));
			String body = Draw2D.ellipsize(font, alert.body(), w - 28);
			Draw2D.text(g, font, body, 22, 17, Colors.fade(Theme.TEXT, alpha));
		}
		pose.popMatrix();
	}
}
