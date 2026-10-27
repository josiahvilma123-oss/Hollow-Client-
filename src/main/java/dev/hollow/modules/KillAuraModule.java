package dev.hollow.modules;

import dev.hollow.combat.KillAura;
import dev.hollow.combat.TargetSettings;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.policy.Automation;
import java.util.Optional;

/**
 * Attacks eligible targets for you, for single-player practice and servers that permit it. It
 * follows the game's own rules: vanilla reach, full attack charge, line of sight, and (by default)
 * it visibly turns your view to the target before attacking.
 */
public final class KillAuraModule extends Module implements Tickable {
	public final NumberSetting range = add("Attacking", new NumberSetting("range", "Range",
		"Maximum distance to the target's hitbox. Never more than your game's own attack reach.", 3.0, 1.0, 6.0, 0.1).unit(" blocks"));
	public final NumberSetting charge = add("Attacking", new NumberSetting("charge", "Attack Charge",
		"Only attack once the attack indicator has charged this far (1.0 = full damage).", 1.0, 0.5, 1.0, 0.05));
	public final BoolSetting turnToTarget = add("Attacking", new BoolSetting("turn_to_target", "Turn to Target",
		"Visibly turn your view to the target and only attack once you are looking at it.", true));
	public final NumberSetting turnSpeed = add("Attacking", new NumberSetting("turn_speed", "Turn Speed",
		"How fast your view turns towards the target.", 360, 60, 1080, 10).unit("°/s"));
	public final NumberSetting fov = add("Attacking", new NumberSetting("fov", "Field of View",
		"Targets must be within this cone around your view.", 120, 10, 360, 5).unit("°"));
	public final BoolSetting weaponOnly = add("Attacking", new BoolSetting("weapon_only", "Only With Sword or Axe", "", false));
	public final BoolSetting pauseWhileUsing = add("Attacking", new BoolSetting("pause_using", "Pause While Using Items",
		"No attacks while eating, blocking or drawing a bow.", true));
	public final TargetSettings targets = new TargetSettings(false, TargetSettings.Priority.CROSSHAIR);
	public final BoolSetting marker = add("Display", new BoolSetting("marker", "Target Outline", "", true));
	public final BoolSetting showHud = add("Display", new BoolSetting("show_hud", "Status Indicator", "", true));

	public KillAuraModule() {
		super("kill_aura", "Kill Aura", "Attacks eligible targets with normal attacks, reach and cooldown. "
			+ "Single-player and permitted servers only.", Category.COMBAT, false);
		for (Setting<?> setting : targets.all()) {
			add("Targets", setting);
		}
		turnSpeed.visibleWhen(turnToTarget::get);
	}

	@Override
	public Optional<String> blockedReason() {
		return Automation.blockedReason();
	}

	@Override
	public void tick() {
		KillAura.INSTANCE.tick(this);
	}

	@Override
	protected void onDisable() {
		KillAura.INSTANCE.reset();
	}
}
