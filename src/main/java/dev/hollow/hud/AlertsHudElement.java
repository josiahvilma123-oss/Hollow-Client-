package dev.hollow.hud;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.modules.HollowAlertsModule;
import dev.hollow.render.HudOverlay;

/** Where alert toasts appear when their placement is Custom. The toasts themselves are drawn by {@link HudOverlay}. */
public final class AlertsHudElement extends HudElement {
	public static final int WIDTH = 196;
	public static final int HEIGHT = 100;
	private static final Alert SAMPLE_A = new Alert(AlertSource.BLOCKS, "Amethyst Cluster", "Detected", 12.4, "sample");
	private static final Alert SAMPLE_B = new Alert(AlertSource.HOSTILE, "Hostile Mob", "Nearby", 18.2, "sample");

	public AlertsHudElement() {
		super("alerts", "Alerts", 1f, 0f);
	}

	@Override
	public float scale() {
		return Hollow.module(HollowAlertsModule.class).scale.getFloat();
	}

	@Override
	public boolean drawnElsewhere() {
		return true;
	}

	@Override
	public void onMoved() {
		Hollow.module(HollowAlertsModule.class).placement.set(HollowAlertsModule.Placement.CUSTOM);
	}

	@Override
	public int width() {
		return WIDTH;
	}

	@Override
	public int height() {
		return HEIGHT;
	}

	@Override
	public boolean visible() {
		return Hollow.module(HollowAlertsModule.class).isEnabled();
	}

	@Override
	public String hiddenReason() {
		return "Alerts are off";
	}

	@Override
	public void render(net.minecraft.client.gui.GuiGraphics g, boolean preview) {
		if (preview) {
			HudOverlay.drawSample(g, HudStyle.font(), SAMPLE_A, 0, 0, 1f);
			HudOverlay.drawSample(g, HudStyle.font(), SAMPLE_B, 0, 35, 0.8f);
		}
	}
}
