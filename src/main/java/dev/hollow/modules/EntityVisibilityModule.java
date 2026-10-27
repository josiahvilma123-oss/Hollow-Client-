package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.exploration.VisibilityLog;

/**
 * Diagnostics of what your client can actually observe about other players: tab-list joins and
 * leaves, game-mode changes, player entities vanishing nearby while still listed, and the
 * invisibility flag turning on or off. It reports changes only; it never reveals hidden players'
 * positions or bypasses a server's vanish system.
 */
public final class EntityVisibilityModule extends Module implements Tickable {
	public final BoolSetting tabList = add("Report", new BoolSetting("tab_list", "Tab List Joins & Leaves", "", true));
	public final BoolSetting gameModes = add("Report", new BoolSetting("game_modes", "Game Mode Changes",
		"Shown in the tab list, e.g. a switch to spectator.", true));
	public final BoolSetting disappearances = add("Report", new BoolSetting("disappearances", "Nearby Disappearances",
		"A player entity close to you was removed while the player is still in the tab list (vanish, teleport, death or a "
			+ "dimension change). Only where player detection is permitted.", true));
	public final NumberSetting nearby = add("Report", new NumberSetting("nearby", "Nearby Distance", "", 48, 8, 128, 4).unit(" blocks"));
	public final BoolSetting invisibility = add("Report", new BoolSetting("invisibility", "Invisibility Changes",
		"The invisible flag of a loaded player turned on or off. Positions of invisible players are never shown.", true));
	public final BoolSetting alerts = add("Output", new BoolSetting("alerts", "Show as Alerts", "Also post each event as a HOLO ALERT.", true));
	public final ActionSetting clear = add("Output", new ActionSetting("clear", "Event Log", "The most recent events are listed below.",
		"Clear", VisibilityLog.INSTANCE::clear));

	public EntityVisibilityModule() {
		super("entity_visibility", "Entity Visibility", "Reports player visibility changes your client can observe. "
			+ "Never reveals hidden players.", Category.EXPLORATION, false);
		nearby.visibleWhen(disappearances::get);
	}

	@Override
	public void tick() {
		if (isEnabled()) {
			VisibilityLog.INSTANCE.tick(this);
		}
	}

	@Override
	protected void onDisable() {
		VisibilityLog.INSTANCE.forget();
	}
}
