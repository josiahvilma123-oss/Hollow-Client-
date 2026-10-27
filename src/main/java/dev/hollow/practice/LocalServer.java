package dev.hollow.practice;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

/**
 * Practice tools act on the built-in server of a single-player world (the player's own world),
 * never on a remote server.
 */
public final class LocalServer {
	private LocalServer() {
	}

	public static boolean available() {
		return Minecraft.getInstance().getSingleplayerServer() != null && Minecraft.getInstance().player != null;
	}

	/** Runs commands as the local player (at their position), with full permission, without chat output. */
	public static boolean runAsPlayer(String... commands) {
		return withPlayer(player -> {
			CommandSourceStack source = player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.OWNER).withSuppressedOutput();
			for (String command : commands) {
				player.level().getServer().getCommands().performPrefixedCommand(source, command);
			}
		});
	}

	/** Runs work on the server thread with the server-side copy of the local player. */
	public static boolean withPlayer(Consumer<ServerPlayer> work) {
		Minecraft minecraft = Minecraft.getInstance();
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server == null || minecraft.player == null) {
			return false;
		}
		UUID id = minecraft.player.getUUID();
		server.execute(() -> {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				work.accept(player);
			}
		});
		return true;
	}
}
