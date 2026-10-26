package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;

/**
 * The Holo loading experience. It only restyles Minecraft's own loading overlay: progress is the
 * real resource-reload progress and nothing waits for animations to finish.
 */
public final class LoadingScreenModule extends Module {
	public final NumberSetting speed = add("Animation", new NumberSetting("speed", "Animation Speed", "", 1.0, 0.25, 2.5, 0.05).unit("x"));
	public final BoolSetting reducedMotion = add("Animation", new BoolSetting("reduced_motion", "Reduced Motion",
		"No rotation, drifting or scaling; only gentle fades.", false));
	public final BoolSetting particles = add("Effects", new BoolSetting("particles", "Particles", "", true));
	public final BoolSetting ring = add("Effects", new BoolSetting("ring", "Celestial Ring", "", true));
	public final BoolSetting motionTrail = add("Effects", new BoolSetting("motion_trail", "Motion Trail", "Faint trail on the turning ring.", true));
	public final BoolSetting character = add("Effects", new BoolSetting("character", "Character Artwork",
		"Shows the featured character on reloads (artwork is not available during the very first start-up).", true));
	public final BoolSetting quickHandoff = add("Behaviour", new BoolSetting("quick_handoff", "Skip Fade-Out",
		"Hand over to the menu as soon as loading completes instead of waiting for the two-second fade.", true));

	public LoadingScreenModule() {
		super("loading_screen", "Loading Screen", "Animated Holo start-up and resource loading screen.", Category.CLIENT, true);
	}
}
