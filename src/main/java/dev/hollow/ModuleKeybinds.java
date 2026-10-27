package dev.hollow;

import dev.hollow.core.input.BindMode;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.ModuleManager;
import dev.hollow.input.KeyInput;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;

/**
 * Applies module keybinds during gameplay: Toggle binds flip the module on each press, Hold
 * binds keep it on only while held. Nothing fires while a screen (chat, sign, menu) is open or
 * the window is unfocused, and Hold modules switch off as soon as that happens.
 */
final class ModuleKeybinds {
	private final ModuleManager modules;
	private final Set<Module> held = new HashSet<>();
	private final Set<Module> enabledByHold = new HashSet<>();

	ModuleKeybinds(ModuleManager modules) {
		this.modules = modules;
	}

	void tick(Minecraft client) {
		boolean active = KeyInput.gameplayInputActive();
		for (Module module : modules.all()) {
			if (!module.isToggleable() || !module.keybind().isBound()) {
				release(module);
				continue;
			}
			boolean down = active && KeyInput.isDown(module.keybind().get());
			boolean wasDown = held.contains(module);
			if (module.bindMode().get() == BindMode.HOLD) {
				if (down && !wasDown && !module.isEnabled()) {
					module.setEnabled(true);
					enabledByHold.add(module);
				} else if (!down && enabledByHold.remove(module)) {
					module.setEnabled(false);
				}
			} else if (down && !wasDown) {
				module.toggle();
			}
			if (down) {
				held.add(module);
			} else {
				held.remove(module);
			}
		}
	}

	private void release(Module module) {
		held.remove(module);
		if (enabledByHold.remove(module)) {
			module.setEnabled(false);
		}
	}
}
