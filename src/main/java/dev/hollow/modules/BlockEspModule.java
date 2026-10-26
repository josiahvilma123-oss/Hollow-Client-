package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.render.BoxStyle;
import dev.hollow.render.TracerOrigin;

/** How Block Finder results are drawn. Disable it to keep Block Finder as alerts-only. */
public final class BlockEspModule extends Module {
	public final EnumSetting<BoxStyle> style = add("Boxes", new EnumSetting<>("style", "Box Style", "Outline, filled or both.", BoxStyle.BOTH));
	public final NumberSetting lineWidth = add("Boxes", new NumberSetting("line_width", "Line Width", "Outline thickness in pixels.", 2, 1, 6, 0.5).unit("px"));
	public final NumberSetting fillOpacity = add("Boxes", new NumberSetting("fill_opacity", "Fill Opacity", "Opacity of filled boxes.", 0.16, 0.02, 0.6, 0.02));
	public final BoolSetting seeThrough = add("Boxes", new BoolSetting("see_through", "Show Through Walls",
		"Draw boxes on top of terrain. Off: only blocks in direct view are outlined.", true));
	public final BoolSetting fade = add("Boxes", new BoolSetting("fade", "Fade With Distance", "Far boxes are drawn fainter.", true));

	public final BoolSetting tracers = add("Tracers", new BoolSetting("tracers", "Tracers",
		"Master switch for block tracers. Each category also has its own tracer toggle.", true));
	public final EnumSetting<TracerOrigin> tracerOrigin = add("Tracers", new EnumSetting<>("tracer_origin", "Origin", "Where tracer lines start.", TracerOrigin.CROSSHAIR));
	public final NumberSetting tracerWidth = add("Tracers", new NumberSetting("tracer_width", "Tracer Width", "", 1.5, 1, 5, 0.5).unit("px"));
	public final NumberSetting tracerOpacity = add("Tracers", new NumberSetting("tracer_opacity", "Tracer Opacity", "", 0.7, 0.1, 1, 0.05));

	public final BoolSetting labels = add("Labels", new BoolSetting("labels", "Labels", "Show the block name above matches.", true));
	public final BoolSetting distance = add("Labels", new BoolSetting("distance", "Distance", "Append the distance to labels.", true));
	public final NumberSetting labelScale = add("Labels", new NumberSetting("label_scale", "Label Scale", "", 1.0, 0.5, 1.5, 0.05).unit("x"));
	public final NumberSetting maxLabels = add("Labels", new NumberSetting("max_labels", "Max Labels", "Only the nearest matches get a label.", 24, 0, 100, 1));

	public final BoolSetting merge = add("Clutter", new BoolSetting("merge", "Merge Veins",
		"Combine touching blocks of the same kind into one box with a count, e.g. Diamond Ore ×6.", true));
	public final NumberSetting maxBoxes = add("Clutter", new NumberSetting("max_boxes", "Max Boxes",
		"Draw at most this many boxes, nearest first.", 300, 20, 2000, 10));
	public final NumberSetting perCategory = add("Clutter", new NumberSetting("per_category", "Max Per Category",
		"Limit how many matches each category may show.", 64, 1, 500, 1));

	public BlockEspModule() {
		super("block_esp", "Block ESP", "Boxes, tracers and labels for Block Finder results.", Category.VISUALS_DETECTION, true);
		BooleanVisibility.when(tracers, tracerOrigin, tracerWidth, tracerOpacity);
		BooleanVisibility.when(labels, distance, labelScale, maxLabels);
	}
}
