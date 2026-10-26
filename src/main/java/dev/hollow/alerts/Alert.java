package dev.hollow.alerts;

import java.util.Locale;
import java.util.Objects;

/**
 * One detection event, e.g. "Amethyst Cluster Detected · 12.4m".
 *
 * @param subject     what was detected ("Amethyst Cluster", "Hostile Mob")
 * @param verb        "Detected", "Nearby", ...
 * @param distance    distance in blocks, or NaN when unknown
 * @param cooldownKey alerts sharing a key are rate-limited and merged together
 * @param detail      optional extra context such as the mob's name; empty when not needed
 */
public record Alert(AlertSource source, String subject, String verb, double distance, String cooldownKey, String detail) {
	public static final String PREFIX = "HOLO ALERT";

	public Alert {
		Objects.requireNonNull(source);
		Objects.requireNonNull(subject);
		Objects.requireNonNull(verb);
		Objects.requireNonNull(cooldownKey);
		detail = detail == null ? "" : detail;
	}

	public Alert(AlertSource source, String subject, String verb, double distance, String cooldownKey) {
		this(source, subject, verb, distance, cooldownKey, "");
	}

	public boolean hasDistance() {
		return !Double.isNaN(distance) && !Double.isInfinite(distance);
	}

	/** "Amethyst Cluster Detected · 12.4m" */
	public String body() {
		String text = subject + " " + verb;
		return hasDistance() ? text + " · " + formatDistance(distance) : text;
	}

	/** "HOLO ALERT — Amethyst Cluster Detected · 12.4m" */
	public String fullText() {
		return PREFIX + " — " + body();
	}

	public static String formatDistance(double distance) {
		return String.format(Locale.ROOT, "%.1fm", Math.max(0, distance));
	}
}
