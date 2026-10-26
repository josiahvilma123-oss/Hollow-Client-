package dev.hollow.modules;

import dev.hollow.character.Cosmetic;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.EnumSetting;

/** Character Studio: original 3D outfits drawn on your own player, visible only to you. */
public final class CharacterStudioModule extends Module {
	public final EnumSetting<Cosmetic> cosmetic = add("Character", new EnumSetting<>("cosmetic", "Character",
		"Outfit worn by your player in third person, the inventory and the menus. Only you can see it.", Cosmetic.NONE));

	public CharacterStudioModule() {
		super("character_studio", "Character Studio", "Eight original 3D outfits for your own skin, from a grim reaper to a space voyager.",
			Category.STUDIO, true);
	}
}
