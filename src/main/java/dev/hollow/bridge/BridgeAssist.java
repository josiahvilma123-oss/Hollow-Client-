package dev.hollow.bridge;

import dev.hollow.Hollow;
import dev.hollow.alerts.Alert;
import dev.hollow.alerts.AlertSource;
import dev.hollow.core.bridge.BridgeMath;
import dev.hollow.core.util.Colors;
import dev.hollow.modules.AutoBridgeModule;
import dev.hollow.modules.SpeedBridgeModule;
import dev.hollow.policy.Automation;
import dev.hollow.render.OverlayProvider;
import dev.hollow.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared engine of Auto Bridge and Speed Bridge: placement, timing cue, preview and statistics. */
public final class BridgeAssist implements OverlayProvider {
	public static final BridgeAssist INSTANCE = new BridgeAssist();

	private int cooldown;
	private Bridging.Result lastResult = Bridging.Result.NOTHING_TO_DO;
	private BridgeMath.Placement nextPlan;
	private boolean cue;
	private boolean cueWasOn;
	private int placed;
	private long firstPlaceAt = -1;
	private long lastPlaceAt = -1;
	private double runLength;
	private double longestRun;
	private int falls;
	private double groundY = Double.NaN;
	private double runStartX;
	private double runStartZ;

	private BridgeAssist() {
	}

	public Bridging.Result lastResult() {
		return lastResult;
	}

	public boolean cue() {
		return cue;
	}

	public int placed() {
		return placed;
	}

	public int falls() {
		return falls;
	}

	public double longestRun() {
		return longestRun;
	}

	/** Blocks per second over the current practice session. */
	public double blocksPerSecond() {
		if (placed < 2 || firstPlaceAt < 0 || lastPlaceAt <= firstPlaceAt) {
			return 0;
		}
		return (placed - 1) / ((lastPlaceAt - firstPlaceAt) / 1000.0);
	}

	public void resetStats() {
		placed = 0;
		firstPlaceAt = lastPlaceAt = -1;
		runLength = longestRun = 0;
		falls = 0;
	}

	/** The plan for the block underfoot, or (with lookahead) the next block in the walking direction. */
	private static BridgeMath.Placement plan(LocalPlayer player, boolean ahead) {
		Minecraft minecraft = Minecraft.getInstance();
		BridgeMath.World world = Bridging.world(minecraft.level);
		Vec3 move = Bridging.movement(player);
		BridgeMath.Placement under = BridgeMath.plan(world, player.getX(), player.getY(), player.getZ(), move.x, move.z);
		if (under != null || !ahead || move.lengthSqr() < 1e-6) {
			return under;
		}
		Vec3 dir = move.normalize().scale(0.6);
		return BridgeMath.plan(world, player.getX() + dir.x, player.getY(), player.getZ() + dir.z, move.x, move.z);
	}

	/** Only bridge from the ground or right after stepping off it, never mid-jump or while flying. */
	private static boolean grounded(LocalPlayer player) {
		return !player.getAbilities().flying && (player.onGround() || player.getDeltaMovement().y <= 0 && player.fallDistance < 1.0);
	}

	public void tickAuto(AutoBridgeModule m) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || minecraft.gameMode == null) {
			return;
		}
		nextPlan = grounded(player) ? plan(player, m.placeAhead.get()) : null;
		if (!Automation.allowed()) {
			return;
		}
		if (cooldown > 0) {
			cooldown--;
			return;
		}
		if (nextPlan == null) {
			lastResult = Bridging.Result.NOTHING_TO_DO;
			return;
		}
		lastResult = Bridging.place(minecraft, player, nextPlan, m.autoSelect.get(), m.faceBlock.get());
		if (lastResult == Bridging.Result.PLACED) {
			cooldown = m.delay.getInt();
			onPlaced();
		} else if (lastResult == Bridging.Result.NO_BLOCKS) {
			m.setEnabled(false);
			if (Hollow.alerts() != null) {
				Hollow.alerts().report(new Alert(AlertSource.SYSTEM, "Auto Bridge", "stopped: out of blocks", Double.NaN, "bridge:empty"));
			}
		}
	}

	public void tickSpeed(SpeedBridgeModule m) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || minecraft.gameMode == null) {
			cue = false;
			return;
		}
		boolean grounded = grounded(player);
		nextPlan = grounded ? plan(player, true) : null;
		Vec3 move = Bridging.movement(player);
		cue = player.onGround() && move.lengthSqr() > 1e-6 && BridgeMath.atEdge(Bridging.world(minecraft.level), player.getX(), player.getY(),
			player.getZ(), move.x, move.z, BridgeInput.LOOKAHEAD + m.cueLead.get());
		if (cue && !cueWasOn && m.cueSound.get()) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f));
		}
		cueWasOn = cue;
		trackRun(player);
		if (m.autoPlace.get() && Automation.allowed()) {
			if (cooldown > 0) {
				cooldown--;
			} else {
				BridgeMath.Placement under = grounded ? plan(player, false) : null;
				if (under != null) {
					lastResult = Bridging.place(minecraft, player, under, true, false);
					if (lastResult == Bridging.Result.PLACED) {
						cooldown = 1;
						onPlaced();
					}
				}
			}
		}
	}

	/** Counts a placement for the statistics (automatic or manual). */
	public void onPlaced() {
		long now = System.currentTimeMillis();
		if (firstPlaceAt < 0 || now - lastPlaceAt > 3000) {
			firstPlaceAt = now;
			placed = 0;
		}
		placed++;
		lastPlaceAt = now;
	}

	/** Manual placements, reported by the block-use hook while Speed Bridge is on. */
	public void onManualPlace(ItemStack stack) {
		if (Bridging.isBridgeBlock(stack)) {
			onPlaced();
		}
	}

	private void trackRun(LocalPlayer player) {
		if (player.onGround()) {
			if (Double.isNaN(groundY) || Math.abs(player.getY() - groundY) > 1.5) {
				runStartX = player.getX();
				runStartZ = player.getZ();
			}
			groundY = player.getY();
			runLength = Math.hypot(player.getX() - runStartX, player.getZ() - runStartZ);
			longestRun = Math.max(longestRun, runLength);
		} else if (!Double.isNaN(groundY) && player.getY() < groundY - 2.5) {
			falls++;
			groundY = Double.NaN;
		}
	}

	@Override
	public void collect(Sink sink, float partialTick) {
		if (Hollow.modules() == null) {
			return;
		}
		AutoBridgeModule auto = Hollow.module(AutoBridgeModule.class);
		SpeedBridgeModule speed = Hollow.module(SpeedBridgeModule.class);
		boolean preview = auto.isEnabled() && auto.preview.get() || speed.isEnabled() && speed.preview.get();
		int accent = Theme.accent();
		if (preview && nextPlan != null) {
			AABB box = new AABB(nextPlan.targetX(), nextPlan.targetY(), nextPlan.targetZ(), nextPlan.targetX() + 1, nextPlan.targetY() + 1,
				nextPlan.targetZ() + 1).deflate(0.02);
			sink.fill(box, Colors.withAlpha(accent, 40), false);
			sink.outline(box, Colors.withAlpha(accent, 200), 2f, false);
		}
		LocalPlayer player = Minecraft.getInstance().player;
		if (speed.isEnabled() && speed.edgeIndicator.get() && player != null && player.onGround()) {
			Vec3 move = Bridging.movement(player);
			if (move.lengthSqr() > 1e-6) {
				int bx = (int) Math.floor(player.getX());
				int by = BridgeMath.supportY(player.getY()) + 1;
				int bz = (int) Math.floor(player.getZ());
				int color = cue ? 0xFFE6B54A : Colors.withAlpha(accent, 160);
				double y = by + 0.01;
				if (Math.abs(move.x) >= Math.abs(move.z)) {
					double x = move.x > 0 ? bx + 1 : bx;
					sink.line(new Vec3(x, y, bz), new Vec3(x, y, bz + 1), color, cue ? 4f : 2f);
				} else {
					double z = move.z > 0 ? bz + 1 : bz;
					sink.line(new Vec3(bx, y, z), new Vec3(bx + 1, y, z), color, cue ? 4f : 2f);
				}
			}
		}
	}
}
