package dev.hollow.gametest;

import dev.hollow.Hollow;
import dev.hollow.character.Cosmetic;
import dev.hollow.detection.EntityCategory;
import dev.hollow.modules.CharacterStudioModule;
import dev.hollow.modules.EntityTracersModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.modules.SkyStudioModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Frame-time benchmarks for comparable scenes. The game test steps frames in lockstep with
 * ticks, so FPS is meaningless; frames are rendered back to back with glFinish so the
 * (software) GPU work counts, and the best of three rounds is kept to filter machine noise.
 * Results are logged with a "[bench]" prefix.
 */
final class Benchmarks {
	private record Scenario(String name, boolean crowd, Consumer<Minecraft> setUp, Supplier<Screen> screen) {
	}

	private Benchmarks() {
	}

	private static void spawnCrowd(TestSingleplayerContext world) {
		for (int i = 0; i < 150; i++) {
			world.getServer().runCommand(String.format(Locale.ROOT, "summon minecraft:zombie %d 200 %d {NoAI:1b,PersistenceRequired:1b,Tags:[\"bench\"]}",
				-15 + (i % 15) * 2, 8 + (i / 15) * 2));
		}
		for (int i = 0; i < 200; i++) {
			world.getServer().runCommand(String.format(Locale.ROOT,
				"summon minecraft:item %d 200 %d {Item:{id:\"minecraft:diamond\",count:1},PickupDelay:32767,Age:-32768,Tags:[\"bench\"]}",
				-10 + (i % 20), -6 - (i / 20)));
		}
	}

	/**
	 * @param menu       opens the Holo menu (the GUI class differs between versions)
	 * @param extraSetUp applied before every scenario, e.g. performance features under test
	 * @param label      tag for this run in the log ("baseline", "optimized", ...)
	 */
	static void run(ClientGameTestContext context, TestSingleplayerContext world, Supplier<Screen> menu, Consumer<Minecraft> extraSetUp,
			String label) {
		List<Scenario> scenarios = List.of(
			new Scenario("standing still", false, client -> {
			}, null),
			new Scenario("particles: 6000 flames", false, client -> {
			}, null),
			new Scenario("Holo menu open", false, client -> {
			}, menu),
			new Scenario("crowd: 150 mobs + 200 items, tracers on", true, client -> {
				EntityTracersModule tracers = Hollow.module(EntityTracersModule.class);
				tracers.category(EntityCategory.HOSTILE).show.set(true);
				tracers.setEnabled(true);
			}, null),
			new Scenario("all visuals + crowd: sky, cosmetic, blur, tracers", true, client -> {
				Hollow.module(SkyStudioModule.class).setEnabled(true);
				Hollow.module(SkyStudioModule.class).applyPreset("Anubis Eclipse");
				Hollow.module(CharacterStudioModule.class).setEnabled(true);
				Hollow.module(CharacterStudioModule.class).cosmetic.set(Cosmetic.GRIM_REAPER);
				Hollow.module(MotionBlurModule.class).setEnabled(true);
				Hollow.module(EntityTracersModule.class).setEnabled(true);
				client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}, null));

		world.getServer().runCommand("tp @a 0 200 0 0 0");
		context.waitTicks(10);

		List<String> lines = new ArrayList<>();
		boolean crowdSpawned = false;
		for (Scenario scenario : scenarios) {
			if (scenario.crowd() && !crowdSpawned) {
				spawnCrowd(world);
				crowdSpawned = true;
				context.waitTicks(20);
			}
			context.runOnClient(client -> {
				extraSetUp.accept(client);
				scenario.setUp().accept(client);
			});
			if (scenario.screen() != null) {
				context.setScreen(scenario.screen());
			}
			context.waitTicks(10);
			double best = Double.MAX_VALUE;
			for (int round = 0; round < 3; round++) {
				if (scenario.name().startsWith("particles")) {
					world.getServer().runCommand("particle minecraft:flame 0 201 6 3 1 3 0.01 6000 force");
					context.waitTicks(2);
				}
				double[] ms = new double[1];
				context.runOnClient(client -> ms[0] = HollowClientGameTest.frameMillis(client, 30));
				best = Math.min(best, ms[0]);
			}
			long[] memory = new long[2];
			context.runOnClient(client -> {
				Runtime runtime = Runtime.getRuntime();
				memory[0] = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
				memory[1] = runtime.maxMemory() / (1024 * 1024);
			});
			lines.add(String.format(Locale.ROOT, "[bench] %-10s | %-45s | %7.2f ms/frame | heap %d/%d MB", label, scenario.name(), best,
				memory[0], memory[1]));
			if (scenario.screen() != null) {
				context.setScreen(() -> null);
			}
		}
		context.runOnClient(client -> {
			client.options.setCameraType(CameraType.FIRST_PERSON);
			Hollow.module(SkyStudioModule.class).setEnabled(false);
			Hollow.module(MotionBlurModule.class).setEnabled(false);
		});
		world.getServer().runCommand("kill @e[tag=bench]");
		context.waitTicks(5);
		for (String line : lines) {
			Hollow.LOGGER.info(line);
		}
	}
}
