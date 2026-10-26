package dev.hollow.modules;

import dev.hollow.Hollow;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.sky.SkyEnums;

/** One place to trade decorative effects for frame rate. */
public final class PerformanceModule extends Module {
	public enum Profile implements EnumSetting.Displayable {
		QUALITY("Quality"),
		BALANCED("Balanced"),
		LOW("Low Performance");

		private final String label;

		Profile(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public final EnumSetting<Profile> profile = add("Profile", new EnumSetting<>("profile", "Profile",
		"Low Performance: low sky detail, no nebula noise, fewer menu particles, no motion blur or trails.", Profile.QUALITY));
	public final ActionSetting apply = add("Profile", new ActionSetting("apply", "Apply Profile",
		"Adjusts Sky Studio detail, menu and loading effects, motion blur and swing trails.", "Apply", this::applyProfile));

	public PerformanceModule() {
		super("performance", "Performance", "Profiles that scale back decorative effects on slower systems.", Category.CLIENT, true);
	}

	@Override
	public boolean isToggleable() {
		return false;
	}

	public void applyProfile() {
		Profile p = profile.get();
		SkyStudioModule sky = Hollow.module(SkyStudioModule.class);
		MainMenuModule menu = Hollow.module(MainMenuModule.class);
		LoadingScreenModule loading = Hollow.module(LoadingScreenModule.class);
		MotionBlurModule blur = Hollow.module(MotionBlurModule.class);
		HollowHandsModule hands = Hollow.module(HollowHandsModule.class);
		switch (p) {
			case QUALITY -> {
				sky.detail.set(SkyEnums.Detail.HIGH);
				menu.particles.set(true);
				menu.particleCount.set(60.0);
				menu.decorations.set(true);
				loading.particles.set(true);
				loading.motionTrail.set(true);
			}
			case BALANCED -> {
				sky.detail.set(SkyEnums.Detail.MEDIUM);
				menu.particleCount.set(30.0);
				loading.motionTrail.set(false);
			}
			case LOW -> {
				sky.detail.set(SkyEnums.Detail.LOW);
				menu.particles.set(false);
				menu.decorations.set(false);
				loading.particles.set(false);
				loading.motionTrail.set(false);
				blur.camera.set(false);
				blur.gui.set(false);
				hands.trails.set(false);
			}
		}
	}
}
