package dev.hollow.modules;

import dev.hollow.combat.Attacks;
import dev.hollow.combat.TargetSettings;
import dev.hollow.combat.Targeting;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.input.KeyInput;
import dev.hollow.policy.Automation;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Attacks when your crosshair is on an eligible target and your attack is charged. You still aim;
 * it only presses attack. Single-player and permitted servers only; never while typing, in menus or
 * when the window is unfocused.
 */
public final class TriggerBotModule extends Module implements Tickable {
	public final NumberSetting delay = add("Attacking", new NumberSetting("delay", "Reaction Delay",
		"How long the crosshair must rest on a target before attacking.", 80, 0, 500, 10).unit(" ms"));
	public final NumberSetting charge = add("Attacking", new NumberSetting("charge", "Attack Charge",
		"Only attack once the attack indicator has charged this far (1.0 = full damage).", 1.0, 0.5, 1.0, 0.05));
	public final BoolSetting weaponOnly = add("Attacking", new BoolSetting("weapon_only", "Only With Sword or Axe", "", false));
	public final BoolSetting pauseWhileUsing = add("Attacking", new BoolSetting("pause_using", "Pause While Using Items", "", true));
	public final TargetSettings targets = new TargetSettings(false, TargetSettings.Priority.CROSSHAIR);

	private Entity aimedAt;
	private long aimedSince;
	private int attacks;

	public TriggerBotModule() {
		super("trigger_bot", "Trigger Bot", "Attacks when your crosshair rests on an eligible target. "
			+ "Single-player and permitted servers only.", Category.COMBAT, false);
		for (Setting<?> setting : targets.all()) {
			if (setting != targets.priority) {
				add("Targets", setting);
			}
		}
	}

	public int attacks() {
		return attacks;
	}

	@Override
	public Optional<String> blockedReason() {
		return Automation.blockedReason();
	}

	@Override
	public void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!isEnabled() || player == null || minecraft.gameMode == null || !Automation.allowed() || !KeyInput.gameplayInputActive()
				|| !player.isAlive() || player.isSpectator() || pauseWhileUsing.get() && player.isUsingItem()
				|| weaponOnly.get() && !Attacks.holdingWeapon(player)) {
			aimedAt = null;
			return;
		}
		Entity entity = minecraft.hitResult instanceof EntityHitResult hit ? hit.getEntity() : null;
		if (entity == null || !Targeting.eligible(player, entity, targets, player.entityInteractionRange(), false)) {
			aimedAt = null;
			return;
		}
		long now = System.currentTimeMillis();
		if (entity != aimedAt) {
			aimedAt = entity;
			aimedSince = now;
		}
		if (now - aimedSince >= delay.get() && Attacks.charged(player, charge.get()) && Attacks.inReach(player, entity)) {
			Attacks.attack(minecraft, player, entity);
			attacks++;
		}
	}
}
