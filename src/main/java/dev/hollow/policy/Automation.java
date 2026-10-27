package dev.hollow.policy;

import dev.hollow.Hollow;
import dev.hollow.modules.ServerRulesModule;
import java.util.Optional;
import net.minecraft.client.Minecraft;

/** Shared gate for modules that act on the player's behalf. */
public final class Automation {
	public static final String NOT_PERMITTED = "Single-player only here: add this server under Settings → Server Rules → Automation if its rules allow it";
	public static final String RESTRICTED = "Off on this server (listed under Server Rules → Restricted Servers)";

	private Automation() {
	}

	public static boolean allowed() {
		return Minecraft.getInstance().level != null && Hollow.module(ServerRulesModule.class).automationAllowed();
	}

	/** Why automation can't act here, or empty when it can (or when not in a world). */
	public static Optional<String> blockedReason() {
		if (Minecraft.getInstance().level == null) {
			return Optional.empty();
		}
		ServerRulesModule rules = Hollow.module(ServerRulesModule.class);
		if (rules.automationAllowed()) {
			return Optional.empty();
		}
		return Optional.of(rules.detectionAllowed() ? NOT_PERMITTED : RESTRICTED);
	}

	/** Why detection-style features (scanner, ESP, freecam) can't act here, or empty. */
	public static Optional<String> detectionBlockedReason() {
		if (Minecraft.getInstance().level == null || Hollow.module(ServerRulesModule.class).detectionAllowed()) {
			return Optional.empty();
		}
		return Optional.of(RESTRICTED);
	}
}
