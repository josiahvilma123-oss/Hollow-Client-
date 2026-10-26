package dev.hollow.modules;

import dev.hollow.blur.MotionBlurRenderer;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;

public final class MotionBlurModule extends Module {
	public final BoolSetting camera = add("Camera", new BoolSetting("camera", "Camera Turns",
		"Blur briefly during fast camera turns. The HUD, text and crosshair are never blurred.", true));
	public final NumberSetting intensity = add("Camera", new NumberSetting("intensity", "Intensity",
		"How strong the blur is during a turn.", 0.4, 0.05, 1.0, 0.05));
	public final NumberSetting duration = add("Camera", new NumberSetting("duration", "Duration",
		"How long the trail lingers after a fast turn.", 80, 20, 250, 5).unit("ms"));
	public final NumberSetting sensitivity = add("Camera", new NumberSetting("sensitivity", "Sensitivity",
		"Higher values blur at slower turn speeds.", 0.5, 0, 1, 0.05));

	public final BoolSetting gui = add("Interface", new BoolSetting("gui", "GUI Transitions",
		"Soft motion trails on Hollow's panels, pages and alert toasts while they move.", true));
	public final NumberSetting guiIntensity = add("Interface", new NumberSetting("gui_intensity", "GUI Intensity",
		"Opacity of the ghost trail behind moving panels.", 0.5, 0.05, 1, 0.05));
	public final NumberSetting guiDuration = add("Interface", new NumberSetting("gui_duration", "GUI Duration",
		"Length of panel and page transitions.", 180, 60, 400, 10).unit("ms"));

	public final BoolSetting pauseWithShaders = add("Compatibility", new BoolSetting("pause_with_shaders", "Pause With Shader Packs",
		"Turns camera blur off while an Iris shader pack is active, since the pack owns the frame.", true));

	public MotionBlurModule() {
		super("motion_blur", "Motion Blur", "Restrained, atmospheric blur during fast turns and GUI transitions.",
			Category.VISUALS_DETECTION, true);
	}

	public boolean cameraActive() {
		return isEnabled() && camera.get();
	}

	/** GUI trail strength in [0, 1]; 0 when GUI blur is off. */
	public double guiTrail() {
		return isEnabled() && gui.get() ? guiIntensity.get() : 0;
	}

	@Override
	protected void onDisable() {
		MotionBlurRenderer.INSTANCE.release();
	}
}
