package dev.hollow.sky;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hollow's original built-in skies. Each preset lists only the values that differ from the
 * Sky Studio defaults; everything else falls back to those defaults when applied.
 */
public final class SkyPresets {
	public static final String VANILLA_ENHANCED = "Vanilla Enhanced";

	private SkyPresets() {
	}

	public static List<SkyPresetStore.Preset> builtIns(JsonObject defaults) {
		List<SkyPresetStore.Preset> presets = new ArrayList<>();
		presets.add(build(defaults, "Anubis Eclipse", "A dark celestial sky held by a burning eclipse, gold corona and violet haze.", Map.ofEntries(
			Map.entry("time_mode", "FIXED"), Map.entry("motion", "PINNED"),
			Map.entry("zenith", "#FF07040D"), Map.entry("horizon", "#FF1D0F24"), Map.entry("ground", "#FF050307"),
			Map.entry("horizon_glow", "#FFB8862F"), Map.entry("glow_strength", 0.35), Map.entry("gradient", 1.4),
			Map.entry("sun_style", "HIDDEN"),
			Map.entry("moon_style", "ECLIPSE"), Map.entry("moon_color", "#FFE7B85A"), Map.entry("moon_size", 8.0), Map.entry("moon_glow", 0.9),
			Map.entry("moon_elevation", 34.0), Map.entry("moon_azimuth", 200.0),
			Map.entry("star_density", 0.35), Map.entry("star_color", "#FFE9DCC8"), Map.entry("star_variance", 0.4), Map.entry("twinkle", 0.4),
			Map.entry("nebula", true), Map.entry("nebula_a", "#FF4A1B6B"), Map.entry("nebula_b", "#FF8C5A1E"), Map.entry("nebula_intensity", 0.35),
			Map.entry("silhouettes", "OBELISKS"), Map.entry("silhouette_color", "#FF0B0710"),
			Map.entry("cloud_mode", "TINTED"), Map.entry("cloud_color", "#FF2A1D33"), Map.entry("fog_tint", 0.6))));
		presets.add(build(defaults, "Void Realm", "A deep, still void with distant stars and slow purple nebulae.", Map.ofEntries(
			Map.entry("time_mode", "FIXED"),
			Map.entry("zenith", "#FF020106"), Map.entry("horizon", "#FF120A1E"), Map.entry("ground", "#FF010103"),
			Map.entry("horizon_glow", "#FF6B2FB0"), Map.entry("glow_strength", 0.25),
			Map.entry("sun_style", "HIDDEN"), Map.entry("moon_color", "#FFCFC2E8"), Map.entry("moon_size", 4.0), Map.entry("moon_glow", 0.4),
			Map.entry("star_density", 0.55), Map.entry("star_size", 0.9), Map.entry("star_brightness", 1.1), Map.entry("star_color", "#FFD9CCFF"),
			Map.entry("star_variance", 0.5), Map.entry("twinkle", 0.6),
			Map.entry("nebula", true), Map.entry("nebula_a", "#FF5B1F9E"), Map.entry("nebula_b", "#FF1E3A8A"), Map.entry("nebula_intensity", 0.75),
			Map.entry("nebula_scale", 1.3),
			Map.entry("planets", 1.0), Map.entry("planet_a_color", "#FF3B2A55"), Map.entry("planet_a_size", 6.0),
			Map.entry("cloud_mode", "HIDDEN"), Map.entry("fog_tint", 0.7))));
		presets.add(build(defaults, "Pharaoh's Night", "An ancient night over the dunes: a ringed moon, golden glyphs and pyramids on the horizon.", Map.ofEntries(
			Map.entry("time_mode", "FIXED"), Map.entry("motion", "PINNED"),
			Map.entry("zenith", "#FF070A1C"), Map.entry("horizon", "#FF1E1530"), Map.entry("ground", "#FF0A0806"),
			Map.entry("horizon_glow", "#FFE0A84A"), Map.entry("glow_strength", 0.45),
			Map.entry("sun_style", "HIDDEN"),
			Map.entry("moon_color", "#FFF3E2B0"), Map.entry("moon_size", 6.0), Map.entry("moon_glow", 0.6),
			Map.entry("moon_elevation", 42.0), Map.entry("moon_azimuth", 160.0),
			Map.entry("ring", true), Map.entry("ring_color", "#FFD8A84C"), Map.entry("ring_radius", 11.0), Map.entry("ring_ornaments", 16.0),
			Map.entry("star_density", 0.45), Map.entry("star_size", 1.1), Map.entry("star_color", "#FFFFF0D0"), Map.entry("star_variance", 0.25),
			Map.entry("nebula", true), Map.entry("nebula_a", "#FF3A2470"), Map.entry("nebula_b", "#FFB07A2A"), Map.entry("nebula_intensity", 0.25),
			Map.entry("silhouettes", "PYRAMIDS"), Map.entry("silhouette_color", "#FF0D0A08"),
			Map.entry("cloud_mode", "TINTED"), Map.entry("cloud_color", "#FF3A2E3A"), Map.entry("fog_tint", 0.55))));
		presets.add(build(defaults, "Astral Horizon", "A smooth gradient sky with distant ringed planets that follows the day.", Map.ofEntries(
			Map.entry("use_vanilla_day", false), Map.entry("zenith", "#FF060B1E"), Map.entry("horizon", "#FF2B2350"),
			Map.entry("day_zenith", "#FF3F5FB8"), Map.entry("day_horizon", "#FFC8A8E0"),
			Map.entry("horizon_glow", "#FFFF9E6B"), Map.entry("glow_strength", 0.4), Map.entry("gradient", 0.8),
			Map.entry("sun_style", "HALO"), Map.entry("sun_color", "#FFFFE7C2"), Map.entry("sun_glow", 0.6),
			Map.entry("planets", 2.0), Map.entry("planet_a_color", "#FFB89AE8"), Map.entry("planet_a_size", 9.0), Map.entry("planet_a_ring", true),
			Map.entry("planet_b_color", "#FF6FA8C8"), Map.entry("planet_b_size", 4.0),
			Map.entry("star_density", 0.35), Map.entry("fog_tint", 0.35), Map.entry("cloud_mode", "TINTED"), Map.entry("cloud_color", "#FFE6DCF5"))));
		presets.add(build(defaults, "Crimson Dusk", "A dramatic, endless sunset in deep red and violet.", Map.ofEntries(
			Map.entry("time_mode", "FIXED"), Map.entry("motion", "PINNED"),
			Map.entry("zenith", "#FF1A0614"), Map.entry("horizon", "#FF8A1E1E"), Map.entry("ground", "#FF120406"),
			Map.entry("horizon_glow", "#FFFF5A2A"), Map.entry("glow_strength", 0.85), Map.entry("gradient", 0.6),
			Map.entry("sun_style", "HALO"), Map.entry("sun_color", "#FFFF7A3A"), Map.entry("sun_size", 10.0), Map.entry("sun_glow", 1.0),
			Map.entry("sun_elevation", 5.0), Map.entry("sun_azimuth", 270.0),
			Map.entry("moon_color", "#FFF0C8D8"), Map.entry("moon_size", 3.5), Map.entry("moon_elevation", 48.0), Map.entry("moon_azimuth", 100.0),
			Map.entry("star_density", 0.18), Map.entry("star_brightness", 0.7),
			Map.entry("cloud_mode", "TINTED"), Map.entry("cloud_color", "#FFB0485A"), Map.entry("fog_tint", 0.75))));
		presets.add(build(defaults, "Frozen Cosmos", "A cold blue night with an aurora veil, icy stars and a frost-ringed moon.", Map.ofEntries(
			Map.entry("time_mode", "FIXED"),
			Map.entry("zenith", "#FF020814"), Map.entry("horizon", "#FF0E2A44"), Map.entry("ground", "#FF02060C"),
			Map.entry("horizon_glow", "#FF5FC8FF"), Map.entry("glow_strength", 0.3),
			Map.entry("sun_style", "HIDDEN"), Map.entry("moon_color", "#FFDDF3FF"), Map.entry("moon_size", 5.0), Map.entry("moon_glow", 0.5),
			Map.entry("ring", true), Map.entry("ring_color", "#FF9FD8FF"), Map.entry("ring_radius", 8.0), Map.entry("ring_ornaments", 6.0),
			Map.entry("star_density", 0.5), Map.entry("star_color", "#FFCFE8FF"), Map.entry("star_variance", 0.3),
			Map.entry("nebula", true), Map.entry("nebula_a", "#FF1B5A7A"), Map.entry("nebula_b", "#FF2A2F7A"), Map.entry("nebula_intensity", 0.4),
			Map.entry("texture", "hollow:textures/sky/aurora_veil.png"), Map.entry("texture_opacity", 0.6),
			Map.entry("silhouettes", "SPIRES"), Map.entry("silhouette_color", "#FF06101A"),
			Map.entry("cloud_mode", "TINTED"), Map.entry("cloud_color", "#FF8FB8D8"), Map.entry("fog_tint", 0.5))));
		presets.add(build(defaults, "Hellfire Rift", "A burning night: a black sun ringed in fire, ember stars and a smouldering rift.", Map.ofEntries(
			Map.entry("time_mode", "FIXED"), Map.entry("motion", "PINNED"),
			Map.entry("zenith", "#FF0C0204"), Map.entry("horizon", "#FF5A1208"), Map.entry("ground", "#FF0A0203"),
			Map.entry("horizon_glow", "#FFFF6A1A"), Map.entry("glow_strength", 0.7), Map.entry("gradient", 0.7),
			Map.entry("sun_style", "ECLIPSE"), Map.entry("sun_color", "#FFFF7A2A"), Map.entry("sun_size", 7.0), Map.entry("sun_glow", 0.9),
			Map.entry("sun_elevation", 28.0), Map.entry("sun_azimuth", 200.0), Map.entry("moon_style", "HIDDEN"),
			Map.entry("star_density", 0.3), Map.entry("star_color", "#FFFFB070"), Map.entry("star_variance", 0.4), Map.entry("twinkle", 0.6),
			Map.entry("nebula", true), Map.entry("nebula_a", "#FF8A1A0A"), Map.entry("nebula_b", "#FF4A0A2A"), Map.entry("nebula_intensity", 0.6),
			Map.entry("silhouettes", "SPIRES"), Map.entry("silhouette_color", "#FF080203"),
			Map.entry("cloud_mode", "TINTED"), Map.entry("cloud_color", "#FF7A2A22"), Map.entry("fog_tint", 0.7))));
		presets.add(build(defaults, VANILLA_ENHANCED, "Minecraft's sky, refined: smoother gradients, softer sun, real moon phases and richer stars.",
			Map.of()));
		return presets;
	}

	private static SkyPresetStore.Preset build(JsonObject defaults, String name, String description, Map<String, Object> overrides) {
		JsonObject values = defaults.deepCopy();
		for (Map.Entry<String, Object> entry : new LinkedHashMap<>(overrides).entrySet()) {
			Object value = entry.getValue();
			if (!values.has(entry.getKey())) {
				throw new IllegalArgumentException("Preset " + name + " sets unknown key " + entry.getKey());
			}
			if (value instanceof Boolean bool) {
				values.add(entry.getKey(), new JsonPrimitive(bool));
			} else if (value instanceof Number number) {
				values.add(entry.getKey(), new JsonPrimitive(number));
			} else {
				values.add(entry.getKey(), new JsonPrimitive(value.toString()));
			}
		}
		return new SkyPresetStore.Preset(name, description, true, values);
	}
}
