package dev.hollow.alerts;

import dev.hollow.core.util.Clock;
import dev.hollow.modules.HollowAlertsModule;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Single entry point for detection features to raise alerts. */
public final class AlertService {
	private final AlertFeed feed = new AlertFeed(Clock.SYSTEM);
	private final Supplier<HollowAlertsModule> module;

	public AlertService(Supplier<HollowAlertsModule> module) {
		this.module = module;
	}

	/** Raises an alert subject to the module's per-source switches and cooldowns. */
	public void report(Alert alert) {
		HollowAlertsModule settings = module.get();
		if (!settings.isEnabled()) {
			return;
		}
		HollowAlertsModule.SourceSettings source = settings.source(alert.source());
		if (!source.enabled.get()) {
			return;
		}
		configureFeed(settings);
		if (feed.offer(alert, Math.round(source.cooldown.get() * 1000))) {
			onShown(settings, alert, source.playSound.get());
		}
	}

	/** Shows an alert immediately, ignoring cooldowns. Used by the settings preview button. */
	public void preview(Alert alert) {
		HollowAlertsModule settings = module.get();
		configureFeed(settings);
		if (feed.offer(alert, 0)) {
			onShown(settings, alert, true);
		}
	}

	public List<AlertFeed.Toast> visible() {
		configureFeed(module.get());
		return feed.visible();
	}

	public long now() {
		return feed.now();
	}

	public long lifetimeMillis() {
		return feed.lifetimeMillis();
	}

	public void clear() {
		feed.clear();
	}

	private void configureFeed(HollowAlertsModule settings) {
		feed.configure(Math.round(settings.lifetime.get() * 1000), settings.maxVisible.getInt());
	}

	private void onShown(HollowAlertsModule settings, Alert alert, boolean sourceSound) {
		Minecraft minecraft = Minecraft.getInstance();
		if (settings.sound.get() && sourceSound) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound(settings.soundType.get()),
				settings.pitch.getFloat(), settings.volume.getFloat()));
		}
		if (settings.chat.get() && minecraft.player != null) {
			Component message = Component.literal(Alert.PREFIX + " — ")
				.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xB57BFF)).withBold(true))
				.append(Component.literal(alert.body()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xE6E0F0)).withBold(false)));
			minecraft.player.displayClientMessage(message, false);
		}
	}

	private static SoundEvent sound(AlertSound type) {
		return switch (type) {
			case CHIME -> SoundEvents.AMETHYST_BLOCK_CHIME;
			case BELL -> SoundEvents.NOTE_BLOCK_BELL.value();
			case SOUL -> SoundEvents.SOUL_ESCAPE.value();
			case PLING -> SoundEvents.NOTE_BLOCK_PLING.value();
			case ORB -> SoundEvents.EXPERIENCE_ORB_PICKUP;
		};
	}
}
