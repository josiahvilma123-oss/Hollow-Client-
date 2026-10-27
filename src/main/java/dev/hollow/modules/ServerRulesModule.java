package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.StringListSetting;
import dev.hollow.policy.ServerContext;
import dev.hollow.policy.ServerPolicy;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lets the user follow each server's rules. Detection only ever uses data the client already
 * has; this module additionally turns it off where a server forbids it.
 */
public final class ServerRulesModule extends Module {
	private static final Pattern ADDRESS = Pattern.compile("(\\*\\.)?[a-z0-9.-]+(:\\d{1,5})?");

	public final StringListSetting restricted = add("Restricted Servers", new StringListSetting("restricted", "Restricted Servers",
		"World Scanner, Block ESP, Entity Tracers and Freecam stay off on these servers. Supports *.example.net.",
		List.of(), ADDRESS.asMatchPredicate(), "play.example.net"));
	public final ActionSetting restrictCurrent = add("Restricted Servers", new ActionSetting("restrict_current", "Restrict This Server",
		"Add the server you are connected to.", "Add current server", () -> ServerContext.current().address().ifPresent(this::restrict)));

	public final StringListSetting playerServers = add("Player Detection", new StringListSetting("player_servers", "Servers Allowing Player Detection",
		"Player tracers and alerts are off in multiplayer unless the server is listed here. Only add servers whose rules allow it.",
		List.of(), ADDRESS.asMatchPredicate(), "play.example.net"));
	public final ActionSetting permitCurrent = add("Player Detection", new ActionSetting("permit_current", "Permit On This Server",
		"Add the server you are connected to. Only do this if its rules allow player tracers.", "Add current server",
		() -> ServerContext.current().address().ifPresent(playerServers::add)));

	public final StringListSetting automationServers = add("Automation", new StringListSetting("automation_servers",
		"Servers Permitting Automation",
		"Aim Assist, Kill Aura, Trigger Bot, automated bridging and Safe Walk only act in single-player, or on servers listed here. "
			+ "Only add a server (such as your own practice server) whose rules allow automated play.",
		List.of(), ADDRESS.asMatchPredicate(), "practice.example.net"));
	public final ActionSetting automationCurrent = add("Automation", new ActionSetting("automation_current", "Permit On This Server",
		"Add the server you are connected to. Only do this if its rules allow automated play.", "Add current server",
		() -> ServerContext.current().address().ifPresent(automationServers::add)));

	public ServerRulesModule() {
		super("server_rules", "Server Rules", "Follow each server's rules: turn detection off where required, and choose where automation may act.", Category.SETTINGS, true);
	}

	@Override
	public boolean isToggleable() {
		return false;
	}

	private void restrict(String address) {
		restricted.add(address);
	}

	public boolean detectionAllowed() {
		ServerContext context = ServerContext.current();
		return ServerPolicy.detectionAllowed(context.singleplayer(), context.address().orElse(null), restricted.get());
	}

	/** Whether automated play (aiming, attacking, placing, edge-holding) may act here. */
	public boolean automationAllowed() {
		ServerContext context = ServerContext.current();
		return ServerPolicy.automationAllowed(context.singleplayer(), context.address().orElse(null), restricted.get(), automationServers.get());
	}

	public boolean playersAllowed() {
		ServerContext context = ServerContext.current();
		return ServerPolicy.playerDetectionAllowed(context.singleplayer(), context.address().orElse(null), restricted.get(), playerServers.get());
	}
}
