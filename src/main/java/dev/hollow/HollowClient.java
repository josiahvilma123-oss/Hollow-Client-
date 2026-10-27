package dev.hollow;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.alerts.AlertService;
import dev.hollow.bridge.BridgeAssist;
import dev.hollow.combat.AimAssist;
import dev.hollow.combat.KillAura;
import dev.hollow.combat.KnockbackTester;
import dev.hollow.combat.ReachMeter;
import dev.hollow.core.config.ProfileStore;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.waypoints.WaypointStore;
import dev.hollow.exploration.VisibilityLog;
import dev.hollow.exploration.Waypoints;
import dev.hollow.hud.AlertsHudElement;
import dev.hollow.hud.HudElements;
import dev.hollow.hud.ModuleHuds;
import dev.hollow.modules.AimAssistModule;
import dev.hollow.modules.AutoBridgeModule;
import dev.hollow.modules.EntityVisibilityModule;
import dev.hollow.modules.FpsBoostModule;
import dev.hollow.modules.FreecamModule;
import dev.hollow.modules.HudLayoutModule;
import dev.hollow.modules.KillAuraModule;
import dev.hollow.modules.KnockbackTesterModule;
import dev.hollow.modules.PerformanceMonitorModule;
import dev.hollow.modules.ReachDisplayModule;
import dev.hollow.modules.SafeWalkModule;
import dev.hollow.modules.SpeedBridgeModule;
import dev.hollow.modules.TriggerBotModule;
import dev.hollow.modules.WaypointsModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import dev.hollow.core.config.ConfigManager;
import dev.hollow.core.module.ModuleManager;
import dev.hollow.core.util.Clock;
import dev.hollow.detection.BlockResults;
import dev.hollow.detection.BlockScanner;
import dev.hollow.detection.EntityTracker;
import dev.hollow.gui.HoloScreen;
import dev.hollow.menu.MenuScene;
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
import dev.hollow.character.CharacterView;
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

		// Registration order is the display order within each sidebar category.
		ModuleManager modules = new ModuleManager();
		modules.register(new KillAuraModule());
		modules.register(new TriggerBotModule());
		modules.register(new ReachDisplayModule());
		modules.register(new KnockbackTesterModule());
		modules.register(new AimAssistModule());
		modules.register(new AutoBridgeModule());
		modules.register(new SpeedBridgeModule());
		modules.register(new SafeWalkModule());
		modules.register(new MotionBlurModule());
		modules.register(new EntityTracersModule());
		modules.register(new FreecamModule());
		modules.register(new BlockEspModule());
		SkyStudioModule skyStudio = modules.register(new SkyStudioModule());
		modules.register(new WaypointsModule());
		modules.register(new BlockFinderModule());
		modules.register(new EntityVisibilityModule());
		modules.register(new HollowHandsModule());
		modules.register(new CharacterStudioModule());
		modules.register(new PerformanceModule());
		modules.register(new FpsBoostModule());
		modules.register(new HollowAlertsModule());
		modules.register(new PerformanceMonitorModule());
		modules.register(new HudLayoutModule());
		modules.register(new InterfaceModule());
		modules.register(new ThemePacksModule());
		modules.register(new MainMenuModule());
		modules.register(new LoadingScreenModule());
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
		CharacterView.register();

		ConfigManager config = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("hollow").resolve("hollow.json"),
			modules, Clock.SYSTEM, 750, Hollow.LOGGER::warn);
		Path configDir = FabricLoader.getInstance().getConfigDir().resolve("hollow");
		boolean hadConfig = Files.isRegularFile(config.file());
		config.load();
		config.watch();
		Hollow.config = config;

		ProfileStore profiles = new ProfileStore(configDir.resolve("profiles"), config, Hollow.LOGGER::warn);
		Hollow.profiles = profiles;
		if (hadConfig && profiles.list().isEmpty()) {
			// First run after the update: keep the existing settings as a profile the user can return to.
			try {
				profiles.saveCurrent("Default", System.currentTimeMillis());
			} catch (IOException | RuntimeException e) {
				Hollow.LOGGER.warn("Could not create the Default profile: {}", e.getMessage());
			}
		}
		Waypoints.INSTANCE.init(new WaypointStore(configDir.resolve("waypoints"), Hollow.LOGGER::warn));

		WorldOverlayRenderer.INSTANCE.addProvider(BridgeAssist.INSTANCE);
		WorldOverlayRenderer.INSTANCE.addProvider(KillAura.INSTANCE);
		WorldOverlayRenderer.INSTANCE.addProvider(ReachMeter.INSTANCE);
		WorldOverlayRenderer.INSTANCE.addProvider(Waypoints.INSTANCE);
		HudOverlay.INSTANCE.setAlertsElement(HudElements.register(new AlertsHudElement()));
		ModuleHuds.registerAll();

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			if (level.isClientSide() && player == Minecraft.getInstance().player) {
				AimAssist.INSTANCE.onAttack(entity);
				ReachMeter.INSTANCE.onAttack(Minecraft.getInstance().player, entity);
			}
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() && player == Minecraft.getInstance().player && modules.get(SpeedBridgeModule.class).isEnabled()) {
				BridgeAssist.INSTANCE.onManualPlace(player.getItemInHand(hand));
			}
			return InteractionResult.PASS;
		});

		KeyMapping.Category category = KeyMapping.Category.register(id("hollow"));
		openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.hollow.open_menu", InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT, category));

		ModuleKeybinds keybinds = new ModuleKeybinds(modules);
		HookHealth hookHealth = new HookHealth();
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openMenuKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new HoloScreen(null));
				}
			}
			keybinds.tick(client);
			for (Module module : modules.all()) {
				if (module instanceof Tickable tickable) {
					ModuleRuntime.safely(module, tickable::tick);
				}
			}
			ReachMeter.INSTANCE.tick();
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
			// Menu artwork isn't needed in game; free it until the next menu visit.
			MenuScene.release();
			Waypoints.INSTANCE.onWorldChanged();
			VisibilityLog.INSTANCE.forget();
			KnockbackTesterModule knockback = modules.get(KnockbackTesterModule.class);
			if (knockback.isEnabled()) {
				KnockbackTester.INSTANCE.applyResistance(knockback.resistance.get());
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			scanner.clear();
			entities.clear();
			ScreenProjector.INSTANCE.invalidate();
			// World-bound tools stop cleanly; their settings are kept.
			modules.get(FreecamModule.class).setEnabled(false);
			Waypoints.INSTANCE.onWorldChanged();
			VisibilityLog.INSTANCE.forget();
			AimAssist.INSTANCE.reset();
			KillAura.INSTANCE.reset();
			config.save();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

		WorldRenderEvents.END_MAIN.register(WorldOverlayRenderer.INSTANCE::render);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, id("labels"), HudOverlay.INSTANCE::renderLabels);
		HudElementRegistry.addLast(id("alerts"), HudOverlay.INSTANCE::renderAlerts);
		HudElementRegistry.addLast(id("hud_elements"), HudElements::render);

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
