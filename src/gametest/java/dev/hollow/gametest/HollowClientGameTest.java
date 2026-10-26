package dev.hollow.gametest;

import dev.hollow.Hollow;
import dev.hollow.core.module.Module;
import dev.hollow.detection.EntityCategory;
import dev.hollow.gui.HollowScreen;
import dev.hollow.hands.HandPreset;
import dev.hollow.character.Cosmetic;
import dev.hollow.menu.HoloTitleScreen;
import dev.hollow.menu.TitleScreenSwap;
import dev.hollow.modules.BlockEspModule;
import dev.hollow.modules.CharacterStudioModule;
import dev.hollow.modules.MainMenuModule;
import dev.hollow.modules.ThemePacksModule;
import dev.hollow.modules.BlockFinderModule;
import dev.hollow.modules.EntityTracersModule;
import dev.hollow.modules.HollowAlertsModule;
import dev.hollow.modules.HollowHandsModule;
import dev.hollow.modules.InterfaceModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.modules.ServerRulesModule;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.sky.SkyPresetStore;
import dev.hollow.render.WorldOverlayRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.TitleScreen;
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
		// Keep CPU-only (software OpenGL) CI machines fast enough to load the world in time: the
		// test harness ticks the server in lockstep with client frames, and vanilla's menu blur
		// dominates frame time under llvmpipe.
		context.getInput().resizeWindow(960, 540);
		context.runOnClient(client -> {
			client.options.renderDistance().set(2);
			client.options.simulationDistance().set(5);
			client.options.framerateLimit().set(30);
			client.options.menuBackgroundBlurriness().set(0);
		});
		menuAndLoading(context);

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

			// The click GUI itself, then every module's settings window.
			context.setScreen(() -> new HollowScreen(null));
			context.waitTicks(12);
			int index = 3;
			context.takeScreenshot(String.format("hollow-%02d-gui-windows", index++));
			String[] pages = {"motion_blur", "hollow_hands", "block_finder", "block_esp", "entity_tracers", "hollow_alerts",
				"interface", "server_rules", "sky_studio", "character_studio", "theme_packs", "main_menu", "loading_screen", "performance"};
			for (String id : pages) {
				Class<? extends Module> type = Hollow.modules().byId(id).orElseThrow().getClass();
				context.setScreen(() -> new HollowScreen(null).openOn(type));
				context.waitTicks(12);
				context.takeScreenshot(String.format("hollow-%02d-gui-%s", index++, id));
			}

			// Sky Studio's library and management rows (search narrows the page to them).
			context.runOnClient(client -> Hollow.module(SkyStudioModule.class).setEnabled(true));
			context.setScreen(() -> new HollowScreen(null).openOn(SkyStudioModule.class));
			context.waitTicks(8);
			context.getInput().typeChars("library");
			context.waitTicks(8);
			context.takeScreenshot(String.format("hollow-%02d-gui-sky-library", index++));
			context.runOnClient(client -> Hollow.module(SkyStudioModule.class).setEnabled(false));

			// Search filters the module list.
			context.setScreen(() -> new HollowScreen(null));
			context.waitTicks(8);
			context.getInput().typeChars("trac");
			context.waitTicks(8);
			context.takeScreenshot(String.format("hollow-%02d-gui-search-modules", index++));

			// With a settings window open, search filters its settings.
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
				// Exaggerated so the effect is visible at this test's ~10 FPS software rendering; at
				// real frame rates the defaults (0.4, 80ms) give a short, restrained trail.
				blur.intensity.set(0.9);
				blur.duration.set(240.0);
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

			// Holo Sky Studio: every built-in preset, looking toward the sky.
			context.runOnClient(client -> {
				client.player.setXRot(-18f);
				client.player.setYRot(180f);
			});
			context.runOnClient(client -> Hollow.module(SkyStudioModule.class).setEnabled(true));
			for (SkyPresetStore.Preset preset : Hollow.skyPresets().builtIns()) {
				context.runOnClient(client -> Hollow.module(SkyStudioModule.class).applyPreset(preset.name()));
				context.waitTicks(4);
				context.takeScreenshot(String.format("hollow-%02d-sky-%s", index++, SkyPresetStore.slug(preset.name())));
			}
			measureSkyCost(context);
			context.runOnClient(client -> Hollow.module(SkyStudioModule.class).setEnabled(false));

			// Character Studio cosmetics in third person, then a whole theme pack.
			context.runOnClient(client -> {
				client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
				client.player.setXRot(10f);
				Hollow.module(CharacterStudioModule.class).setEnabled(true);
			});
			for (Cosmetic cosmetic : new Cosmetic[] {Cosmetic.PHARAOH, Cosmetic.WRAITH, Cosmetic.HOLLOW_KNIGHT}) {
				context.runOnClient(client -> Hollow.module(CharacterStudioModule.class).cosmetic.set(cosmetic));
				context.waitTicks(4);
				context.takeScreenshot(String.format("hollow-%02d-cosmetic-%s", index++, cosmetic.asset()));
			}
			context.runOnClient(client -> {
				ThemePacksModule packs = Hollow.module(ThemePacksModule.class);
				packs.pack.set(ThemePacksModule.Pack.PHARAOH);
				packs.applyAll.run();
				if (!Hollow.module(SkyStudioModule.class).preset.get().equals("Pharaoh's Night")
						|| Hollow.module(CharacterStudioModule.class).cosmetic.get() != Cosmetic.PHARAOH
						|| Hollow.module(MainMenuModule.class).scene.get() != MainMenuModule.Scene.TEMPLE) {
					throw new AssertionError("Theme pack did not apply every component: " + packs.lastApplied());
				}
				// Applying only the character must leave the sky alone.
				packs.pack.set(ThemePacksModule.Pack.WRAITH);
				packs.applySky.set(false);
				packs.applyInterface.set(false);
				packs.applyWeapon.set(false);
				packs.applyMenu.set(false);
				packs.applySelected.run();
				if (!Hollow.module(SkyStudioModule.class).preset.get().equals("Pharaoh's Night")
						|| Hollow.module(CharacterStudioModule.class).cosmetic.get() != Cosmetic.WRAITH) {
					throw new AssertionError("Applying one component changed others: " + packs.lastApplied());
				}
				packs.pack.set(ThemePacksModule.Pack.PHARAOH);
				client.player.setXRot(-12f);
			});
			context.waitTicks(6);
			context.takeScreenshot(String.format("hollow-%02d-theme-pack", index++));
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));

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

	/** The Holo title screen in each scene, the vanilla fallback, studio pages from the menu, and a resource reload. */
	private static void menuAndLoading(ClientGameTestContext context) {
		context.setScreen(TitleScreen::new);
		context.waitTicks(30);
		context.runOnClient(client -> {
			if (!(client.screen instanceof HoloTitleScreen)) {
				throw new AssertionError("Title screen was not replaced: " + client.screen);
			}
			Hollow.module(MainMenuModule.class).character.set(MainMenuModule.Featured.WRAITH);
		});
		context.waitTicks(10);
		context.takeScreenshot("holo-01-menu-temple");
		MainMenuModule.Scene[] scenes = {MainMenuModule.Scene.PYRAMIDS, MainMenuModule.Scene.VOID, MainMenuModule.Scene.SKY,
			MainMenuModule.Scene.CUSTOM};
		int index = 2;
		for (MainMenuModule.Scene scene : scenes) {
			context.runOnClient(client -> {
				MainMenuModule menu = Hollow.module(MainMenuModule.class);
				menu.scene.set(scene);
				menu.character.set(scene == MainMenuModule.Scene.PYRAMIDS ? MainMenuModule.Featured.PHARAOH : MainMenuModule.Featured.HOLLOW_KNIGHT);
				if (scene == MainMenuModule.Scene.SKY) {
					Hollow.module(SkyStudioModule.class).applyPreset("Anubis Eclipse");
				}
			});
			context.waitTicks(12);
			context.takeScreenshot(String.format("holo-%02d-menu-%s", index++, scene.name().toLowerCase(java.util.Locale.ROOT)));
		}
		context.runOnClient(client -> {
			MainMenuModule menu = Hollow.module(MainMenuModule.class);
			menu.scene.set(MainMenuModule.Scene.TEMPLE);
			menu.character.set(MainMenuModule.Featured.WRAITH);
			Hollow.module(SkyStudioModule.class).applyPreset("Vanilla Enhanced");
		});
		context.waitTicks(8);

		// Studio pages opened from the menu keep the scene behind them.
		context.setScreen(() -> new HollowScreen(new HoloTitleScreen()).openOn(SkyStudioModule.class));
		context.waitTicks(12);
		context.takeScreenshot(String.format("holo-%02d-menu-sky-studio", index++));
		context.setScreen(() -> new HollowScreen(new HoloTitleScreen()).openOn(ThemePacksModule.class));
		context.waitTicks(12);
		context.takeScreenshot(String.format("holo-%02d-menu-theme-packs", index++));

		// Other menus share the backdrop.
		context.setScreen(() -> new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(new HoloTitleScreen()));
		context.waitTicks(12);
		context.takeScreenshot(String.format("holo-%02d-menu-world-list", index++));

		// Vanilla fallback: the button shows vanilla once; turning the module off keeps it.
		context.runOnClient(client -> TitleScreenSwap.showVanilla());
		context.waitTicks(10);
		context.runOnClient(client -> {
			if (client.screen == null || client.screen.getClass() != TitleScreen.class) {
				throw new AssertionError("Vanilla Menu did not show vanilla's title screen");
			}
			Hollow.module(MainMenuModule.class).setEnabled(false);
			client.setScreen(new TitleScreen());
			if (client.screen.getClass() != TitleScreen.class) {
				throw new AssertionError("Disabled main menu still replaced the title screen");
			}
		});
		context.waitTicks(10);
		context.takeScreenshot(String.format("holo-%02d-menu-vanilla-restored", index++));
		context.runOnClient(client -> {
			Hollow.module(MainMenuModule.class).setEnabled(true);
			client.setScreen(new TitleScreen());
		});
		context.waitTicks(10);

		measureMenuCost(context);

		// Resource reload shows the Holo loading screen; it must hand back to the menu by itself.
		long[] reloadStart = new long[1];
		context.runOnClient(client -> {
			reloadStart[0] = System.nanoTime();
			client.reloadResourcePacks();
		});
		context.waitTicks(3);
		context.takeScreenshot(String.format("holo-%02d-loading-reload", index++));
		context.waitFor(client -> client.getOverlay() == null, 20 * 120);
		context.runOnClient(client -> {
			Hollow.LOGGER.info("[gametest] Resource reload with the Holo loading screen took {} ms",
				(System.nanoTime() - reloadStart[0]) / 1_000_000);
			if (!(client.screen instanceof HoloTitleScreen)) {
				throw new AssertionError("Expected the Holo menu after reloading, got " + client.screen);
			}
		});
		context.waitTicks(10);
	}

	/**
	 * Time to render one frame with Sky Studio off and in several configurations. The harness
	 * steps frames in lockstep with ticks, so FPS is meaningless here; instead frames are rendered
	 * back to back with glFinish so the (software) GPU work counts. Configurations are interleaved
	 * over three rounds and the best round is kept, which filters out machine noise.
	 */
	private static void measureSkyCost(ClientGameTestContext context) {
		String[] labels = {"off", "Anubis Eclipse (high detail)", "Anubis Eclipse (low detail)", "Vanilla Enhanced (high detail)"};
		double[] best = new double[labels.length];
		java.util.Arrays.fill(best, Double.MAX_VALUE);
		for (int round = 0; round < 3; round++) {
			for (int pass = 0; pass < labels.length; pass++) {
				int slot = pass;
				context.runOnClient(client -> {
					SkyStudioModule sky = Hollow.module(SkyStudioModule.class);
					sky.setEnabled(slot > 0);
					sky.applyPreset(slot == 3 ? "Vanilla Enhanced" : "Anubis Eclipse");
					sky.detail.set(slot == 2 ? dev.hollow.sky.SkyEnums.Detail.LOW : dev.hollow.sky.SkyEnums.Detail.HIGH);
				});
				context.waitTicks(3);
				context.runOnClient(client -> best[slot] = Math.min(best[slot], frameMillis(client, 30)));
			}
		}
		context.runOnClient(client -> Hollow.module(SkyStudioModule.class).detail.set(dev.hollow.sky.SkyEnums.Detail.HIGH));
		for (int i = 0; i < labels.length; i++) {
			Hollow.LOGGER.info("[gametest] Frame time, Sky Studio {}: {} ms", labels[i], String.format(java.util.Locale.ROOT, "%.2f", best[i]));
		}
	}

	/** Same measurement for the title screen: vanilla's, Holo's, and Holo's without particles and decorations. */
	private static void measureMenuCost(ClientGameTestContext context) {
		String[] labels = {"vanilla title screen", "Holo title screen", "Holo title screen without particles/decorations"};
		double[] best = new double[labels.length];
		java.util.Arrays.fill(best, Double.MAX_VALUE);
		for (int round = 0; round < 3; round++) {
			for (int pass = 0; pass < labels.length; pass++) {
				int slot = pass;
				if (slot == 0) {
					context.runOnClient(client -> TitleScreenSwap.showVanilla());
				} else {
					context.runOnClient(client -> {
						MainMenuModule menu = Hollow.module(MainMenuModule.class);
						menu.particles.set(slot == 1);
						menu.decorations.set(slot == 1);
						client.setScreen(new TitleScreen());
					});
				}
				context.waitTicks(25);
				context.runOnClient(client -> best[slot] = Math.min(best[slot], frameMillis(client, 30)));
			}
		}
		context.runOnClient(client -> {
			MainMenuModule menu = Hollow.module(MainMenuModule.class);
			menu.particles.set(true);
			menu.decorations.set(true);
			client.setScreen(new TitleScreen());
		});
		for (int i = 0; i < labels.length; i++) {
			Hollow.LOGGER.info("[gametest] Frame time, {}: {} ms", labels[i], String.format(java.util.Locale.ROOT, "%.2f", best[i]));
		}
	}

	private static double frameMillis(net.minecraft.client.Minecraft client, int frames) {
		client.gameRenderer.render(client.getDeltaTracker(), true);
		org.lwjgl.opengl.GL11.glFinish();
		long start = System.nanoTime();
		for (int i = 0; i < frames; i++) {
			client.gameRenderer.render(client.getDeltaTracker(), true);
			org.lwjgl.opengl.GL11.glFinish();
		}
		return (System.nanoTime() - start) / 1e6 / frames;
	}

	private static void buildScene(TestSingleplayerContext world) {
		String[] commands = {
			"gamerule send_command_feedback false",
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
