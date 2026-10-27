package dev.hollow.exploration;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.modules.EntityVisibilityModule;
import dev.hollow.modules.ServerRulesModule;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/** Compares successive snapshots of the tab list and loaded player entities and logs observable changes. */
public final class VisibilityLog {
	public static final VisibilityLog INSTANCE = new VisibilityLog();
	private static final int MAX_EVENTS = 60;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	public record Event(String time, String text) {
	}

	private record Seen(String name, double distance, boolean invisible) {
	}

	private final Deque<Event> events = new ArrayDeque<>();
	private Map<UUID, PlayerInfo> listed = null;
	private final Map<UUID, GameType> modes = new HashMap<>();
	private final Map<UUID, Seen> loaded = new HashMap<>();
	private int tickCounter;

	private VisibilityLog() {
	}

	public List<Event> events() {
		return new ArrayList<>(events);
	}

	public void clear() {
		events.clear();
	}

	/** Drops snapshots (e.g. on disconnect) so the next world starts fresh, without false "left" events. */
	public void forget() {
		listed = null;
		modes.clear();
		loaded.clear();
	}

	public void tick(EntityVisibilityModule m) {
		if (++tickCounter % 5 != 0) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer self = minecraft.player;
		ClientPacketListener connection = minecraft.getConnection();
		if (self == null || connection == null || minecraft.level == null) {
			forget();
			return;
		}
		Map<UUID, PlayerInfo> nowListed = new HashMap<>();
		for (PlayerInfo info : connection.getOnlinePlayers()) {
			nowListed.put(info.getProfile().id(), info);
		}
		if (listed != null) {
			for (Map.Entry<UUID, PlayerInfo> entry : nowListed.entrySet()) {
				UUID id = entry.getKey();
				String name = entry.getValue().getProfile().name();
				if (!listed.containsKey(id) && m.tabList.get()) {
					log(m, name + " joined the tab list");
				}
				GameType mode = entry.getValue().getGameMode();
				GameType before = modes.get(id);
				if (before != null && mode != before && m.gameModes.get()) {
					log(m, name + " switched to " + mode.getShortDisplayName().getString());
				}
			}
			for (Map.Entry<UUID, PlayerInfo> entry : listed.entrySet()) {
				if (!nowListed.containsKey(entry.getKey()) && m.tabList.get()) {
					log(m, entry.getValue().getProfile().name() + " left the tab list");
				}
			}
		}
		modes.clear();
		nowListed.forEach((id, info) -> modes.put(id, info.getGameMode()));

		boolean entitiesAllowed = Hollow.module(ServerRulesModule.class).playersAllowed();
		Map<UUID, Seen> nowLoaded = new HashMap<>();
		if (entitiesAllowed) {
			for (Entity entity : minecraft.level.entitiesForRendering()) {
				if (entity instanceof Player player && player != self) {
					nowLoaded.put(player.getUUID(), new Seen(player.getName().getString(), player.distanceTo(self), player.isInvisible()));
				}
			}
			if (listed != null) {
				for (Map.Entry<UUID, Seen> entry : loaded.entrySet()) {
					Seen before = entry.getValue();
					Seen now = nowLoaded.get(entry.getKey());
					if (now == null && m.disappearances.get() && nowListed.containsKey(entry.getKey()) && before.distance() <= m.nearby.get()) {
						log(m, String.format("%s disappeared %.0fm away but is still listed (vanish, teleport, death or dimension change)",
							before.name(), before.distance()));
					}
					if (now != null && m.invisibility.get() && now.invisible() != before.invisible()) {
						log(m, now.name() + (now.invisible() ? " became invisible" : " is visible again"));
					}
				}
			}
		}
		loaded.clear();
		loaded.putAll(nowLoaded);
		listed = nowListed;
	}

	private void log(EntityVisibilityModule m, String text) {
		events.addFirst(new Event(LocalTime.now().format(TIME), text));
		while (events.size() > MAX_EVENTS) {
			events.removeLast();
		}
		if (m.alerts.get() && Hollow.alerts() != null) {
			Hollow.alerts().report(new Alert(AlertSource.VISIBILITY, text, "", Double.NaN, "visibility:" + text));
		}
	}
}
