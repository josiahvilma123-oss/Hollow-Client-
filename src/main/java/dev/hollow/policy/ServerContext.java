package dev.hollow.policy;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/** Snapshot of where the client is connected. */
public record ServerContext(boolean singleplayer, Optional<String> address) {
	public static ServerContext current() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.hasSingleplayerServer()) {
			return new ServerContext(true, Optional.empty());
		}
		ServerData server = minecraft.getCurrentServer();
		if (server == null || server.ip == null || server.ip.isBlank()) {
			return new ServerContext(false, Optional.empty());
		}
		return new ServerContext(false, Optional.of(ServerPolicy.normalize(server.ip)));
	}
}
