package dev.hollow.compat;

import dev.hollow.Hollow;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Detects an active Iris shader pack without a compile-time dependency on Iris.
 * Any failure is treated as "no shader pack", and logged once.
 */
public final class ShaderPackCompat {
	private static final boolean IRIS_PRESENT = FabricLoader.getInstance().isModLoaded("iris");
	private static Object irisApi;
	private static Method inUse;
	private static boolean failed;

	private ShaderPackCompat() {
	}

	public static boolean irisPresent() {
		return IRIS_PRESENT;
	}

	public static boolean shaderPackActive() {
		if (!IRIS_PRESENT || failed) {
			return false;
		}
		try {
			if (inUse == null) {
				Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
				irisApi = api.getMethod("getInstance").invoke(null);
				inUse = api.getMethod("isShaderPackInUse");
			}
			return (Boolean) inUse.invoke(irisApi);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			failed = true;
			Hollow.LOGGER.warn("Could not query Iris shader pack state; assuming none is active", e);
			return false;
		}
	}
}
