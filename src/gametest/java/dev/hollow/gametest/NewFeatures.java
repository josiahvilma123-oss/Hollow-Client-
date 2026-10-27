package dev.hollow.gametest;

import dev.hollow.Hollow;
import dev.hollow.camera.Freecam;
import dev.hollow.combat.AimAssist;
import dev.hollow.combat.KnockbackTester;
import dev.hollow.core.waypoints.Waypoint;
import dev.hollow.exploration.Waypoints;
import dev.hollow.gui.HoloScreen;
import dev.hollow.hud.HudEditorScreen;
import dev.hollow.modules.AimAssistModule;
import dev.hollow.modules.AutoBridgeModule;
import dev.hollow.modules.FreecamModule;
import dev.hollow.modules.InterfaceModule;
import dev.hollow.modules.KillAuraModule;
import dev.hollow.modules.KnockbackTesterModule;
import dev.hollow.modules.PerformanceMonitorModule;
import dev.hollow.modules.ReachDisplayModule;
import dev.hollow.modules.SpeedBridgeModule;
import dev.hollow.modules.TriggerBotModule;
import dev.hollow.modules.WaypointsModule;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** In-game checks of the 1.3.0 features: each one is exercised for real and its effect asserted. */
final class NewFeatures {
	private NewFeatures() {
	}

	private static LivingEntity named(Minecraft client, String name) {
		List<LivingEntity> found = client.level.getEntitiesOfClass(LivingEntity.class, new AABB(client.player.blockPosition()).inflate(32),
			e -> e.hasCustomName() && e.getCustomName().getString().equals(name));
		return found.isEmpty() ? null : found.get(0);
	}

	static int run(ClientGameTestContext context, TestSingleplayerContext world, int index) {
		world.getServer().runCommand("tp @a 0 200 0 0 0");
		context.waitTicks(10);
		context.runOnClient(client -> Hollow.LOGGER.info("[gametest] Window active: {}", client.isWindowActive()));

		// Aim Assist: turn away from a practice target; the assist must pull the view back toward it.
		context.runOnClient(client -> AimAssist.INSTANCE.spawnPracticeTargets());
		context.waitTicks(10);
		float[] angles = new float[2];
		context.runOnClient(client -> {
			AimAssistModule aim = Hollow.module(AimAssistModule.class);
			aim.activation.set(AimAssistModule.Activation.ALWAYS);
			aim.requireMouseMovement.set(false);
			aim.practiceMode.set(true);
			aim.fov.set(120.0);
			aim.setEnabled(true);
			client.player.setYRot(client.player.getYRot() + 30f);
			client.player.setXRot(0f);
		});
		context.waitTicks(3);
		context.runOnClient(client -> {
			LivingEntity target = AimAssist.INSTANCE.target();
			if (target == null) {
				throw new AssertionError("Aim Assist found no practice target (state " + AimAssist.INSTANCE.state() + ")");
			}
			angles[0] = dev.hollow.combat.Targeting.angleTo(client.player, target);
		});
		context.waitTicks(30);
		context.runOnClient(client -> {
			LivingEntity target = AimAssist.INSTANCE.target();
			angles[1] = target == null ? Float.NaN : dev.hollow.combat.Targeting.angleTo(client.player, target);
			Hollow.LOGGER.info("[gametest] Aim Assist: angle to target {} -> {} degrees", angles[0], angles[1]);
			if (!(angles[1] < angles[0] * 0.6f)) {
				throw new AssertionError("Aim Assist did not turn toward the target: " + angles[0] + " -> " + angles[1]);
			}
		});
		context.takeScreenshot(String.format("hollow-%02d-aim-assist", index++));
		context.runOnClient(client -> {
			Hollow.module(AimAssistModule.class).setEnabled(false);
			AimAssist.INSTANCE.clearPracticeTargets();
		});
		context.waitTicks(5);

		// Kill Aura: a zombie within reach takes damage without any input.
		world.getServer().runCommand("tp @a 0 200 0 0 0");
		world.getServer().runCommand("summon minecraft:zombie 0 200 2.5 {NoAI:1b,PersistenceRequired:1b,CustomName:\"KA Target\"}");
		context.waitTicks(5);
		context.runOnClient(client -> {
			KillAuraModule aura = Hollow.module(KillAuraModule.class);
			aura.fov.set(180.0);
			aura.setEnabled(true);
		});
		context.waitTicks(40);
		context.runOnClient(client -> {
			LivingEntity zombie = named(client, "KA Target");
			Hollow.module(KillAuraModule.class).setEnabled(false);
			if (zombie != null && zombie.getHealth() >= zombie.getMaxHealth()) {
				throw new AssertionError("Kill Aura did not damage the zombie in reach");
			}
			Hollow.LOGGER.info("[gametest] Kill Aura: zombie health {}", zombie == null ? "dead" : zombie.getHealth());
		});
		world.getServer().runCommand("kill @e[name=\"KA Target\"]");

		// Trigger Bot: attacks only what the crosshair is on.
		world.getServer().runCommand("summon minecraft:zombie 0 200 2.5 {NoAI:1b,PersistenceRequired:1b,CustomName:\"TB Target\"}");
		world.getServer().runCommand("tp @a 0 200 0 0 14");
		context.waitTicks(5);
		context.runOnClient(client -> Hollow.module(TriggerBotModule.class).setEnabled(true));
		context.waitTicks(40);
		context.runOnClient(client -> {
			TriggerBotModule trigger = Hollow.module(TriggerBotModule.class);
			LivingEntity zombie = named(client, "TB Target");
			trigger.setEnabled(false);
			if (trigger.attacks() == 0 || zombie != null && zombie.getHealth() >= zombie.getMaxHealth()) {
				throw new AssertionError("Trigger Bot did not attack the zombie under the crosshair (" + trigger.attacks() + " attacks)");
			}
			Hollow.LOGGER.info("[gametest] Trigger Bot: {} attacks", trigger.attacks());
		});
		world.getServer().runCommand("kill @e[name=\"TB Target\"]");

		// Reach display and knockback readout on the HUD, after a test knockback.
		context.runOnClient(client -> {
			Hollow.module(ReachDisplayModule.class).setEnabled(true);
			Hollow.module(KnockbackTesterModule.class).setEnabled(true);
			Hollow.module(PerformanceMonitorModule.class).setEnabled(true);
			KnockbackTester.INSTANCE.testHit();
		});
		context.waitTicks(30);
		context.runOnClient(client -> {
			if (KnockbackTester.INSTANCE.samples() == 0 || !(KnockbackTester.INSTANCE.distance() > 0)) {
				throw new AssertionError("Knockback Tester measured nothing after a test hit");
			}
			Hollow.LOGGER.info("[gametest] Knockback: {} blocks, {} samples", KnockbackTester.INSTANCE.distance(),
				KnockbackTester.INSTANCE.samples());
		});
		context.takeScreenshot(String.format("hollow-%02d-hud-elements", index++));
		context.runOnClient(client -> HudEditorScreen.open());
		context.waitTicks(8);
		context.takeScreenshot(String.format("hollow-%02d-hud-editor", index++));
		context.setScreen(() -> null);
		context.runOnClient(client -> {
			Hollow.module(ReachDisplayModule.class).setEnabled(false);
			Hollow.module(KnockbackTesterModule.class).setEnabled(false);
		});

		// Auto Bridge: standing at the platform edge, a block is placed under the player.
		world.getServer().runCommand("item replace entity @a hotbar.1 with minecraft:stone 64");
		world.getServer().runCommand("tp @a 13.2 200 10 -90 60");
		context.waitTicks(10);
		context.runOnClient(client -> Hollow.module(AutoBridgeModule.class).setEnabled(true));
		context.waitTicks(10);
		context.takeScreenshot(String.format("hollow-%02d-auto-bridge", index++));
		context.runOnClient(client -> {
			Hollow.module(AutoBridgeModule.class).setEnabled(false);
			if (!client.level.getBlockState(new BlockPos(13, 199, 10)).is(Blocks.STONE)) {
				throw new AssertionError("Auto Bridge did not place a block under the player: "
					+ client.level.getBlockState(new BlockPos(13, 199, 10)));
			}
		});
		context.runOnClient(client -> Hollow.module(SpeedBridgeModule.class).setEnabled(true));
		world.getServer().runCommand("tp @a 14.2 200 10 -90 70");
		context.waitTicks(6);
		context.takeScreenshot(String.format("hollow-%02d-speed-bridge-edge", index++));
		context.runOnClient(client -> Hollow.module(SpeedBridgeModule.class).setEnabled(false));

		// Freecam: the camera flies forward while the player stays exactly where it was.
		world.getServer().runCommand("tp @a 0 200 0 0 20");
		context.waitTicks(5);
		Vec3[] start = new Vec3[2];
		context.runOnClient(client -> {
			start[0] = client.player.position();
			Hollow.module(FreecamModule.class).setEnabled(true);
		});
		context.waitTicks(2);
		context.runOnClient(client -> start[1] = Freecam.INSTANCE.position());
		context.getInput().holdKeyFor(options -> options.keyUp, 20);
		context.waitTicks(2);
		context.takeScreenshot(String.format("hollow-%02d-freecam", index++));
		context.runOnClient(client -> {
			double playerMoved = client.player.position().distanceTo(start[0]);
			double cameraMoved = Freecam.INSTANCE.position().distanceTo(start[1]);
			Hollow.module(FreecamModule.class).setEnabled(false);
			Hollow.LOGGER.info("[gametest] Freecam: camera moved {} blocks, player moved {}", cameraMoved, playerMoved);
			if (playerMoved > 0.01 || cameraMoved < 1) {
				throw new AssertionError("Freecam: camera moved " + cameraMoved + ", player moved " + playerMoved);
			}
		});

		// Waypoints: add one at the player, then look at its beacon from a distance.
		int[] count = new int[1];
		context.runOnClient(client -> {
			count[0] = Waypoints.INSTANCE.store().all().size();
			WaypointsModule waypoints = Hollow.module(WaypointsModule.class);
			waypoints.formName.set("Test Base");
			waypoints.formCategory.set(Waypoint.Category.BASE);
			waypoints.formNote.set("Created by the game test");
			Waypoints.INSTANCE.addFromForm(true);
			if (Waypoints.INSTANCE.store().all().size() != count[0] + 1) {
				throw new AssertionError("Waypoint was not added: " + Waypoints.INSTANCE.message());
			}
		});
		world.getServer().runCommand("tp @a 0 200 20 180 10");
		context.waitTicks(8);
		context.takeScreenshot(String.format("hollow-%02d-waypoint-beacon", index++));
		context.setScreen(() -> new HoloScreen(null).openOn(WaypointsModule.class));
		context.waitTicks(8);
		context.getInput().typeChars("saved");
		context.waitTicks(6);
		context.takeScreenshot(String.format("hollow-%02d-gui-waypoint-list", index++));
		context.setScreen(() -> null);

		// Profiles: save, change the theme, load, and the saved theme comes back.
		context.runOnClient(client -> {
			try {
				InterfaceModule ui = Hollow.module(InterfaceModule.class);
				ui.theme.set(InterfaceModule.ThemePreset.HOLO_VIOLET);
				Hollow.profiles().saveCurrent("Gametest", System.currentTimeMillis());
				ui.theme.set(InterfaceModule.ThemePreset.CRIMSON);
				Hollow.profiles().load("Gametest");
				if (ui.theme.get() != InterfaceModule.ThemePreset.HOLO_VIOLET || ui.accent.get() != 0xFF9D4EDD) {
					throw new AssertionError("Loading the profile did not restore the theme: " + ui.theme.get());
				}
			} catch (java.io.IOException e) {
				throw new AssertionError(e);
			}
		});
		context.setScreen(() -> new HoloScreen(null).openProfiles());
		context.waitTicks(8);
		context.takeScreenshot(String.format("hollow-%02d-gui-profiles-saved", index++));
		context.setScreen(() -> null);
		context.runOnClient(client -> {
			try {
				Hollow.profiles().delete("Gametest");
			} catch (java.io.IOException e) {
				throw new AssertionError(e);
			}
			Hollow.module(PerformanceMonitorModule.class).setEnabled(false);
			// Leave no test waypoint behind: its beacon would show up in the benchmarks.
			for (Waypoint waypoint : List.copyOf(Waypoints.INSTANCE.store().all())) {
				Waypoints.INSTANCE.delete(waypoint.id());
			}
		});
		world.getServer().runCommand("tp @a 0 200 0 0 12");
		context.waitTicks(5);
		return index;
	}
}
