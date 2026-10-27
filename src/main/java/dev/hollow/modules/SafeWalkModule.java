package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.policy.Automation;
import java.util.Optional;

/**
 * Stops you walking off block edges, like sneaking does, but at full walking speed. Jumping and
 * flying are unaffected. Single-player and permitted servers only.
 */
public final class SafeWalkModule extends Module {
	public SafeWalkModule() {
		super("safe_walk", "Safe Walk", "Never walk off an edge by accident, without slowing down like sneaking. "
			+ "Single-player and permitted servers only.", Category.MOVEMENT, false);
	}

	/** Whether edge protection applies right now. */
	public boolean active() {
		return isEnabled() && Automation.allowed();
	}

	@Override
	public Optional<String> blockedReason() {
		return Automation.blockedReason();
	}
}
