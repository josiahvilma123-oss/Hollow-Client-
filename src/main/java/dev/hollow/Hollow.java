package dev.hollow;

import dev.hollow.alerts.AlertService;
import dev.hollow.core.config.ConfigManager;
import dev.hollow.core.module.ModuleManager;
import dev.hollow.detection.BlockScanner;
import dev.hollow.detection.EntityTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared access to Hollow's long-lived services. Populated once in {@link HollowClient}. */
public final class Hollow {
	public static final String MOD_ID = "hollow";
	public static final String NAME = "Holo";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	static ModuleManager modules;
	static ConfigManager config;
	static AlertService alerts;
	static BlockScanner scanner;
	static EntityTracker entities;
	static dev.hollow.sky.SkyPresetStore skyPresets;

	private Hollow() {
	}

	public static ModuleManager modules() {
		return modules;
	}

	public static ConfigManager config() {
		return config;
	}

	public static AlertService alerts() {
		return alerts;
	}

	public static BlockScanner scanner() {
		return scanner;
	}

	public static EntityTracker entities() {
		return entities;
	}

	public static dev.hollow.sky.SkyPresetStore skyPresets() {
		return skyPresets;
	}

	public static <M extends dev.hollow.core.module.Module> M module(Class<M> type) {
		return modules.get(type);
	}
}
