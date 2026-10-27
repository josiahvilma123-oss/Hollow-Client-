package dev.hollow.hud;

import dev.hollow.Hollow;
import dev.hollow.bridge.BridgeAssist;
import dev.hollow.bridge.Bridging;
import dev.hollow.camera.Freecam;
import dev.hollow.combat.AimAssist;
import dev.hollow.combat.KillAura;
import dev.hollow.combat.KnockbackTester;
import dev.hollow.combat.ReachMeter;
import dev.hollow.core.module.Module;
import dev.hollow.core.util.Colors;
import dev.hollow.core.waypoints.Waypoint;
import dev.hollow.exploration.Waypoints;
import dev.hollow.modules.AimAssistModule;
import dev.hollow.modules.AutoBridgeModule;
import dev.hollow.modules.FreecamModule;
import dev.hollow.modules.KillAuraModule;
import dev.hollow.modules.KnockbackTesterModule;
import dev.hollow.modules.PerformanceModule;
import dev.hollow.modules.PerformanceMonitorModule;
import dev.hollow.modules.ReachDisplayModule;
import dev.hollow.modules.SpeedBridgeModule;
import dev.hollow.modules.TriggerBotModule;
import dev.hollow.modules.WaypointsModule;
import dev.hollow.perf.FpsBoost;
import dev.hollow.perf.PerformanceState;
import dev.hollow.render.Draw2D;
import dev.hollow.render.Theme;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** HUD elements of the new modules. Text is rebuilt at most a few times per second where it is costly. */
public final class ModuleHuds {
	private static final int GOOD = 0xFF5FD68A;
	private static final int BAD = 0xFFE0405E;
	private static final int WARN = 0xFFFFC857;

	private ModuleHuds() {
	}

	public static void registerAll() {
		HudElements.register(new AimAssistHud());
		HudElements.register(new CombatHud());
		HudElements.register(new ReachHud());
		HudElements.register(new KnockbackHud());
		HudElements.register(new BridgeHud());
		HudElements.register(new FreecamHud());
		HudElements.register(new CompassHud());
		HudElements.register(new PerformanceHud());
	}

	private static String offReason(Module module) {
		return module.name() + " is off";
	}

	private static int stateColor(AimAssist.State state) {
		return switch (state) {
			case ACTIVE -> GOOD;
			case BLOCKED -> BAD;
			case OFF -> Theme.TEXT_FAINT;
			default -> Theme.TEXT_DIM;
		};
	}

	static final class AimAssistHud extends HudElement {
		AimAssistHud() {
			super("aim_assist", "Aim Assist", 0.5f, 0.62f);
		}

		private static AimAssistModule m() {
			return Hollow.module(AimAssistModule.class);
		}

		@Override
		public int width() {
			return 120;
		}

		@Override
		public int height() {
			return m().practiceMode.get() ? 34 : 24;
		}

		@Override
		public boolean visible() {
			return m().isEnabled() && m().showIndicator.get();
		}

		@Override
		public String hiddenReason() {
			return m().isEnabled() ? "indicator off" : offReason(m());
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			Font font = HudStyle.font();
			AimAssist.State state = preview && !m().isEnabled() ? AimAssist.State.ACTIVE : AimAssist.INSTANCE.state();
			HudStyle.panel(g, width(), height());
			HudStyle.title(g, "AIM ASSIST", 5, 4);
			Draw2D.text(g, font, state.label(), width() - 5 - font.width(state.label()), 4, stateColor(state));
			String target = AimAssist.INSTANCE.target() != null ? AimAssist.INSTANCE.target().getDisplayName().getString()
				: preview ? "Zombie" : "—";
			Draw2D.text(g, font, Draw2D.ellipsize(font, "Target: " + target, width() - 10), 5, 14, Theme.TEXT_DIM);
			if (m().practiceMode.get()) {
				int swings = AimAssist.INSTANCE.practiceSwings();
				int hits = AimAssist.INSTANCE.practiceHits();
				String accuracy = swings == 0 ? "—" : Math.round(100.0 * hits / swings) + "%";
				Draw2D.text(g, font, "Hits " + hits + "/" + swings + "  " + accuracy, 5, 24, HudStyle.GOLD);
			}
		}
	}

	static final class CombatHud extends HudElement {
		CombatHud() {
			super("combat", "Combat", 0.5f, 0.70f);
		}

		@Override
		public int width() {
			return 120;
		}

		@Override
		public int height() {
			return 24;
		}

		@Override
		public boolean visible() {
			KillAuraModule aura = Hollow.module(KillAuraModule.class);
			return aura.isEnabled() && aura.showHud.get() || Hollow.module(TriggerBotModule.class).isEnabled();
		}

		@Override
		public String hiddenReason() {
			return "Kill Aura and Trigger Bot are off";
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			Font font = HudStyle.font();
			KillAuraModule aura = Hollow.module(KillAuraModule.class);
			TriggerBotModule trigger = Hollow.module(TriggerBotModule.class);
			HudStyle.panel(g, width(), height());
			String title = aura.isEnabled() ? "KILL AURA" : trigger.isEnabled() ? "TRIGGER BOT" : "COMBAT";
			HudStyle.title(g, title, 5, 4);
			boolean blocked = aura.isEnabled() && aura.blockedReason().isPresent() || trigger.isEnabled() && trigger.blockedReason().isPresent();
			String status = blocked ? "Blocked" : "Ready";
			Draw2D.text(g, font, status, width() - 5 - font.width(status), 4, blocked ? BAD : GOOD);
			String line = KillAura.INSTANCE.target() != null ? "Target: " + KillAura.INSTANCE.target().getDisplayName().getString()
				: "Attacks: " + (KillAura.INSTANCE.attacks() + trigger.attacks());
			Draw2D.text(g, font, Draw2D.ellipsize(font, line, width() - 10), 5, 14, Theme.TEXT_DIM);
		}
	}

	static final class ReachHud extends HudElement {
		ReachHud() {
			super("reach", "Reach Display", 0.5f, 0.56f);
		}

		@Override
		public int width() {
			return 120;
		}

		@Override
		public int height() {
			return 34;
		}

		@Override
		public boolean visible() {
			ReachDisplayModule m = Hollow.module(ReachDisplayModule.class);
			return m.isEnabled() && m.showHud.get();
		}

		@Override
		public String hiddenReason() {
			return offReason(Hollow.module(ReachDisplayModule.class));
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			Font font = HudStyle.font();
			LocalPlayer player = Minecraft.getInstance().player;
			HudStyle.panel(g, width(), height());
			HudStyle.title(g, "REACH", 5, 4);
			double distance = ReachMeter.INSTANCE.targetDistance();
			double reach = player == null ? 3.0 : ReachMeter.INSTANCE.targetIsEntity() ? player.entityInteractionRange() : player.blockInteractionRange();
			String value = Double.isNaN(distance) ? (preview ? "2.84 m" : "—") : String.format(Locale.ROOT, "%.2f m", distance);
			boolean inRange = !Double.isNaN(distance) && distance <= reach;
			Draw2D.text(g, font, value, width() - 5 - font.width(value), 4, Double.isNaN(distance) ? Theme.TEXT_DIM : inRange ? GOOD : BAD);
			String name = ReachMeter.INSTANCE.targetName().isEmpty() ? (preview ? "Zombie" : "Nothing in view") : ReachMeter.INSTANCE.targetName();
			Draw2D.text(g, font, Draw2D.ellipsize(font, name, width() - 10), 5, 14, Theme.TEXT_DIM);
			double last = ReachMeter.INSTANCE.lastHitDistance();
			String lastText = String.format(Locale.ROOT, "Reach %.1f · last hit %s", reach,
				Double.isNaN(last) ? "—" : String.format(Locale.ROOT, "%.2f", last));
			Draw2D.text(g, font, Draw2D.ellipsize(font, lastText, width() - 10), 5, 24, Theme.TEXT_FAINT);
		}
	}

	static final class KnockbackHud extends HudElement {
		KnockbackHud() {
			super("knockback", "Knockback Tester", 0f, 0.62f);
		}

		@Override
		public int width() {
			return 124;
		}

		@Override
		public int height() {
			return 34;
		}

		@Override
		public boolean visible() {
			KnockbackTesterModule m = Hollow.module(KnockbackTesterModule.class);
			return m.isEnabled() && m.showHud.get();
		}

		@Override
		public String hiddenReason() {
			return offReason(Hollow.module(KnockbackTesterModule.class));
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			Font font = HudStyle.font();
			KnockbackTester k = KnockbackTester.INSTANCE;
			HudStyle.panel(g, width(), height());
			HudStyle.title(g, "KNOCKBACK", 5, 4);
			String count = k.samples() + " hits";
			Draw2D.text(g, font, count, width() - 5 - font.width(count), 4, Theme.TEXT_FAINT);
			HudStyle.line(g, "Speed", Double.isNaN(k.horizontalSpeed()) ? "—"
				: String.format(Locale.ROOT, "%.1f / %.1f b/s", k.horizontalSpeed(), k.verticalSpeed()), 5, 14, width() - 10, Theme.TEXT);
			HudStyle.line(g, "Distance", Double.isNaN(k.distance()) ? "—" : String.format(Locale.ROOT, "%.2f blocks", k.distance()), 5, 24,
				width() - 10, Theme.TEXT);
		}
	}

	static final class BridgeHud extends HudElement {
		BridgeHud() {
			super("bridge", "Bridging", 0.5f, 0.78f);
		}

		@Override
		public int width() {
			return 124;
		}

		@Override
		public int height() {
			return Hollow.module(SpeedBridgeModule.class).isEnabled()
				&& Hollow.module(SpeedBridgeModule.class).preset.get() == SpeedBridgeModule.Preset.PRACTICE ? 44 : 30;
		}

		@Override
		public boolean visible() {
			AutoBridgeModule auto = Hollow.module(AutoBridgeModule.class);
			SpeedBridgeModule speed = Hollow.module(SpeedBridgeModule.class);
			return auto.isEnabled() && auto.showHud.get() || speed.isEnabled() && speed.showHud.get();
		}

		@Override
		public String hiddenReason() {
			return "Auto Bridge and Speed Bridge are off";
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			Font font = HudStyle.font();
			LocalPlayer player = Minecraft.getInstance().player;
			AutoBridgeModule auto = Hollow.module(AutoBridgeModule.class);
			SpeedBridgeModule speed = Hollow.module(SpeedBridgeModule.class);
			HudStyle.panel(g, width(), height());
			ItemStack stack = ItemStack.EMPTY;
			int count = 0;
			if (player != null) {
				int slot = Bridging.hotbarBlockSlot(player.getInventory());
				if (slot >= 0) {
					stack = player.getInventory().getItem(slot);
					count = Bridging.count(player.getInventory(), stack.getItem());
				}
			}
			if (stack.isEmpty() && preview) {
				stack = new ItemStack(Items.COBBLESTONE);
				count = 128;
			}
			if (!stack.isEmpty()) {
				g.renderItem(stack, 5, 7);
			}
			String title = auto.isEnabled() ? "AUTO BRIDGE" : "SPEED BRIDGE";
			HudStyle.title(g, title, 25, 5);
			int countColor = count == 0 ? BAD : count < 16 ? WARN : Theme.TEXT;
			Draw2D.text(g, font, count + " blocks", 25, 16, countColor);
			String state;
			int stateColor;
			if ((auto.isEnabled() ? auto.blockedReason() : speed.blockedReason()).isPresent()) {
				state = "Blocked";
				stateColor = BAD;
			} else if (speed.isEnabled() && BridgeAssist.INSTANCE.cue()) {
				state = "PLACE";
				stateColor = HudStyle.GOLD;
			} else {
				state = auto.isEnabled() ? BridgeAssist.INSTANCE.lastResult().label() : speed.preset.get().displayName();
				stateColor = Theme.TEXT_DIM;
			}
			Draw2D.text(g, font, Draw2D.ellipsize(font, state, width() - 30 - font.width(count + " blocks") - 6),
				width() - 5 - Math.min(font.width(state), width() - 30 - font.width(count + " blocks") - 6), 16, stateColor);
			if (height() > 30) {
				String stats = String.format(Locale.ROOT, "%.1f b/s · best %.0f · falls %d", BridgeAssist.INSTANCE.blocksPerSecond(),
					BridgeAssist.INSTANCE.longestRun(), BridgeAssist.INSTANCE.falls());
				Draw2D.text(g, font, Draw2D.ellipsize(font, stats, width() - 10), 5, 31, Theme.TEXT_FAINT);
			}
		}
	}

	static final class FreecamHud extends HudElement {
		FreecamHud() {
			super("freecam", "Freecam", 0f, 0.4f);
		}

		@Override
		public int width() {
			return 130;
		}

		@Override
		public int height() {
			return 34;
		}

		@Override
		public boolean visible() {
			FreecamModule m = Hollow.module(FreecamModule.class);
			return m.isEnabled() && m.showHud.get();
		}

		@Override
		public String hiddenReason() {
			return offReason(Hollow.module(FreecamModule.class));
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			Font font = HudStyle.font();
			HudStyle.panel(g, width(), height());
			HudStyle.title(g, "FREECAM", 5, 4);
			String speed = String.format(Locale.ROOT, "%.1f b/s", Hollow.module(FreecamModule.class).speed.get());
			Draw2D.text(g, font, speed, width() - 5 - font.width(speed), 4, Theme.TEXT_FAINT);
			var pos = Freecam.INSTANCE.active() ? Freecam.INSTANCE.position() : new net.minecraft.world.phys.Vec3(120.5, 72, -340.5);
			Draw2D.text(g, font, String.format(Locale.ROOT, "%.1f  %.1f  %.1f", pos.x, pos.y, pos.z), 5, 14, Theme.TEXT);
			LocalPlayer player = Minecraft.getInstance().player;
			String away = player == null ? "Camera only; you stay put"
				: String.format(Locale.ROOT, "%.0f blocks from you", player.getEyePosition().distanceTo(pos));
			Draw2D.text(g, font, away, 5, 24, Theme.TEXT_FAINT);
		}
	}

	static final class CompassHud extends HudElement {
		private static final String[] HEADINGS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

		CompassHud() {
			super("compass", "Waypoint Compass", 0.5f, 0f);
		}

		@Override
		public int width() {
			return 200;
		}

		@Override
		public int height() {
			return 28;
		}

		/** The compass only appears once there is something to point at in this dimension. */
		private static boolean waypointsHere() {
			if (Waypoints.INSTANCE.store() == null) {
				return false;
			}
			String dimension = Waypoints.dimension();
			for (Waypoint waypoint : Waypoints.INSTANCE.store().all()) {
				if (waypoint.visible() && waypoint.dimension().equals(dimension)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean visible() {
			WaypointsModule m = Hollow.module(WaypointsModule.class);
			return m.isEnabled() && m.compass.get() && waypointsHere();
		}

		@Override
		public String hiddenReason() {
			WaypointsModule m = Hollow.module(WaypointsModule.class);
			return m.isEnabled() && m.compass.get() ? "Shows once this dimension has a visible waypoint" : offReason(m);
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			Font font = HudStyle.font();
			LocalPlayer player = Minecraft.getInstance().player;
			float yaw = player == null ? 180f : player.getYRot();
			int w = width();
			HudStyle.panel(g, w, height());
			int center = w / 2;
			float pixelsPerDegree = w / 180f;
			for (int i = 0; i < 8; i++) {
				float angle = dev.hollow.core.aim.AimMath.wrapDegrees(i * 45f - yaw);
				if (Math.abs(angle) > 88) {
					continue;
				}
				int x = Math.round(center + angle * pixelsPerDegree);
				String label = HEADINGS[i];
				Draw2D.text(g, font, label, x - font.width(label) / 2, 3, label.length() == 1 ? Theme.TEXT : Theme.TEXT_FAINT);
			}
			g.fill(center, 12, center + 1, 16, HudStyle.GOLD);
			if (player == null) {
				return;
			}
			for (Waypoint waypoint : Waypoints.INSTANCE.store() == null ? java.util.List.<Waypoint>of() : Waypoints.INSTANCE.store().all()) {
				if (!waypoint.visible() || !waypoint.dimension().equals(Waypoints.dimension())) {
					continue;
				}
				double dx = waypoint.x() + 0.5 - player.getX();
				double dz = waypoint.z() + 0.5 - player.getZ();
				float angle = dev.hollow.core.aim.AimMath.wrapDegrees(dev.hollow.core.aim.AimMath.yawTo(dx, dz) - yaw);
				float clamped = Math.max(-88, Math.min(88, angle));
				int x = Math.round(center + clamped * pixelsPerDegree);
				dev.hollow.menu.Pill.fill(g, x - 2, 13, 5, 5, Colors.withAlpha(waypoint.color(), Math.abs(angle) > 88 ? 120 : 255));
			}
			Waypoint nearest = Waypoints.INSTANCE.nearest(player.position());
			String text = nearest == null ? "No waypoints here" : nearest.name() + " · "
				+ dev.hollow.alerts.Alert.formatDistance(nearest.distanceTo(player.getX(), player.getY(), player.getZ()));
			Draw2D.text(g, font, Draw2D.ellipsize(font, text, w - 10), center - Math.min(font.width(text), w - 10) / 2, 18, Theme.TEXT_DIM);
		}
	}

	static final class PerformanceHud extends HudElement {
		private String[] lines = new String[0];
		private int[] colors = new int[0];
		private long builtAt;

		PerformanceHud() {
			super("performance", "Performance Monitor", 0f, 0f);
		}

		private static PerformanceMonitorModule m() {
			return Hollow.module(PerformanceMonitorModule.class);
		}

		@Override
		public int width() {
			return 124;
		}

		@Override
		public int height() {
			rebuild();
			return 6 + Math.max(1, lines.length) * 10;
		}

		@Override
		public boolean visible() {
			return m().isEnabled();
		}

		@Override
		public String hiddenReason() {
			return offReason(m());
		}

		/** Text is rebuilt four times a second; drawing reuses it. */
		private void rebuild() {
			long now = System.nanoTime();
			if (now - builtAt < 250_000_000L && lines.length > 0) {
				return;
			}
			builtAt = now;
			PerformanceMonitorModule m = m();
			var stats = PerformanceState.INSTANCE.stats();
			long window = (long) (m.averageSeconds.get() * 1e9);
			double fps = stats.fps(now, 1_000_000_000L);
			java.util.List<String> text = new java.util.ArrayList<>();
			java.util.List<Integer> tint = new java.util.ArrayList<>();
			boolean warn = m.warnings.get();
			if (m.fps.get()) {
				text.add(String.format(Locale.ROOT, "FPS %.0f", fps));
				tint.add(warn && fps < m.warnFps.get() ? BAD : GOOD);
			}
			if (m.average.get()) {
				text.add(String.format(Locale.ROOT, "Avg %.0f FPS (%ds)", stats.fps(now, window), m.averageSeconds.getInt()));
				tint.add(Theme.TEXT);
			}
			if (m.frameTime.get()) {
				double worst = stats.worstMillis(now, window);
				text.add(String.format(Locale.ROOT, "Frame %.1f ms · max %.0f", stats.averageMillis(now, window), worst));
				tint.add(warn && worst > 100 ? WARN : Theme.TEXT);
			}
			if (m.memory.get()) {
				Runtime runtime = Runtime.getRuntime();
				long used = (runtime.totalMemory() - runtime.freeMemory()) >> 20;
				long max = runtime.maxMemory() >> 20;
				text.add("Memory " + used + " / " + max + " MB");
				tint.add(warn && used > max * 0.9 ? WARN : Theme.TEXT);
			}
			if (m.entities.get()) {
				var level = Minecraft.getInstance().level;
				text.add("Entities " + (level == null ? 0 : level.getEntityCount()) + " · particles " + FpsBoost.particleCount());
				tint.add(Theme.TEXT_DIM);
			}
			if (m.preset.get()) {
				PerformanceModule perf = Hollow.module(PerformanceModule.class);
				String adaptive = perf.adaptive.get() ? " · adaptive " + PerformanceState.INSTANCE.effectLevel() : "";
				text.add(perf.preset.get().displayName() + adaptive);
				tint.add(HudStyle.GOLD);
			}
			lines = text.toArray(new String[0]);
			colors = tint.stream().mapToInt(Integer::intValue).toArray();
		}

		@Override
		public void render(GuiGraphics g, boolean preview) {
			rebuild();
			Font font = HudStyle.font();
			HudStyle.panel(g, width(), height());
			for (int i = 0; i < lines.length; i++) {
				Draw2D.text(g, font, Draw2D.ellipsize(font, lines[i], width() - 10), 5, 4 + i * 10, colors[i]);
			}
		}
	}
}
