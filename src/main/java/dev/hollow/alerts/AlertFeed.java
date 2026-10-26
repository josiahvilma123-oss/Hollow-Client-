package dev.hollow.alerts;

import dev.hollow.core.util.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Rate-limits alerts per cooldown key and manages the visible toast stack.
 * Repeated alerts inside the cooldown are folded into the existing toast as a count
 * instead of spamming new notifications.
 */
public final class AlertFeed {
	private final Clock clock;
	private final Map<String, Long> lastFired = new HashMap<>();
	private final List<Toast> toasts = new ArrayList<>();
	private long lifetimeMillis = 4000;
	private int maxVisible = 4;

	public AlertFeed(Clock clock) {
		this.clock = clock;
	}

	public void configure(long lifetimeMillis, int maxVisible) {
		this.lifetimeMillis = Math.max(500, lifetimeMillis);
		this.maxVisible = Math.max(1, maxVisible);
	}

	public long lifetimeMillis() {
		return lifetimeMillis;
	}

	/**
	 * Offers an alert. Returns true when a new toast was shown (callers play the sound only then).
	 * Alerts inside the cooldown window merge into a visible toast with the same key, if any.
	 */
	public boolean offer(Alert alert, long cooldownMillis) {
		long now = clock.millis();
		Long last = lastFired.get(alert.cooldownKey());
		if (last != null && now - last < cooldownMillis) {
			for (Toast toast : toasts) {
				if (toast.alert.cooldownKey().equals(alert.cooldownKey()) && !toast.expiring(now, lifetimeMillis)) {
					toast.merge(alert);
					return false;
				}
			}
			return false;
		}
		lastFired.put(alert.cooldownKey(), now);
		toasts.add(0, new Toast(alert, now));
		while (toasts.size() > maxVisible) {
			toasts.remove(toasts.size() - 1);
		}
		return true;
	}

	/** Drops expired toasts and returns the ones still on screen, newest first. */
	public List<Toast> visible() {
		long now = clock.millis();
		Iterator<Toast> iterator = toasts.iterator();
		while (iterator.hasNext()) {
			if (now - iterator.next().createdAt >= lifetimeMillis) {
				iterator.remove();
			}
		}
		return List.copyOf(toasts);
	}

	public void clear() {
		toasts.clear();
		lastFired.clear();
	}

	public long now() {
		return clock.millis();
	}

	public static final class Toast {
		private Alert alert;
		private final long createdAt;
		private int count = 1;

		private Toast(Alert alert, long createdAt) {
			this.alert = alert;
			this.createdAt = createdAt;
		}

		private void merge(Alert newer) {
			count++;
			// Keep the closest distance so the toast stays useful.
			if (newer.hasDistance() && (!alert.hasDistance() || newer.distance() < alert.distance())) {
				alert = newer;
			}
		}

		private boolean expiring(long now, long lifetime) {
			return now - createdAt > lifetime * 3 / 4;
		}

		public Alert alert() {
			return alert;
		}

		public int count() {
			return count;
		}

		public long createdAt() {
			return createdAt;
		}

		/** Age of the toast in milliseconds. */
		public long age(long now) {
			return now - createdAt;
		}
	}
}
