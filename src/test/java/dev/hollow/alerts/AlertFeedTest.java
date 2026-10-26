package dev.hollow.alerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AlertFeedTest {
	@Test
	void formatsLikeTheHollowSpec() {
		Alert block = new Alert(AlertSource.BLOCKS, "Amethyst Cluster", "Detected", 12.42, "block:amethyst");
		Alert mob = new Alert(AlertSource.HOSTILE, "Hostile Mob", "Nearby", 18.2, "entity:hostile");
		assertEquals("HOLO ALERT — Amethyst Cluster Detected · 12.4m", block.fullText());
		assertEquals("HOLO ALERT — Hostile Mob Nearby · 18.2m", mob.fullText());
		Alert noDistance = new Alert(AlertSource.BLOCKS, "Ancient Debris", "Detected", Double.NaN, "k");
		assertEquals("Ancient Debris Detected", noDistance.body());
	}

	@Test
	void cooldownSuppressesAndMergesRepeats() {
		AtomicLong now = new AtomicLong(0);
		AlertFeed feed = new AlertFeed(now::get);
		Alert far = new Alert(AlertSource.HOSTILE, "Hostile Mob", "Nearby", 20, "entity:hostile");
		Alert near = new Alert(AlertSource.HOSTILE, "Hostile Mob", "Nearby", 9, "entity:hostile");
		assertTrue(feed.offer(far, 5000));
		now.set(1000);
		assertFalse(feed.offer(near, 5000));
		assertEquals(1, feed.visible().size());
		AlertFeed.Toast toast = feed.visible().get(0);
		assertEquals(2, toast.count());
		assertEquals(9, toast.alert().distance());
		now.set(6000);
		assertTrue(feed.offer(far, 5000));
	}

	@Test
	void differentKeysDoNotShareCooldowns() {
		AlertFeed feed = new AlertFeed(() -> 0);
		assertTrue(feed.offer(new Alert(AlertSource.BLOCKS, "Diamond Ore", "Detected", 5, "block:diamond"), 10000));
		assertTrue(feed.offer(new Alert(AlertSource.BLOCKS, "Ancient Debris", "Detected", 5, "block:debris"), 10000));
		assertEquals(2, feed.visible().size());
	}

	@Test
	void toastsExpireAndStackIsCapped() {
		AtomicLong now = new AtomicLong(0);
		AlertFeed feed = new AlertFeed(now::get);
		feed.configure(2000, 2);
		for (int i = 0; i < 4; i++) {
			feed.offer(new Alert(AlertSource.BLOCKS, "B" + i, "Detected", 1, "k" + i), 0);
		}
		assertEquals(2, feed.visible().size());
		assertEquals("B3", feed.visible().get(0).alert().subject());
		now.set(2500);
		assertTrue(feed.visible().isEmpty());
	}
}
