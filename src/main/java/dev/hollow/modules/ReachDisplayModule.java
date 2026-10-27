package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.NumberSetting;

/**
 * Shows how far away the thing under your crosshair is, compared with your game's own reach,
 * and can draw your reach as a ring on the ground. Display only: reach itself is unchanged.
 */
public final class ReachDisplayModule extends Module {
	public final NumberSetting measureRange = add("Display", new NumberSetting("measure_range", "Measure Up To",
		"Distances are measured along your view up to this far.", 12, 4, 64, 1).unit(" blocks"));
	public final BoolSetting showHud = add("Display", new BoolSetting("show_hud", "Distance Readout",
		"HUD element with the target distance, your reach and your last hit distance.", true));
	public final BoolSetting entityRing = add("Range Rings", new BoolSetting("entity_ring", "Attack Reach Ring",
		"A ring on the ground at your entity attack reach.", true));
	public final ColorSetting entityRingColor = add("Range Rings", new ColorSetting("entity_ring_color", "Attack Ring Color", "", 0xC0E0405E, true));
	public final BoolSetting blockRing = add("Range Rings", new BoolSetting("block_ring", "Block Reach Ring", "", false));
	public final ColorSetting blockRingColor = add("Range Rings", new ColorSetting("block_ring_color", "Block Ring Color", "", 0xA09D4EDD, true));

	public ReachDisplayModule() {
		super("reach_display", "Reach Display", "Distance to what you aim at versus your real reach, plus reach rings. Visual only.",
			Category.COMBAT, false);
		entityRingColor.visibleWhen(entityRing::get);
		blockRingColor.visibleWhen(blockRing::get);
	}
}
