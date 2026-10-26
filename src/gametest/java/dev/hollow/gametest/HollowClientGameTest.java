package dev.hollow.gametest;

import dev.hollow.Hollow;
import dev.hollow.core.module.Module;
import dev.hollow.detection.EntityCategory;
import dev.hollow.gui.HollowScreen;
import dev.hollow.hands.HandPreset;
import dev.hollow.modules.BlockEspModule;
import dev.hollow.modules.BlockFinderModule;
import dev.hollow.modules.EntityTracersModule;
import dev.hollow.modules.HollowAlertsModule;
import dev.hollow.modules.HollowHandsModule;
import dev.hollow.modules.InterfaceModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.modules.ServerRulesModule;
import dev.hollow.render.WorldOverlayRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.InteractionHand;

/**
 * Launches the real client, builds a small night-time scene and captures screenshots of every
 * Hollow feature. Assertions cover the behaviour that can be checked from code; the screenshots
 * are for visual review.
 */
public final class HollowClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			buildScene(world);
			world.getClientWorld().waitForChunksRender();

			context.runOnClient(client -> {
				BlockFinderModule finder = Hollow.module(BlockFinderModule.class);
				finder.category("chests").enabled.set(true);
				finder.category("diamond_ore").tracer.set(true);
				finder.category("ancient_debris").tracer.set(true);
				finder.setEnabled(true);
				EntityTracersModule tracers = Hollow.module(EntityTracersModule.class);
				tracers.category(EntityCategory.PASSIVE).show.set(true);
				tracers.category(EntityCategory.NEUTRAL).show.set(true);
				tracers.boxes.set(true);
				tracers.setEnabled(true);
			});
			context.waitTicks(60);

			context.runOnClient(client -> {
				int matches = Hollow.scanner().matches().size();
				if (matches < 8) {
					throw new AssertionError("Block Finder found only " + matches + " matches in the test scene");
				}
				int tracked = Hollow.entities().tracked().size();
				if (tracked < 3) {
					throw new AssertionError("Entity Tracers tracked only " + tracked + " entities");
				}
				Hollow.LOGGER.info("[gametest] Block Finder matches: {}, entities tracked: {}, labels: {}", matches, tracked,
					WorldOverlayRenderer.INSTANCE.labels().size());
			});
			context.takeScreenshot("hollow-01-world-overlays");

			// Alerts preview stacked with real detection alerts.
			context.runOnClient(client -> Hollow.module(HollowAlertsModule.class).preview.run());
			context.waitTicks(10);
			context.takeScreenshot("hollow-02-alerts");

			// Player detection must stay off unless permitted (single-player permits it).
			context.runOnClient(client -> {
				if (!Hollow.module(ServerRulesModule.class).playersAllowed()) {
					throw new AssertionError("Players should be permitted in single-player");
				}
			});

			// Every page of the module browser.
			String[] pages = {"motion_blur", "hollow_hands", "block_finder", "block_esp", "entity_tracers", "hollow_alerts",
				"interface", "server_rules"};
			int index = 3;
			for (String id : pages) {
				Class<? extends Module> type = Hollow.modules().byId(id).orElseThrow().getClass();
				context.setScreen(() -> new HollowScreen(null).openOn(type));
				context.waitTicks(12);
				context.takeScreenshot(String.format("hollow-%02d-gui-%s", index++, id));
			}

			// Search filters settings across the page.
			context.setScreen(() -> new HollowScreen(null).openOn(EntityTracersModule.class));
			context.waitTicks(8);
			context.getInput().typeChars("color");
			context.waitTicks(8);
			context.takeScreenshot(String.format("hollow-%02d-gui-search", index++));
			context.setScreen(() -> null);
			context.waitTicks(5);

			// Motion blur during a fast camera turn.
			context.runOnClient(client -> {
				MotionBlurModule blur = Hollow.module(MotionBlurModule.class);
				blur.intensity.set(0.8);
				blur.duration.set(160.0);
				Hollow.module(BlockFinderModule.class).setEnabled(false);
				Hollow.module(EntityTracersModule.class).setEnabled(false);
			});
			for (int i = 0; i < 6; i++) {
				context.runOnClient(client -> client.player.setYRot(client.player.getYRot() + 14f));
				context.waitTick();
			}
			context.takeScreenshot(String.format("hollow-%02d-motion-blur-turn", index++));
			context.waitTicks(20);

			// Hollow Hands: stylized swing with trails.
			world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:netherite_sword");
			context.runOnClient(client -> Hollow.module(HollowHandsModule.class).preset.set(HandPreset.STYLIZED));
			context.waitTicks(10);
			context.runOnClient(client -> client.player.swing(InteractionHand.MAIN_HAND));
			context.waitTicks(2);
			context.takeScreenshot(String.format("hollow-%02d-hands-swing", index++));
			context.waitTicks(10);
			context.runOnClient(client -> {
				HollowHandsModule hands = Hollow.module(HollowHandsModule.class);
				hands.offsetX.set(-0.12);
				hands.scale.set(0.8);
				hands.rotY.set(-12.0);
			});
			context.waitTicks(5);
			context.takeScreenshot(String.format("hollow-%02d-hands-positioned", index++));

			// Disabling every module restores vanilla rendering.
			context.runOnClient(client -> {
				for (Module module : Hollow.modules().all()) {
					if (module.isToggleable()) {
						module.setEnabled(false);
					}
				}
			});
			context.waitTicks(10);
			context.runOnClient(client -> {
				if (!WorldOverlayRenderer.INSTANCE.labels().isEmpty() || !Hollow.scanner().matches().isEmpty()) {
					throw new AssertionError("Overlays still present after disabling modules");
				}
			});
			context.takeScreenshot(String.format("hollow-%02d-all-disabled", index++));

			context.runOnClient(client -> {
				for (Module module : Hollow.modules().all()) {
					if (!module.isAvailable()) {
						throw new AssertionError(module.name() + " became unavailable: " + module.unavailableReason().orElse(""));
					}
				}
				Hollow.module(InterfaceModule.class);
			});
		}
	}

	private static void buildScene(TestSingleplayerContext world) {
		String[] commands = {
			"gamerule doDaylightCycle false",
			"gamerule doWeatherCycle false",
			"gamerule doMobSpawning false",
			"time set 18000",
			"weather clear",
			"fill -12 199 -4 12 199 24 minecraft:polished_blackstone",
			"fill -12 200 -4 12 210 24 minecraft:air",
			"setblock -3 200 6 minecraft:diamond_ore",
			"setblock -3 201 6 minecraft:deepslate_diamond_ore",
			"setblock -2 200 6 minecraft:diamond_ore",
			"setblock -3 200 7 minecraft:diamond_ore",
			"setblock 3 200 8 minecraft:budding_amethyst",
			"setblock 3 201 8 minecraft:amethyst_cluster",
			"setblock 4 200 8 minecraft:budding_amethyst",
			"setblock 4 201 8 minecraft:large_amethyst_bud",
			"setblock 0 200 14 minecraft:ancient_debris",
			"fill -2 200 12 2 203 12 minecraft:stone",
			"setblock 6 200 4 minecraft:chest",
			"setblock -7 200 10 minecraft:chest",
			"summon minecraft:zombie -5 200 12 {NoAI:1b,PersistenceRequired:1b}",
			"summon minecraft:cow 6 200 13 {NoAI:1b}",
			"summon minecraft:villager 0 200 18 {NoAI:1b}",
			"effect give @a minecraft:night_vision infinite 0 true",
			"tp @a 0 200 0 0 12"
		};
		for (String command : commands) {
			world.getServer().runCommand(command);
		}
	}
}
