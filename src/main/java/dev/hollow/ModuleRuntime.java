package dev.hollow;

import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.core.module.Module;

/** Runs module code so an unexpected error turns that one module off (and says so) instead of crashing the game. */
public final class ModuleRuntime {
	private ModuleRuntime() {
	}

	public static void safely(Module module, Runnable work) {
		try {
			work.run();
		} catch (RuntimeException e) {
			Hollow.LOGGER.error("{} failed and was turned off", module.name(), e);
			try {
				module.setEnabled(false);
			} catch (RuntimeException ignored) {
				// The module's own disable code failed too; it is still marked off.
			}
			if (Hollow.alerts() != null) {
				Hollow.alerts().report(new Alert(AlertSource.SYSTEM, module.name(), "turned off after an error", Double.NaN,
					"error:" + module.id(), "see log"));
			}
		}
	}
}
