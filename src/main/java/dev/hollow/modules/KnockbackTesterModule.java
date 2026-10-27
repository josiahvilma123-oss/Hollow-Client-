package dev.hollow.modules;

import dev.hollow.combat.KnockbackTester;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.ActionBarSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;

/**
 * Measures the knockback you receive (speed and distance) and, in your own single-player world,
 * lets you test it: change your knockback resistance through the game's attribute system and
 * apply test hits. It never alters or drops network packets.
 */
public final class KnockbackTesterModule extends Module implements Tickable {
	public final BoolSetting showHud = add("Measurement", new BoolSetting("show_hud", "Knockback Readout",
		"HUD element with the last knockback's speed and distance.", true));
	public final NumberSetting resistance = add("Single-player Testing", new NumberSetting("resistance", "Knockback Resistance",
		"Your knockback resistance attribute while this module is on (single-player worlds only). 0% is vanilla.", 0, 0, 100, 5).unit("%"));
	public final NumberSetting testStrength = add("Single-player Testing", new NumberSetting("test_strength", "Test Hit Strength",
		"0.4 is an ordinary hit; 0.9 is a sprinting hit; higher values match Knockback enchantments.", 0.4, 0.1, 3.0, 0.1));
	public final ActionBarSetting actions = add("Single-player Testing", new ActionBarSetting("actions", "Test",
		"Applies a knockback from the direction you are facing, as if hit from the front.",
		new ActionBarSetting.Button("Knock me back", KnockbackTester.INSTANCE::testHit),
		new ActionBarSetting.Button("Clear readings", KnockbackTester.INSTANCE::clear)));

	public KnockbackTesterModule() {
		super("knockback_tester", "Knockback Tester", "Measure the knockback you take, and test resistance in single-player. "
			+ "No packet changes.", Category.COMBAT, false);
		resistance.onChange(value -> KnockbackTester.INSTANCE.applyResistance(isEnabled() ? value : 0));
	}

	@Override
	protected void onEnable() {
		KnockbackTester.INSTANCE.applyResistance(resistance.get());
	}

	@Override
	protected void onDisable() {
		KnockbackTester.INSTANCE.applyResistance(0);
	}

	@Override
	public void tick() {
		if (isEnabled()) {
			KnockbackTester.INSTANCE.tick();
		}
	}
}
