package dev.hollow.modules;

import dev.hollow.camera.Freecam;
import dev.hollow.core.input.Keybind;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.Tickable;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.policy.Automation;
import java.util.Optional;

/**
 * Detaches the camera from the player. Your player stays exactly where it is; the camera flies
 * with the normal movement keys. Interactions (attacking, using, mining) still happen from your
 * player, never from the camera, so by default they are blocked while the camera is detached.
 */
public final class FreecamModule extends Module implements Tickable {
	public final NumberSetting speed = add("Movement", new NumberSetting("speed", "Speed",
		"Camera speed. The mouse wheel changes it while Freecam is on.", 10, 0.5, 60, 0.5).unit(" blocks/s"));
	public final NumberSetting acceleration = add("Movement", new NumberSetting("acceleration", "Acceleration",
		"How quickly the camera reaches full speed and stops. Lower is floatier.", 8, 1, 30, 0.5));
	public final NumberSetting boost = add("Movement", new NumberSetting("boost", "Sprint Multiplier",
		"Speed multiplier while the sprint key is held.", 3, 1, 10, 0.5).unit("x"));
	public final NumberSetting smoothRotation = add("Movement", new NumberSetting("smooth_rotation", "Rotation Smoothing",
		"0 follows the mouse exactly; higher values glide.", 0, 0, 0.9, 0.05));
	public final NumberSetting maxDistance = add("Movement", new NumberSetting("max_distance", "Max Distance From Player",
		"Beyond this the camera stops; your client only has the world loaded around your player.", 96, 8, 512, 8).unit(" blocks"));
	public final KeySetting resetKey = add("Controls", new KeySetting("reset_key", "Return Camera to Player",
		"Moves the camera back to your player's eyes without leaving Freecam.", Keybind.NONE));
	public final BoolSetting blockInteractions = add("Behaviour", new BoolSetting("block_interactions", "Block Interactions",
		"Ignore attack, use and pick-block while the camera is detached (they would act from your player, not the camera).", true));
	public final BoolSetting showHud = add("Behaviour", new BoolSetting("show_hud", "Camera Position HUD",
		"Shows the camera's coordinates and speed (move it in the HUD editor).", true));

	public FreecamModule() {
		super("freecam", "Freecam", "Fly the camera freely while your player stays put. Turn it off to return to normal view.",
			Category.RENDER, false);
	}

	@Override
	public Optional<String> blockedReason() {
		return Automation.detectionBlockedReason();
	}

	@Override
	protected void onEnable() {
		Freecam.INSTANCE.start();
	}

	@Override
	protected void onDisable() {
		Freecam.INSTANCE.stop();
	}

	@Override
	public void tick() {
		Freecam.INSTANCE.tick(this);
	}
}
