package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;

/**
 * Real rendering savings in busy scenes: skip drawing far-away entities, cap the number of live
 * particles, and limit the frame rate while the game window is in the background.
 */
public final class FpsBoostModule extends Module {
	public final BoolSetting entityCulling = add("Entity Distance", new BoolSetting("entity_culling", "Limit Entity Render Distance",
		"Entities beyond these distances are not drawn (they still exist and behave normally). Players, your vehicle, glowing and "
			+ "large entities are always drawn.", true));
	public final NumberSetting mobDistance = add("Entity Distance", new NumberSetting("mob_distance", "Mobs & Animals", "", 64, 16, 256, 8).unit(" blocks"));
	public final NumberSetting itemDistance = add("Entity Distance", new NumberSetting("item_distance", "Dropped Items & XP", "", 32, 8, 128, 4).unit(" blocks"));
	public final NumberSetting otherDistance = add("Entity Distance", new NumberSetting("other_distance", "Other Entities",
		"Projectiles, boats, minecarts, item frames and similar.", 64, 16, 256, 8).unit(" blocks"));
	public final BoolSetting keepNamed = add("Entity Distance", new BoolSetting("keep_named", "Always Draw Named Entities",
		"Name-tagged pets and mobs are never hidden.", true));
	public final BoolSetting particleCap = add("Particles", new BoolSetting("particle_cap", "Cap Particles",
		"New particles are skipped while this many are alive (explosions and farms can create tens of thousands).", true));
	public final NumberSetting maxParticles = add("Particles", new NumberSetting("max_particles", "Maximum Particles", "", 4000, 250, 16000, 250));
	public final BoolSetting backgroundLimit = add("Background", new BoolSetting("background_limit", "Limit FPS in Background",
		"Lower frame rate while the game window isn't focused, saving power and heat. Minecraft already limits minimized windows.", true));
	public final NumberSetting backgroundFps = add("Background", new NumberSetting("background_fps", "Background FPS", "", 30, 5, 120, 5));

	public FpsBoostModule() {
		super("fps_boost", "FPS Boost", "Skip far entities, cap particles and save power in the background.", Category.PERFORMANCE, false);
		mobDistance.visibleWhen(entityCulling::get);
		itemDistance.visibleWhen(entityCulling::get);
		otherDistance.visibleWhen(entityCulling::get);
		keepNamed.visibleWhen(entityCulling::get);
		maxParticles.visibleWhen(particleCap::get);
		backgroundFps.visibleWhen(backgroundLimit::get);
	}

	/** Used by performance presets. */
	public void configure(int mobs, int items, int others, int particles, int background) {
		entityCulling.set(true);
		mobDistance.set((double) mobs);
		itemDistance.set((double) items);
		otherDistance.set((double) others);
		particleCap.set(true);
		maxParticles.set((double) particles);
		backgroundLimit.set(true);
		backgroundFps.set((double) background);
	}
}
