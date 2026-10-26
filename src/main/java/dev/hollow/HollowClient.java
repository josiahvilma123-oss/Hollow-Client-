package dev.hollow;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.alerts.AlertService;
import dev.hollow.core.config.ConfigManager;
import dev.hollow.core.module.ModuleManager;
import dev.hollow.core.util.Clock;
import dev.hollow.detection.BlockResults;
import dev.hollow.detection.BlockScanner;
import dev.hollow.detection.EntityTracker;
import dev.hollow.gui.HollowScreen;
import dev.hollow.modules.BlockEspModule;
import dev.hollow.modules.BlockFinderModule;
import dev.hollow.modules.EntityTracersModule;
import dev.hollow.modules.HollowAlertsModule;
import dev.hollow.modules.HollowHandsModule;
import dev.hollow.modules.InterfaceModule;
import dev.hollow.modules.MotionBlurModule;
import dev.hollow.modules.ServerRulesModule;
import dev.hollow.render.HollowPipelines;
import dev.hollow.render.HudOverlay;
import dev.hollow.render.ScreenProjector;
import dev.hollow.render.WorldOverlayRenderer;
import dev.hollow.character.CosmeticModels;
import dev.hollow.modules.CharacterStudioModule;
import dev.hollow.modules.LoadingScreenModule;
import dev.hollow.modules.MainMenuModule;
import dev.hollow.modules.PerformanceModule;
import dev.hollow.modules.SkyStudioModule;
import dev.hollow.modules.ThemePacksModule;
import dev.hollow.sky.HoloSkyRenderer;
import dev.hollow.sky.SkyPresetStore;
import dev.hollow.sky.SkyPresets;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleResourceReloader;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class HollowClient implements ClientModInitializer {
	private static KeyMapping openMenuKey;

	@Override
	public void onInitializeClient() {
		HollowPipelines.bootstrap();

		ModuleManager modules = new ModuleManager();
		modules.register(new MotionBlurModule());
		modules.register(new HollowHandsModule());
		modules.register(new BlockFinderModule());
		modules.register(new BlockEspModule());
		modules.register(new EntityTracersModule());
		modules.register(new HollowAlertsModule());
		modules.register(new InterfaceModule());
		SkyStudioModule skyStudio = modules.register(new SkyStudioModule());
		modules.register(new CharacterStudioModule());
		modules.register(new ThemePacksModule());
		modules.register(new MainMenuModule());
		modules.register(new LoadingScreenModule());
		modules.register(new PerformanceModule());
		modules.register(new ServerRulesModule());
		Hollow.modules = modules;

		BlockScanner scanner = new BlockScanner();
		EntityTracker entities = new EntityTracker();
		BlockResults blockResults = new BlockResults(scanner);
		Hollow.scanner = scanner;
		Hollow.entities = entities;
		Hollow.alerts = new AlertService(() -> modules.get(HollowAlertsModule.class));
		WorldOverlayRenderer.INSTANCE.setBlockResults(blockResults);

		SkyPresetStore skyPresets = new SkyPresetStore(FabricLoader.getInstance().getConfigDir().resolve("hollow").resolve("skies"),
			SkyPresets.builtIns(skyStudio.defaultValues()), Hollow.LOGGER::warn);
		skyPresets.load();
		Hollow.skyPresets = skyPresets;
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(id("sky_textures"), new SimpleResourceReloader<Void>() {
			@Override
			protected Void prepare(PreparableReloadListener.SharedState store) {
				return null;
			}

			@Override
			protected void apply(Void prepared, PreparableReloadListener.SharedState store) {
				HoloSkyRenderer.INSTANCE.onResourcesReloaded();
			}
		});

		CosmeticModels.register();

		ConfigManager config = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("hollow").resolve("hollow.json"),
			modules, Clock.SYSTEM, 750, Hollow.LOGGER::warn);
		config.load();
		config.watch();
		Hollow.config = config;

		KeyMapping.Category category = KeyMapping.Category.register(id("hollow"));
		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.hollow.open_menu", InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT, category));

		ModuleKeybinds keybinds = new ModuleKeybinds(modules);
		HookHealth hookHealth = new HookHealth();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new HollowScreen(null));
				}
			}
			keybinds.tick(client);
			scanner.tick(client);
			blockResults.tick(client.level, client.player == null ? null : client.player.getEyePosition());
			entities.tick(client);
			hookHealth.tick(client);
			config.tick();
		});

		ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> scanner.onChunkLoaded(chunk));
		ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> scanner.onChunkUnloaded(chunk));
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			// Block tags arrive from the server during login, so tag entries resolve only now.
			scanner.requestRebuild();
			Hollow.alerts().clear();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			scanner.clear();
			entities.clear();
			ScreenProjector.INSTANCE.invalidate();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

		WorldRenderEvents.END_MAIN.register(WorldOverlayRenderer.INSTANCE::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, id("labels"), HudOverlay.INSTANCE::renderLabels);
		HudElementRegistry.addLast(id("alerts"), HudOverlay.INSTANCE::renderAlerts);

		Hollow.LOGGER.info("Holo initialised with {} modules", modules.all().size());
	}

	public static KeyMapping openMenuKey() {
		return openMenuKey;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Hollow.MOD_ID, path);
	}

	static Minecraft client() {
		return Minecraft.getInstance();
	}
}
