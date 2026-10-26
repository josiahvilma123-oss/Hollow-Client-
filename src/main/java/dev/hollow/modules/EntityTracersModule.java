package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.StringListSetting;
import dev.hollow.detection.EntityCategory;
import dev.hollow.detection.EntityFilter;
import dev.hollow.render.TracerOrigin;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class EntityTracersModule extends Module {
	private static final Pattern ENTITY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public final NumberSetting maxDistance = add("General", new NumberSetting("max_distance", "Max Distance",
		"Entities further than this are ignored.", 64, 8, 256, 4).unit("m"));

	private final Map<EntityCategory, CategorySettings> categories = new EnumMap<>(EntityCategory.class);

	public final BoolSetting lines = add("Lines", new BoolSetting("lines", "Tracer Lines", "Draw a line to each entity.", true));
	public final EnumSetting<TracerOrigin> origin = add("Lines", new EnumSetting<>("origin", "Origin", "Where lines start.", TracerOrigin.CROSSHAIR));
	public final EnumSetting<TargetPoint> target = add("Lines", new EnumSetting<>("target", "Target Point", "Which part of the entity lines point at.", TargetPoint.BODY));
	public final NumberSetting thickness = add("Lines", new NumberSetting("thickness", "Thickness", "", 1.5, 1, 6, 0.5).unit("px"));
	public final NumberSetting opacity = add("Lines", new NumberSetting("opacity", "Opacity", "", 0.75, 0.1, 1, 0.05));

	public final BoolSetting boxes = add("Boxes", new BoolSetting("boxes", "Bounding Boxes", "Outline each entity's hitbox.", false));
	public final BoolSetting boxFill = add("Boxes", new BoolSetting("box_fill", "Fill Boxes", "Add a faint fill inside boxes.", false));
	public final BoolSetting seeThrough = add("Boxes", new BoolSetting("see_through", "Show Through Walls", "Draw on top of terrain.", true));

	public final BoolSetting names = add("Labels", new BoolSetting("names", "Names", "Show entity names.", true));
	public final BoolSetting health = add("Labels", new BoolSetting("health", "Health", "Show health where the client knows it.", true));
	public final BoolSetting distance = add("Labels", new BoolSetting("distance", "Distance", "Show the distance.", true));
	public final NumberSetting labelScale = add("Labels", new NumberSetting("label_scale", "Label Scale", "", 1.0, 0.5, 1.5, 0.05).unit("x"));
	public final NumberSetting maxLabels = add("Labels", new NumberSetting("max_labels", "Max Labels", "Only the nearest entities get a label.", 16, 0, 64, 1));

	public final BoolSetting onlyListed = add("Custom Filter", new BoolSetting("only_listed", "Only Listed Types",
		"Show only the entity types in the list below.", false));
	public final StringListSetting listed = add("Custom Filter", new StringListSetting("listed", "Listed Types",
		"Entity ids for the custom filter, e.g. minecraft:armor_stand.", List.of(), ENTITY_ID.asMatchPredicate(), "minecraft:entity_id"));
	public final StringListSetting hidden = add("Custom Filter", new StringListSetting("hidden", "Hidden Types",
		"Entity ids that are never shown.", List.of(), ENTITY_ID.asMatchPredicate(), "minecraft:entity_id"));

	public final NumberSetting alertRadius = add("Alerts", new NumberSetting("alert_radius", "Alert Radius",
		"Entities entering this radius trigger an alert (if enabled for their category).", 24, 4, 128, 1).unit("m"));

	public EntityTracersModule() {
		super("entity_tracers", "Entity Tracers", "Lines, boxes and labels for mobs, animals and permitted players.",
			Category.VISUALS_DETECTION, false);
		for (EntityCategory category : EntityCategory.values()) {
			categories.put(category, new CategorySettings(category));
		}
		BooleanVisibility.when(lines, origin, target, thickness, opacity);
		BooleanVisibility.when(boxes, boxFill, seeThrough);
	}

	public CategorySettings category(EntityCategory category) {
		return categories.get(category);
	}

	public EntityFilter filter(boolean playersPermitted) {
		List<EntityCategory> enabled = new ArrayList<>();
		for (CategorySettings settings : categories.values()) {
			if (settings.show.get() && (settings.category != EntityCategory.PLAYERS || playersPermitted)) {
				enabled.add(settings.category);
			}
		}
		return new EntityFilter(enabled, onlyListed.get(), listed.get(), hidden.get());
	}

	public final class CategorySettings {
		public final EntityCategory category;
		public final BoolSetting show;
		public final ColorSetting color;
		public final BoolSetting alert;

		private CategorySettings(EntityCategory category) {
			this.category = category;
			String group = "Categories · " + category.displayName();
			String prefix = category.id() + ".";
			String showDescription = category == EntityCategory.PLAYERS
				? "Players are only shown in single-player, or on servers listed under Server Rules as permitting it."
				: category == EntityCategory.CUSTOM
					? "Show entities from the custom filter list that have no other category."
					: "Show " + category.displayName().toLowerCase(java.util.Locale.ROOT) + ".";
			this.show = add(group, new BoolSetting(prefix + "show", "Show " + category.displayName(), showDescription,
				category == EntityCategory.HOSTILE));
			this.color = add(group, new ColorSetting(prefix + "color", "Color", "", category.defaultColor()));
			this.alert = add(group, new BoolSetting(prefix + "alert", "Alert On Approach",
				"Notify when one enters the alert radius.", category == EntityCategory.HOSTILE));
			BooleanVisibility.when(show, color, alert);
		}
	}

	public enum TargetPoint implements EnumSetting.Displayable {
		FEET("Feet"),
		BODY("Body"),
		HEAD("Head");

		private final String displayName;

		TargetPoint(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public String displayName() {
			return displayName;
		}
	}
}
