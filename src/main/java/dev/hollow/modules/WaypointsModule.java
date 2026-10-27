package dev.hollow.modules;

import dev.hollow.core.input.Keybind;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.ActionBarSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.TextSetting;
import dev.hollow.core.waypoints.Waypoint;
import dev.hollow.exploration.Waypoints;
import java.util.regex.Pattern;

/**
 * Base Locator and waypoints: save and label your own bases and points of interest per world,
 * see them as beacons with distances and on a compass, keep notes, and record a scouting trail.
 */
public final class WaypointsModule extends Module implements Tickable {
	private static final Pattern COORDINATES = Pattern.compile("-?\\d{1,8}[ ,]+-?\\d{1,4}[ ,]+-?\\d{1,8}");

	public final TextSetting formName = add("New Waypoint", new TextSetting("form_name", "Name", "", "", Waypoint.MAX_NAME, s -> true,
		"Name (optional)").notSaved());
	public final TextSetting formCoordinates = add("New Waypoint", new TextSetting("form_coordinates", "Coordinates",
		"X Y Z, e.g. 120 64 -340. Leave empty to use your position.", "", 32, COORDINATES.asMatchPredicate(), "x y z"));
	public final EnumSetting<Waypoint.Category> formCategory = add("New Waypoint", new EnumSetting<>("form_category", "Category", "",
		Waypoint.Category.BASE));
	public final TextSetting formNote = add("New Waypoint", new TextSetting("form_note", "Note", "", "", Waypoint.MAX_NOTE, s -> true,
		"Note (optional)").notSaved());
	public final ActionBarSetting formActions = add("New Waypoint", new ActionBarSetting("form_actions", "Save",
		"Add creates a new waypoint; Update changes the one selected in the list below.",
		new ActionBarSetting.Button("Add", () -> Waypoints.INSTANCE.addFromForm(false)),
		new ActionBarSetting.Button("Add at my position", () -> Waypoints.INSTANCE.addFromForm(true)),
		new ActionBarSetting.Button("Update selected", Waypoints.INSTANCE::updateSelectedFromForm),
		new ActionBarSetting.Button("Clear form", Waypoints.INSTANCE::clearForm)));

	public final ActionBarSetting share = add("Saved Waypoints", new ActionBarSetting("share", "Share",
		"Export copies this world's waypoints to the clipboard; Import adds waypoints from the clipboard.",
		new ActionBarSetting.Button("Export", Waypoints.INSTANCE::exportToClipboard),
		new ActionBarSetting.Button("Import", Waypoints.INSTANCE::importFromClipboard),
		new ActionBarSetting.Button("Open folder", Waypoints.INSTANCE::openFolder)));

	public final KeySetting quickAdd = add("Controls", new KeySetting("quick_add", "Add Waypoint Here",
		"Saves your current position as a Scouted waypoint.", Keybind.NONE));

	public final BoolSetting beams = add("Display", new BoolSetting("beams", "Beacons", "A vertical beam at each waypoint.", true));
	public final BoolSetting labels = add("Display", new BoolSetting("labels", "Names & Distances", "", true));
	public final NumberSetting labelRange = add("Display", new NumberSetting("label_range", "Label Range",
		"Labels and beams show for waypoints closer than this.", 2000, 50, 30000, 50).unit(" blocks"));
	public final BoolSetting compass = add("Display", new BoolSetting("compass", "Compass HUD",
		"A heading strip with waypoint markers (move it in the HUD editor).", true));

	public final BoolSetting deathPoints = add("Automatic", new BoolSetting("death_points", "Death Waypoints",
		"Save a waypoint where you die.", true));
	public final BoolSetting trail = add("Automatic", new BoolSetting("trail", "Scouting Trail",
		"Record the path you travel as a breadcrumb line (this session only).", false));
	public final NumberSetting trailSpacing = add("Automatic", new NumberSetting("trail_spacing", "Trail Spacing",
		"Distance between breadcrumbs.", 8, 2, 64, 1).unit(" blocks"));
	public final NumberSetting trailLength = add("Automatic", new NumberSetting("trail_length", "Trail Length",
		"Oldest breadcrumbs are dropped beyond this many.", 400, 50, 2000, 50));

	public WaypointsModule() {
		super("waypoints", "Waypoints & Bases", "Save, label and find your bases and points of interest, with beacons, a compass, "
			+ "notes and a scouting trail.", Category.EXPLORATION, true);
		trailSpacing.visibleWhen(trail::get);
		trailLength.visibleWhen(trail::get);
		labelRange.visibleWhen(() -> beams.get() || labels.get());
	}

	@Override
	public void tick() {
		Waypoints.INSTANCE.tick(this);
	}

	@Override
	protected void onDisable() {
		Waypoints.INSTANCE.clearTrail();
	}
}
