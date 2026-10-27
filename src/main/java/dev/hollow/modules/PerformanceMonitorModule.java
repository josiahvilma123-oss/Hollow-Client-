package dev.hollow.modules;

import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.NumberSetting;

/** A small HUD panel with FPS, frame times, memory and entity counts. Position and scale via the HUD editor. */
public final class PerformanceMonitorModule extends Module {
	public final BoolSetting fps = add("Metrics", new BoolSetting("fps", "Current FPS", "", true));
	public final BoolSetting average = add("Metrics", new BoolSetting("average", "Average FPS", "", true));
	public final NumberSetting averageSeconds = add("Metrics", new NumberSetting("average_seconds", "Average Over", "", 5, 1, 30, 1).unit(" s"));
	public final BoolSetting frameTime = add("Metrics", new BoolSetting("frame_time", "Frame Time", "Average and worst frame in the interval.", true));
	public final BoolSetting memory = add("Metrics", new BoolSetting("memory", "Memory", "Java heap in use / maximum.", true));
	public final BoolSetting entities = add("Metrics", new BoolSetting("entities", "Entities", "Loaded entities and particles.", false));
	public final BoolSetting preset = add("Metrics", new BoolSetting("preset", "Preset & Adaptive Level", "", true));
	public final BoolSetting warnings = add("Warnings", new BoolSetting("warnings", "Warnings",
		"Highlight low FPS, long frames (stutter) and high memory use.", true));
	public final NumberSetting warnFps = add("Warnings", new NumberSetting("warn_fps", "Warn Below", "", 30, 10, 144, 1).unit(" FPS"));

	public PerformanceMonitorModule() {
		super("performance_monitor", "Performance Monitor", "FPS, average FPS, frame time, memory and entity counts on your HUD.",
			Category.HUD, false);
		averageSeconds.visibleWhen(average::get);
		warnFps.visibleWhen(warnings::get);
	}
}
