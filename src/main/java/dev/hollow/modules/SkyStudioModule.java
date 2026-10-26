package dev.hollow.modules;

import com.google.gson.JsonObject;
import dev.hollow.Hollow;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.Setting;
import dev.hollow.core.setting.TextSetting;
import dev.hollow.sky.HoloSkyRenderer;
import dev.hollow.sky.SkyEnums.BodyStyle;
import dev.hollow.sky.SkyEnums.CloudMode;
import dev.hollow.sky.SkyEnums.Detail;
import dev.hollow.sky.SkyEnums.Motion;
import dev.hollow.sky.SkyEnums.Silhouettes;
import dev.hollow.sky.SkyEnums.TimeMode;
import dev.hollow.sky.SkyPresetStore;
import dev.hollow.sky.SkyPresets;
import dev.hollow.core.setting.ActionBarSetting;
import dev.hollow.core.setting.ActionSetting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/**
 * Holo Sky Studio: replaces the overworld sky with a layered procedural sky. Every visual value
 * is a setting, and presets are named snapshots of those settings.
 */
public final class SkyStudioModule extends Module {
	private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+\\.png");

	// Library state (not part of a preset).
	public final TextSetting preset = add("Library", new TextSetting("preset", "Active Preset", "", SkyPresets.VANILLA_ENHANCED,
		SkyPresetStore.MAX_NAME_LENGTH, s -> true, ""));
	public final TextSetting presetName = add("Library", new TextSetting("preset_name", "Preset Name",
		"Name used by Save As and Rename.", "My Sky", SkyPresetStore.MAX_NAME_LENGTH, s -> !s.isBlank(), "Preset name"));

	public final ActionBarSetting presetActions = add("Library", new ActionBarSetting("preset_actions", "Manage",
		"Save overwrites the active custom preset (built-ins are saved as a copy). Save As, Rename and Duplicate use the name above.",
		new ActionBarSetting.Button("Save", this::save),
		new ActionBarSetting.Button("Save As", () -> saveAs(presetName.get())),
		new ActionBarSetting.Button("Rename", () -> rename(presetName.get())),
		new ActionBarSetting.Button("Duplicate", this::duplicate),
		new ActionBarSetting.Button("Delete", this::deleteSelected),
		new ActionBarSetting.Button("Revert", this::revert)));
	public final ActionBarSetting shareActions = add("Library", new ActionBarSetting("share_actions", "Share",
		"Export copies the sky to the clipboard and writes it to skies/exports. Import reads a sky from the clipboard.",
		new ActionBarSetting.Button("Export", this::exportToClipboardAndFile),
		new ActionBarSetting.Button("Import", this::importFromClipboard),
		new ActionBarSetting.Button("Open Folder", this::openFolder)));
	public final ActionSetting restore = add("Library", new ActionSetting("restore_vanilla", "Restore Vanilla Sky",
		"Turns Sky Studio off. Your presets and current values are kept.", "Restore", this::restoreVanilla));
	public final BoolSetting advanced = add("Library", new BoolSetting("advanced", "Advanced Controls",
		"Show every sky parameter. Off shows the essentials.", false));

	private final List<Setting<?>> skyValues = new ArrayList<>();

	public final EnumSetting<TimeMode> timeMode = sky("Time & Motion", new EnumSetting<>("time_mode", "Time Mode",
		"Follow Day Cycle blends day and night colors with game time. Always Theme keeps the theme's look.", TimeMode.FOLLOW_GAME));
	public final EnumSetting<Motion> motion = sky("Time & Motion", new EnumSetting<>("motion", "Sun & Moon Motion",
		"Pinned keeps the sun and moon where the theme places them, for a composed sky.", Motion.FOLLOW_GAME));
	public final BoolSetting useVanillaDay = sky("Time & Motion", new BoolSetting("use_vanilla_day", "Vanilla Day Colors",
		"Use Minecraft's own (biome-aware) daytime sky color.", true));

	public final ColorSetting zenith = sky("Sky Gradient", new ColorSetting("zenith", "Night Zenith", "Color straight up at night.", 0xFF02030A, false));
	public final ColorSetting horizon = sky("Sky Gradient", new ColorSetting("horizon", "Night Horizon", "Color at the horizon at night.", 0xFF0B1020, false));
	public final ColorSetting dayZenith = sky("Sky Gradient", new ColorSetting("day_zenith", "Day Zenith", "", 0xFF6F9BFF, false));
	public final ColorSetting dayHorizon = sky("Sky Gradient", new ColorSetting("day_horizon", "Day Horizon", "", 0xFFBFD6FF, false));
	public final ColorSetting ground = sky("Sky Gradient", new ColorSetting("ground", "Below Horizon", "", 0xFF05070C, false));
	public final NumberSetting gradient = sky("Sky Gradient", new NumberSetting("gradient", "Gradient Curve",
		"Lower values keep horizon color higher up the sky.", 1.0, 0.2, 3.0, 0.05));
	public final ColorSetting horizonGlow = sky("Sky Gradient", new ColorSetting("horizon_glow", "Horizon Glow", "", 0xFFFFB27A, false));
	public final NumberSetting glowStrength = sky("Sky Gradient", new NumberSetting("glow_strength", "Glow Strength", "", 0.2, 0, 1, 0.05));

	public final NumberSetting brightness = sky("Brightness", new NumberSetting("brightness", "Sky Brightness", "", 1.0, 0.2, 2.0, 0.05).unit("x"));
	public final NumberSetting intensity = sky("Brightness", new NumberSetting("intensity", "Visual Intensity",
		"Color saturation of the whole sky.", 1.0, 0, 2.0, 0.05).unit("x"));

	public final EnumSetting<BodyStyle> sunStyle = sky("Sun", new EnumSetting<>("sun_style", "Sun Style", "", BodyStyle.DISC));
	public final ColorSetting sunColor = sky("Sun", new ColorSetting("sun_color", "Sun Color", "", 0xFFFFF6D8, false));
	public final NumberSetting sunSize = sky("Sun", new NumberSetting("sun_size", "Sun Size", "", 5, 1, 20, 0.5).unit("°"));
	public final NumberSetting sunGlow = sky("Sun", new NumberSetting("sun_glow", "Sun Glow", "", 0.45, 0, 1, 0.05));
	public final NumberSetting sunElevation = sky("Sun", new NumberSetting("sun_elevation", "Pinned Elevation", "Used when motion is Pinned.", 20, -10, 90, 1).unit("°"));
	public final NumberSetting sunAzimuth = sky("Sun", new NumberSetting("sun_azimuth", "Pinned Direction", "Used when motion is Pinned. 0 = south, 90 = west.", 90, 0, 359, 1).unit("°"));

	public final EnumSetting<BodyStyle> moonStyle = sky("Moon", new EnumSetting<>("moon_style", "Moon Style", "", BodyStyle.CRESCENT));
	public final ColorSetting moonColor = sky("Moon", new ColorSetting("moon_color", "Moon Color", "", 0xFFE8ECF5, false));
	public final NumberSetting moonSize = sky("Moon", new NumberSetting("moon_size", "Moon Size", "", 4, 1, 20, 0.5).unit("°"));
	public final NumberSetting moonGlow = sky("Moon", new NumberSetting("moon_glow", "Moon Glow", "", 0.3, 0, 1, 0.05));
	public final NumberSetting moonElevation = sky("Moon", new NumberSetting("moon_elevation", "Pinned Elevation", "", 40, -10, 90, 1).unit("°"));
	public final NumberSetting moonAzimuth = sky("Moon", new NumberSetting("moon_azimuth", "Pinned Direction", "", 270, 0, 359, 1).unit("°"));

	public final NumberSetting starDensity = sky("Stars", new NumberSetting("star_density", "Density", "", 0.3, 0, 1, 0.01));
	public final NumberSetting starSize = sky("Stars", new NumberSetting("star_size", "Size", "", 1.0, 0.4, 3, 0.05).unit("x"));
	public final NumberSetting starBrightness = sky("Stars", new NumberSetting("star_brightness", "Brightness", "", 1.0, 0, 2, 0.05).unit("x"));
	public final ColorSetting starColor = sky("Stars", new ColorSetting("star_color", "Color", "", 0xFFFFFFFF, false));
	public final NumberSetting starVariance = sky("Stars", new NumberSetting("star_variance", "Color Variance", "Mix of warm and cool stars.", 0.15, 0, 1, 0.05));
	public final NumberSetting twinkle = sky("Stars", new NumberSetting("twinkle", "Twinkle", "", 0.25, 0, 1, 0.05));

	public final BoolSetting nebula = sky("Nebula", new BoolSetting("nebula", "Nebula", "Soft clouds of color across a band of the sky.", false));
	public final ColorSetting nebulaA = sky("Nebula", new ColorSetting("nebula_a", "Primary Color", "", 0xFF5B1F9E, false));
	public final ColorSetting nebulaB = sky("Nebula", new ColorSetting("nebula_b", "Secondary Color", "", 0xFF1E3A8A, false));
	public final NumberSetting nebulaIntensity = sky("Nebula", new NumberSetting("nebula_intensity", "Intensity", "", 0.4, 0, 1.5, 0.05));
	public final NumberSetting nebulaScale = sky("Nebula", new NumberSetting("nebula_scale", "Scale", "", 1.0, 0.3, 3, 0.05).unit("x"));

	public final NumberSetting planets = sky("Planets", new NumberSetting("planets", "Planets", "Distant planets that drift with the stars.", 0, 0, 2, 1));
	public final ColorSetting planetAColor = sky("Planets", new ColorSetting("planet_a_color", "First Planet Color", "", 0xFFB89AE8, false));
	public final NumberSetting planetASize = sky("Planets", new NumberSetting("planet_a_size", "First Planet Size", "", 7, 1, 20, 0.5).unit("°"));
	public final BoolSetting planetARing = sky("Planets", new BoolSetting("planet_a_ring", "First Planet Ring", "", false));
	public final ColorSetting planetBColor = sky("Planets", new ColorSetting("planet_b_color", "Second Planet Color", "", 0xFF6FA8C8, false));
	public final NumberSetting planetBSize = sky("Planets", new NumberSetting("planet_b_size", "Second Planet Size", "", 4, 1, 20, 0.5).unit("°"));
	public final BoolSetting planetBRing = sky("Planets", new BoolSetting("planet_b_ring", "Second Planet Ring", "", false));

	public final BoolSetting ring = sky("Celestial Ring", new BoolSetting("ring", "Celestial Ring", "An ornamental ring around the moon.", false));
	public final ColorSetting ringColor = sky("Celestial Ring", new ColorSetting("ring_color", "Ring Color", "", 0xFFD8A84C, false));
	public final NumberSetting ringRadius = sky("Celestial Ring", new NumberSetting("ring_radius", "Ring Radius", "", 10, 2, 30, 0.5).unit("°"));
	public final NumberSetting ringOrnaments = sky("Celestial Ring", new NumberSetting("ring_ornaments", "Ornaments", "Glyph marks around the ring.", 12, 0, 24, 1));

	public final EnumSetting<Silhouettes> silhouettes = sky("Horizon", new EnumSetting<>("silhouettes", "Distant Silhouettes",
		"Decorative shapes on the far horizon, behind all terrain.", Silhouettes.NONE));
	public final ColorSetting silhouetteColor = sky("Horizon", new ColorSetting("silhouette_color", "Silhouette Color", "", 0xFF0A0810, false));

	public final EnumSetting<CloudMode> cloudMode = sky("Clouds & Fog", new EnumSetting<>("cloud_mode", "Clouds", "", CloudMode.VANILLA));
	public final ColorSetting cloudColor = sky("Clouds & Fog", new ColorSetting("cloud_color", "Cloud Tint", "", 0xFFFFFFFF, false));
	public final NumberSetting fogTint = sky("Clouds & Fog", new NumberSetting("fog_tint", "Fog Tint",
		"Blend distance fog toward the sky's horizon color so terrain meets the sky seamlessly.", 0, 0, 1, 0.05));

	public final TextSetting texture = sky("Texture Layer", new TextSetting("texture", "Sky Texture",
		"Equirectangular PNG from a resource pack, e.g. hollow:textures/sky/aurora_veil.png. Empty for none.", "", 128,
		RESOURCE_ID.asMatchPredicate(), "namespace:textures/sky/name.png"));
	public final NumberSetting textureOpacity = sky("Texture Layer", new NumberSetting("texture_opacity", "Texture Opacity", "", 0.6, 0, 1, 0.05));

	public final EnumSetting<Detail> detail = add("Performance", new EnumSetting<>("detail", "Detail",
		"Low skips nebula noise and the second star layer.", Detail.HIGH));

	private boolean applying;
	private boolean modified;
	private long version;
	private String message = "";

	public SkyStudioModule() {
		super("sky_studio", "Holo Sky Studio", "Original custom skies, presets and a full sky creator for the overworld.", Category.STUDIO, false);
		for (Setting<?> setting : skyValues) {
			setting.onChange(value -> {
				version++;
				if (!applying) {
					modified = true;
				}
			});
		}
		detail.onChange(value -> version++);
		BooleanVisibility.when(nebula, nebulaA, nebulaB, nebulaIntensity, nebulaScale);
		BooleanVisibility.when(ring, ringColor, ringRadius, ringOrnaments);
		planetAColor.visibleWhen(() -> planets.getInt() >= 1);
		planetASize.visibleWhen(() -> planets.getInt() >= 1);
		planetARing.visibleWhen(() -> planets.getInt() >= 1);
		planetBColor.visibleWhen(() -> planets.getInt() >= 2);
		planetBSize.visibleWhen(() -> planets.getInt() >= 2);
		planetBRing.visibleWhen(() -> planets.getInt() >= 2);
		silhouetteColor.visibleWhen(() -> silhouettes.get() != Silhouettes.NONE);
		cloudColor.visibleWhen(() -> cloudMode.get() == CloudMode.TINTED);
		textureOpacity.visibleWhen(() -> !texture.get().isEmpty());
		dayZenith.visibleWhen(() -> timeMode.get() == TimeMode.FOLLOW_GAME && !useVanillaDay.get());
		dayHorizon.visibleWhen(() -> timeMode.get() == TimeMode.FOLLOW_GAME && !useVanillaDay.get());
		useVanillaDay.visibleWhen(() -> timeMode.get() == TimeMode.FOLLOW_GAME);
		sunElevation.visibleWhen(() -> motion.get() == Motion.PINNED);
		sunAzimuth.visibleWhen(() -> motion.get() == Motion.PINNED);
		moonElevation.visibleWhen(() -> motion.get() == Motion.PINNED);
		moonAzimuth.visibleWhen(() -> motion.get() == Motion.PINNED);
		// Simple mode shows the essentials; this runs last so it composes with the conditions above.
		List<Setting<?>> simple = List.of(timeMode, zenith, horizon, brightness, sunStyle, sunColor, moonStyle, moonColor, starDensity,
			nebula, ring, silhouettes, cloudMode);
		for (Setting<?> setting : skyValues) {
			if (!simple.contains(setting)) {
				setting.alsoVisibleWhen(advanced::get);
			}
		}
	}

	private <S extends Setting<?>> S sky(String group, S setting) {
		skyValues.add(setting);
		return add(group, setting);
	}

	/** Bumped on every change to a sky value, so the renderer only repacks parameters when needed. */
	public long version() {
		return version;
	}

	public boolean modified() {
		return modified;
	}

	public String message() {
		return message;
	}

	public void message(String text) {
		message = text == null ? "" : text;
	}

	/** Current sky values as preset data. */
	public JsonObject captureValues() {
		JsonObject values = new JsonObject();
		for (Setting<?> setting : skyValues) {
			values.add(setting.id(), setting.toJson());
		}
		return values;
	}

	/** Default values, the base that built-in presets override. */
	public JsonObject defaultValues() {
		JsonObject values = new JsonObject();
		for (Setting<?> setting : skyValues) {
			values.add(setting.id(), setting.defaultJson());
		}
		return values;
	}

	public void applyValues(JsonObject values) {
		applying = true;
		try {
			for (Setting<?> setting : skyValues) {
				if (values.has(setting.id())) {
					setting.fromJson(values.get(setting.id()));
				} else {
					setting.reset();
				}
			}
		} finally {
			applying = false;
		}
		modified = false;
	}

	// ---------------------------------------------------------------- preset actions (used by the GUI)

	private SkyPresetStore store() {
		return Hollow.skyPresets();
	}

	public void applyPreset(String name) {
		store().find(name).ifPresentOrElse(p -> {
			applyValues(p.values());
			preset.set(p.name());
			message("Applied " + p.name());
		}, () -> message("No preset named " + name));
	}

	public void revert() {
		applyPreset(preset.get());
	}

	public void save() {
		if (store().isBuiltIn(preset.get())) {
			saveAs(store().uniqueName(preset.get() + " Custom"));
			return;
		}
		runSafely(() -> {
			String description = store().find(preset.get()).map(SkyPresetStore.Preset::description).orElse("");
			store().save(preset.get(), description, captureValues());
			modified = false;
			message("Saved " + preset.get());
		});
	}

	public void saveAs(String name) {
		runSafely(() -> {
			String unique = store().uniqueName(name);
			store().save(unique, "Custom sky", captureValues());
			preset.set(unique);
			modified = false;
			message("Saved as " + unique);
		});
	}

	public void rename(String name) {
		runSafely(() -> {
			SkyPresetStore.Preset renamed = store().rename(preset.get(), name);
			preset.set(renamed.name());
			message("Renamed to " + renamed.name());
		});
	}

	public void duplicate() {
		runSafely(() -> {
			SkyPresetStore.Preset copy = store().duplicate(preset.get());
			preset.set(copy.name());
			applyValues(copy.values());
			message("Duplicated as " + copy.name());
		});
	}

	public void deleteSelected() {
		runSafely(() -> {
			String name = preset.get();
			if (store().delete(name)) {
				message("Deleted " + name);
				applyPreset(SkyPresets.VANILLA_ENHANCED);
			} else {
				message("Built-in presets can't be deleted");
			}
		});
	}

	public String exportSelected() {
		SkyPresetStore.Preset current = new SkyPresetStore.Preset(preset.get(),
			store().find(preset.get()).map(SkyPresetStore.Preset::description).orElse(""), false, captureValues());
		return store().export(current);
	}

	public void importJson(String json) {
		runSafely(() -> {
			SkyPresetStore.Preset imported = store().importJson(json);
			applyValues(imported.values());
			preset.set(imported.name());
			message("Imported " + imported.name());
		});
	}

	private void exportToClipboardAndFile() {
		runSafely(() -> {
			String json = exportSelected();
			Minecraft.getInstance().keyboardHandler.setClipboard(json);
			Path exports = store().directory().resolve("exports");
			try {
				Files.createDirectories(exports);
				Path file = exports.resolve(SkyPresetStore.slug(preset.get()) + ".json");
				Files.writeString(file, json);
				message("Copied to clipboard and saved exports/" + file.getFileName());
			} catch (IOException e) {
				message("Copied to clipboard (could not write file: " + e.getMessage() + ")");
			}
		});
	}

	private void importFromClipboard() {
		String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
		if (clipboard == null || clipboard.isBlank()) {
			message("Clipboard is empty; copy an exported sky first");
			return;
		}
		importJson(clipboard);
	}

	private void openFolder() {
		try {
			Files.createDirectories(store().directory());
			Util.getPlatform().openPath(store().directory());
		} catch (IOException e) {
			message("Could not open folder: " + e.getMessage());
		}
	}

	public void restoreVanilla() {
		setEnabled(false);
		message("Vanilla sky restored");
	}

	private void runSafely(Runnable action) {
		try {
			action.run();
		} catch (RuntimeException e) {
			message(e.getMessage());
		}
	}

	@Override
	protected void onDisable() {
		HoloSkyRenderer.INSTANCE.release();
	}
}
