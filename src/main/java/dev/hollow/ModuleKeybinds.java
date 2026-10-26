package dev.hollow;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.core.module.Module;
import dev.hollow.core.module.ModuleManager;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;

/** Toggles modules from their keybinds while no screen is open. */
final class ModuleKeybinds {
	private final ModuleManager modules;
	private final Set<Integer> held = new HashSet<>();

	ModuleKeybinds(ModuleManager modules) {
		this.modules = modules;
	}

	void tick(Minecraft client) {
		if (client.screen != null || client.player == null) {
			held.clear();
			return;
		}
		Set<Integer> pressedNow = new HashSet<>();
		for (Module module : modules.all()) {
			if (!module.isToggleable() || !module.keybind().isBound()) {
				continue;
			}
			int key = module.keybind().get();
			if (InputConstants.isKeyDown(client.getWindow(), key)) {
				pressedNow.add(key);
				if (!held.contains(key)) {
					module.toggle();
				}
			}
		}
		held.clear();
		held.addAll(pressedNow);
	}
}
