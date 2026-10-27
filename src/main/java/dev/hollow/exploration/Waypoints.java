package dev.hollow.exploration;

import dev.hollow.Hollow;
import dev.hollow.core.waypoints.Waypoint;
import dev.hollow.core.waypoints.WaypointStore;
import dev.hollow.core.util.Colors;
import dev.hollow.alerts.Alert;
import dev.hollow.input.KeyInput;
import dev.hollow.modules.WaypointsModule;
import dev.hollow.policy.ServerContext;
import dev.hollow.render.OverlayLabel;
import dev.hollow.render.OverlayProvider;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Runtime of {@link WaypointsModule}: per-world storage, death points, the trail and world rendering. */
public final class Waypoints implements OverlayProvider {
	public static final Waypoints INSTANCE = new Waypoints();

	private WaypointStore store;
	private final Deque<Vec3> trail = new ArrayDeque<>();
	private String trailDimension = "";
	private boolean wasDead;
	private boolean quickAddWasDown;
	private String selectedId;
	private String message = "";

	private Waypoints() {
	}

	public void init(WaypointStore store) {
		this.store = store;
	}

	public WaypointStore store() {
		return store;
	}

	public String message() {
		return message;
	}

	public String selectedId() {
		return selectedId;
	}

	public void select(String id) {
		selectedId = id;
		store.all().stream().filter(w -> w.id().equals(id)).findFirst().ifPresent(w -> {
			WaypointsModule m = module();
			m.formName.set(w.name());
			m.formCoordinates.set(w.x() + " " + w.y() + " " + w.z());
			m.formCategory.set(w.category());
			m.formNote.set(w.note());
		});
	}

	private static WaypointsModule module() {
		return Hollow.module(WaypointsModule.class);
	}

	/** Key identifying the current world: the single-player save name or the server address. */
	public static String worldKey() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return "";
		}
		MinecraftServer server = minecraft.getSingleplayerServer();
		if (server != null) {
			return "sp_" + server.getWorldData().getLevelName();
		}
		return "mp_" + ServerContext.current().address().orElse("unknown");
	}

	public static String dimension() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level == null ? "" : minecraft.level.dimension().identifier().toString();
	}

	/** Called when joining or leaving a world. */
	public void onWorldChanged() {
		if (store != null) {
			store.open(worldKey());
		}
		clearTrail();
		selectedId = null;
	}

	public void clearTrail() {
		trail.clear();
	}

	public Deque<Vec3> trail() {
		return trail;
	}

	public void tick(WaypointsModule m) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || store == null) {
			return;
		}
		if (!store.world().equals(worldKey())) {
			onWorldChanged();
		}
		if (!m.isEnabled()) {
			return;
		}
		boolean dead = player.isDeadOrDying();
		if (dead && !wasDead && m.deathPoints.get()) {
			add(store.nextName("Death"), player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ(),
				Waypoint.Category.DEATH, "");
		}
		wasDead = dead;
		boolean quick = KeyInput.gameplayInputActive() && KeyInput.isDown(m.quickAdd.get());
		if (quick && !quickAddWasDown) {
			Waypoint added = add(store.nextName("Scouted"), player.blockPosition().getX(), player.blockPosition().getY(),
				player.blockPosition().getZ(), Waypoint.Category.SCOUTED, "");
			if (added != null) {
				player.displayClientMessage(Component.literal("[Holo] Saved " + added.name() + " at " + added.x() + " " + added.y() + " " + added.z()), true);
			}
		}
		quickAddWasDown = quick;
		if (m.trail.get()) {
			String dim = dimension();
			if (!dim.equals(trailDimension)) {
				trail.clear();
				trailDimension = dim;
			}
			Vec3 here = player.position();
			if (trail.isEmpty() || trail.peekLast().distanceTo(here) >= m.trailSpacing.get()) {
				trail.addLast(here);
				while (trail.size() > m.trailLength.getInt()) {
					trail.removeFirst();
				}
			}
		}
	}

	private Waypoint add(String name, int x, int y, int z, Waypoint.Category category, String note) {
		try {
			Waypoint waypoint = store.add(name, x, y, z, dimension(), category, note, System.currentTimeMillis());
			message = "Added " + waypoint.name();
			return waypoint;
		} catch (IllegalStateException e) {
			message = e.getMessage();
			return null;
		}
	}

	public void addFromForm(boolean atPlayer) {
		WaypointsModule m = module();
		LocalPlayer player = Minecraft.getInstance().player;
		int[] coords = atPlayer || m.formCoordinates.get().isBlank() ? playerCoords(player) : parse(m.formCoordinates.get());
		if (coords == null) {
			message = atPlayer || m.formCoordinates.get().isBlank() ? "Join a world first" : "Enter coordinates as X Y Z";
			return;
		}
		String name = m.formName.get().isBlank() ? store.nextName(m.formCategory.get().displayName()) : m.formName.get();
		Waypoint added = add(name, coords[0], coords[1], coords[2], m.formCategory.get(), m.formNote.get());
		if (added != null) {
			selectedId = added.id();
		}
	}

	public void updateSelectedFromForm() {
		WaypointsModule m = module();
		Waypoint current = store.all().stream().filter(w -> w.id().equals(selectedId)).findFirst().orElse(null);
		if (current == null) {
			message = "Select a waypoint in the list first";
			return;
		}
		int[] coords = m.formCoordinates.get().isBlank() ? new int[] {current.x(), current.y(), current.z()} : parse(m.formCoordinates.get());
		if (coords == null) {
			message = "Enter coordinates as X Y Z";
			return;
		}
		Waypoint updated = new Waypoint(current.id(), m.formName.get().isBlank() ? current.name() : m.formName.get(), coords[0], coords[1],
			coords[2], current.dimension(), m.formCategory.get(), m.formCategory.get() == current.category() ? current.color() : 0,
			m.formNote.get(), current.created(), current.visible());
		store.update(updated);
		message = "Updated " + updated.name();
	}

	public void clearForm() {
		WaypointsModule m = module();
		m.formName.set("");
		m.formCoordinates.set("");
		m.formNote.set("");
		selectedId = null;
		message = "";
	}

	public void delete(String id) {
		if (store.remove(id)) {
			message = "Deleted waypoint";
			if (id.equals(selectedId)) {
				selectedId = null;
			}
		}
	}

	public void toggleVisible(Waypoint waypoint) {
		store.update(waypoint.withVisible(!waypoint.visible()));
	}

	private static int[] playerCoords(LocalPlayer player) {
		return player == null ? null : new int[] {player.blockPosition().getX(), player.blockPosition().getY(), player.blockPosition().getZ()};
	}

	static int[] parse(String text) {
		String[] parts = text.trim().split("[ ,]+");
		if (parts.length != 3) {
			return null;
		}
		try {
			return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])};
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public void exportToClipboard() {
		if (store.world().isEmpty()) {
			message = "Join a world first";
			return;
		}
		Minecraft.getInstance().keyboardHandler.setClipboard(store.export());
		message = "Copied " + store.all().size() + " waypoints to the clipboard";
	}

	public void importFromClipboard() {
		try {
			int added = store.importJson(Minecraft.getInstance().keyboardHandler.getClipboard());
			message = added == 0 ? "Nothing new to import" : "Imported " + added + " waypoints";
		} catch (RuntimeException e) {
			message = e.getMessage();
		}
	}

	public void openFolder() {
		try {
			Files.createDirectories(store.directory());
			Util.getPlatform().openPath(store.directory());
		} catch (Exception e) {
			message = "Could not open folder: " + e.getMessage();
		}
	}

	@Override
	public void collect(Sink sink, float partialTick) {
		if (store == null || Hollow.modules() == null) {
			return;
		}
		WaypointsModule m = module();
		LocalPlayer player = Minecraft.getInstance().player;
		if (!m.isEnabled() || player == null) {
			return;
		}
		String dim = dimension();
		Vec3 eye = player.getEyePosition(partialTick);
		double range = m.labelRange.get();
		for (Waypoint waypoint : store.all()) {
			if (!waypoint.visible() || !waypoint.dimension().equals(dim)) {
				continue;
			}
			double distance = waypoint.distanceTo(eye.x, eye.y, eye.z);
			if (distance > range) {
				continue;
			}
			int color = waypoint.color();
			if (m.beams.get()) {
				double cx = waypoint.x() + 0.5;
				double cz = waypoint.z() + 0.5;
				sink.line(new Vec3(cx, waypoint.y(), cz), new Vec3(cx, waypoint.y() + 96, cz), Colors.withAlpha(color, 170), 2f);
				if (distance < 64) {
					sink.outline(new AABB(waypoint.x(), waypoint.y(), waypoint.z(), waypoint.x() + 1, waypoint.y() + 1, waypoint.z() + 1),
						Colors.withAlpha(color, 200), 1.5f, true);
				}
			}
			if (m.labels.get()) {
				sink.label(new OverlayLabel(waypoint.x() + 0.5, waypoint.y() + 1.6, waypoint.z() + 0.5, waypoint.name(),
					Alert.formatDistance(distance), color, distance, distance > 200 ? 0.9f : 1f));
			}
		}
		if (m.trail.get() && trail.size() > 1) {
			Vec3 previous = null;
			int alphaStep = Math.max(1, trail.size() / 8);
			int index = 0;
			for (Vec3 point : trail) {
				if (previous != null) {
					int alpha = 60 + Math.min(160, index / alphaStep * 20);
					sink.line(previous.add(0, 0.1, 0), point.add(0, 0.1, 0), Colors.withAlpha(0xFF5FC8FF, alpha), 2f);
				}
				previous = point;
				index++;
			}
		}
	}

	/** Nearest visible waypoint in this dimension, for the compass. */
	public Waypoint nearest(Vec3 from) {
		if (store == null) {
			return null;
		}
		String dim = dimension();
		Waypoint best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Waypoint waypoint : store.all()) {
			if (waypoint.visible() && waypoint.dimension().equals(dim)) {
				double d = waypoint.distanceTo(from.x, from.y, from.z);
				if (d < bestDistance) {
					bestDistance = d;
					best = waypoint;
				}
			}
		}
		return best;
	}

	public static String coords(Waypoint waypoint) {
		return String.format(Locale.ROOT, "%d %d %d", waypoint.x(), waypoint.y(), waypoint.z());
	}
}
