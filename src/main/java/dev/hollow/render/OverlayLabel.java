package dev.hollow.render;

/** A label anchored to a world position, drawn later in screen space by the HUD. */
public record OverlayLabel(double x, double y, double z, String title, String detail, int color, double distance, float scale) {
}
