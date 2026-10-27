package dev.hollow.modules;

import dev.hollow.bridge.BridgeAssist;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.policy.Automation;
import java.util.Optional;

/**
 * Places blocks under you as you walk over a gap, using the ordinary right-click placement
 * against an adjacent block. Stops when you run out of blocks. Single-player and permitted
 * servers only.
 */
public final class AutoBridgeModule extends Module implements Tickable {
	public final NumberSetting delay = add("Placement", new NumberSetting("delay", "Placement Delay",
		"Minimum ticks between placements (20 ticks = 1 second).", 1, 0, 10, 1).unit(" ticks"));
	public final BoolSetting placeAhead = add("Placement", new BoolSetting("place_ahead", "Place Ahead",
		"Also fill the next block in your walking direction just before you reach it.", true));
	public final BoolSetting autoSelect = add("Placement", new BoolSetting("auto_select", "Pick Blocks From Hotbar",
		"Switch to a hotbar slot with full, non-falling blocks when your hand holds something else.", true));
	public final BoolSetting faceBlock = add("Placement", new BoolSetting("face_block", "Look at Placement",
		"Turn your view to the face being clicked, as a manual placement would.", false));
	public final BoolSetting sneakAssist = add("Placement", new BoolSetting("sneak_assist", "Sneak at Edges",
		"Hold sneak while the next step is over a gap, so you can't walk off before the block is placed.", true));
	public final BoolSetting preview = add("Display", new BoolSetting("preview", "Placement Preview",
		"Outline where the next block will go.", true));
	public final BoolSetting showHud = add("Display", new BoolSetting("show_hud", "Block Counter",
		"HUD element with the selected block, blocks left and state.", true));

	public AutoBridgeModule() {
		super("auto_bridge", "Auto Bridge", "Places blocks under you across gaps with normal placements. "
			+ "Single-player and permitted servers only.", Category.BRIDGING, false);
	}

	@Override
	public Optional<String> blockedReason() {
		return Automation.blockedReason();
	}

	@Override
	public void tick() {
		if (isEnabled()) {
			BridgeAssist.INSTANCE.tickAuto(this);
		}
	}
}
