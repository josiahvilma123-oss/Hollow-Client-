package dev.hollow.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hollow.Hollow;
import dev.hollow.core.input.Keybind;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.KeySetting;
import dev.hollow.core.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Finds other Holo binds and Minecraft controls that use the same key. */
public final class KeybindConflicts {
	private KeybindConflicts() {
	}

	/** Human-readable conflicts for a bind, e.g. "Kill Aura: Key" or "Minecraft: Jump". Empty when unbound. */
	public static List<String> of(KeySetting owner, Keybind keybind) {
		List<String> conflicts = new ArrayList<>();
		if (!keybind.isBound()) {
			return conflicts;
		}
		for (Module module : Hollow.modules().all()) {
			for (Setting<?> setting : module.settings()) {
				if (setting != owner && setting instanceof KeySetting other && other.get().equals(keybind)) {
					conflicts.add(module.name() + ": " + other.name());
				}
			}
		}
		Minecraft minecraft = Minecraft.getInstance();
		for (KeyMapping mapping : minecraft.options.keyMappings) {
			InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(mapping);
			boolean sameType = keybind.mouse() ? bound.getType() == InputConstants.Type.MOUSE : bound.getType() == InputConstants.Type.KEYSYM;
			if (sameType && bound.getValue() == keybind.code()) {
				conflicts.add("Minecraft: " + net.minecraft.network.chat.Component.translatable(mapping.getName()).getString());
			}
		}
		return conflicts;
	}
}
