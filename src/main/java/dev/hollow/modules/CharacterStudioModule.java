package dev.hollow.modules;

import dev.hollow.character.Cosmetic;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.EnumSetting;

/** Character Studio: original cosmetic sets drawn on your own player, visible only to you. */
public final class CharacterStudioModule extends Module {
	public final EnumSetting<Cosmetic> cosmetic = add("Character", new EnumSetting<>("cosmetic", "Character",
		"Cosmetic set worn by your player in third person and inventory views. Only you can see it.", Cosmetic.NONE));

	public CharacterStudioModule() {
		super("character_studio", "Character Studio", "Original Pharaoh, Wraith and Hollow Knight character cosmetics.",
			Category.STUDIO, true);
	}
}
