package dev.hollow.modules;

import dev.hollow.Hollow;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.sky.SkyEnums;

/**
 * Performance presets and adaptive quality. Presets only change performance-related settings of
 * Holo's visual effects and FPS Boost; they never switch other modules on or off, change keybinds
 * or touch Minecraft's own video settings.
 */
public final class PerformanceModule extends Module {
	public enum Preset implements EnumSetting.Displayable {
		POTATO("Potato Mode"),
		LOW("Low"),
		BALANCED("Balanced"),
		HIGH_QUALITY("High Quality"),
		CUSTOM("Custom");

		private final String label;

		Preset(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public final EnumSetting<Preset> preset = add("Preset", new EnumSetting<>("preset", "Preset",
		"Potato: minimal effects and strongest FPS Boost. Low: FPS first. Balanced: a middle ground. High Quality: full effects with "
			+ "safe optimizations. Custom: adjust each setting yourself; Apply changes nothing.", Preset.HIGH_QUALITY));
	public final ActionSetting apply = add("Preset", new ActionSetting("apply", "Apply Preset",
		"Sets: Sky Studio detail; menu and loading particles; motion blur; swing trails; FPS Boost entity distances, particle cap "
			+ "and background frame limit (and turns FPS Boost on for Potato, Low and Balanced).", "Apply", this::applyPreset));

	public final BoolSetting adaptive = add("Adaptive Quality", new BoolSetting("adaptive", "Adaptive Quality",
		"When FPS stays below the target, step decorative effects down one level at a time (with a cooldown), and back up when "
			+ "there is headroom. Settings are not changed; the reduction is temporary.", false));
	public final NumberSetting targetFps = add("Adaptive Quality", new NumberSetting("target_fps", "Target FPS",
		"Below this for 5 seconds lowers effects; above 1.3× this for 20 seconds raises them.", 45, 20, 240, 5));
	public final BoolSetting notify = add("Adaptive Quality", new BoolSetting("notify", "Announce Changes",
		"Post a HOLO ALERT whenever adaptive quality changes level.", true));

	private String lastApplied = "";

	public PerformanceModule() {
		super("performance", "Performance", "Presets from Potato to High Quality, plus optional adaptive quality.", Category.PERFORMANCE, true);
		targetFps.visibleWhen(adaptive::get);
		notify.visibleWhen(adaptive::get);
	}

	@Override
	public boolean isToggleable() {
		return false;
	}

	public String lastApplied() {
		return lastApplied;
	}

	public void applyPreset() {
		Preset p = preset.get();
		if (p == Preset.CUSTOM) {
			lastApplied = "Custom: nothing changed";
			return;
		}
		SkyStudioModule sky = Hollow.module(SkyStudioModule.class);
		MainMenuModule menu = Hollow.module(MainMenuModule.class);
		LoadingScreenModule loading = Hollow.module(LoadingScreenModule.class);
		MotionBlurModule blur = Hollow.module(MotionBlurModule.class);
		HollowHandsModule hands = Hollow.module(HollowHandsModule.class);
		FpsBoostModule boost = Hollow.module(FpsBoostModule.class);
		switch (p) {
			case POTATO -> {
				sky.detail.set(SkyEnums.Detail.LOW);
				menu.particles.set(false);
				menu.decorations.set(false);
				loading.particles.set(false);
				loading.motionTrail.set(false);
				blur.camera.set(false);
				blur.gui.set(false);
				hands.trails.set(false);
				boost.configure(32, 16, 32, 1000, 15);
				boost.setEnabled(true);
			}
			case LOW -> {
				sky.detail.set(SkyEnums.Detail.LOW);
				menu.particles.set(true);
				menu.particleCount.set(20.0);
				menu.decorations.set(false);
				loading.particles.set(true);
				loading.motionTrail.set(false);
				blur.camera.set(false);
				blur.gui.set(false);
				hands.trails.set(false);
				boost.configure(48, 24, 48, 2000, 20);
				boost.setEnabled(true);
			}
			case BALANCED -> {
				sky.detail.set(SkyEnums.Detail.MEDIUM);
				menu.particles.set(true);
				menu.particleCount.set(30.0);
				menu.decorations.set(true);
				loading.particles.set(true);
				loading.motionTrail.set(false);
				blur.gui.set(true);
				hands.trails.set(true);
				boost.configure(64, 32, 64, 4000, 30);
				boost.setEnabled(true);
			}
			case HIGH_QUALITY -> {
				sky.detail.set(SkyEnums.Detail.HIGH);
				menu.particles.set(true);
				menu.particleCount.set(60.0);
				menu.decorations.set(true);
				loading.particles.set(true);
				loading.motionTrail.set(true);
				blur.gui.set(true);
				hands.trails.set(true);
				boost.configure(128, 64, 128, 8000, 60);
			}
			default -> {
			}
		}
		lastApplied = "Applied " + p.displayName();
	}
}
