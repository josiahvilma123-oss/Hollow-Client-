package dev.hollow.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import dev.hollow.core.input.Keybind;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Reads the live state of keybinds and names them for display. */
public final class KeyInput {
	private KeyInput() {
	}

	public static String keyName(int code) {
		return InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
	}

	public static String buttonName(int button) {
		return InputConstants.Type.MOUSE.getOrCreate(button).getDisplayName().getString();
	}

	public static String describe(Keybind keybind) {
		return keybind.describe(KeyInput::keyName, KeyInput::buttonName);
	}

	/** Ctrl/Shift/Alt currently held, as {@link Keybind} modifier flags. */
	public static int heldModifiers(Window window) {
		int mods = 0;
		if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
			mods |= Keybind.CTRL;
		}
		if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
			mods |= Keybind.SHIFT;
		}
		if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
			mods |= Keybind.ALT;
		}
		return mods;
	}

	/** The modifier flag a key itself represents (so binding "Left Shift" isn't also required to hold Shift). */
	public static int modifierOf(int code) {
		return switch (code) {
			case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> Keybind.CTRL;
			case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> Keybind.SHIFT;
			case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> Keybind.ALT;
			default -> 0;
		};
	}

	/** Whether the bind is held right now, with exactly its modifiers. */
	public static boolean isDown(Keybind keybind) {
		if (!keybind.isBound()) {
			return false;
		}
		Window window = Minecraft.getInstance().getWindow();
		boolean down = keybind.mouse()
			? GLFW.glfwGetMouseButton(window.handle(), keybind.code()) == GLFW.GLFW_PRESS
			: InputConstants.isKeyDown(window, keybind.code());
		if (!down) {
			return false;
		}
		int self = keybind.mouse() ? 0 : modifierOf(keybind.code());
		return (heldModifiers(window) & ~self) == keybind.modifiers();
	}

	/**
	 * Whether keybinds may act: in a world, window focused, no screen open (so typing in chat,
	 * signs or any menu never triggers modules).
	 */
	public static boolean gameplayInputActive() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null && minecraft.screen == null && minecraft.isWindowActive() && minecraft.getOverlay() == null;
	}
}
