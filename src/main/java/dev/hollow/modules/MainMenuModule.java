package dev.hollow.modules;

import dev.hollow.character.Cosmetic;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.ColorSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.NumberSetting;
import dev.hollow.core.setting.TextSetting;
import dev.hollow.menu.MenuBackgrounds;
import java.util.regex.Pattern;
import net.minecraft.util.Util;

/** The Holo main menu. Turning it off restores Minecraft's own title screen. */
public final class MainMenuModule extends Module {
	public enum Scene implements EnumSetting.Displayable {
		TEMPLE("Temple of Anubis"),
		PYRAMIDS("Pyramids at Night"),
		VOID("Void Realm"),
		SKY("Current Sky Studio Sky"),
		CUSTOM("Custom Image");

		private final String label;

		Scene(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public enum Featured implements EnumSetting.Displayable {
		NONE("Hidden"),
		FOLLOW_STUDIO("Your Character Studio Look"),
		CHOOSE("A Specific Character");

		private final String label;

		Featured(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9 _().-]+\\.png");

	public final EnumSetting<Scene> scene = add("Background", new EnumSetting<>("scene", "Scene", "", Scene.TEMPLE));
	public final TextSetting customImage = add("Background", new TextSetting("custom_image", "Custom Image",
		"A PNG in config/hollow/backgrounds (max 4096 px). Invalid files fall back to the temple scene.", "background.png", 64,
		FILE_NAME.asMatchPredicate(), "background.png"));
	public final ActionSetting openFolder = add("Background", new ActionSetting("open_folder", "Backgrounds Folder",
		"Open the folder custom backgrounds are read from.", "Open folder", () -> Util.getPlatform().openPath(MenuBackgrounds.directory())));
	public final EnumSetting<Featured> character = add("Character", new EnumSetting<>("character", "Featured Character",
		"A 3D figure wearing your skin. Your Character Studio Look follows the outfit you wear in game.", Featured.FOLLOW_STUDIO));
	public final EnumSetting<Cosmetic> featuredCharacter = add("Character", new EnumSetting<>("featured_character", "Character",
		"", Cosmetic.GRIM_REAPER));
	public final ColorSetting accent = add("Lighting", new ColorSetting("accent", "Menu Accent", "Logo glow and button highlights.", 0xFF9D4EDD, false));
	public final NumberSetting lighting = add("Lighting", new NumberSetting("lighting", "Lighting", "Strength of glows and rim light.", 1.0, 0, 1.5, 0.05).unit("x"));
	public final NumberSetting animation = add("Animation", new NumberSetting("animation", "Animation Intensity",
		"0 holds the scene still.", 1.0, 0, 1.5, 0.05).unit("x"));
	public final BoolSetting particles = add("Effects", new BoolSetting("particles", "Floating Particles", "Drifting embers and motes.", true));
	public final NumberSetting particleCount = add("Effects", new NumberSetting("particle_count", "Particle Count", "", 60, 0, 200, 5));
	public final BoolSetting decorations = add("Effects", new BoolSetting("decorations", "Decorative Effects", "Light rays, logo ring and vignette.", true));

	public MainMenuModule() {
		super("main_menu", "Main Menu", "The Holo title screen. Turn off to restore Minecraft's own menu.", Category.SETTINGS, true);
		customImage.visibleWhen(() -> scene.get() == Scene.CUSTOM);
		featuredCharacter.visibleWhen(() -> character.get() == Featured.CHOOSE);
		openFolder.visibleWhen(() -> scene.get() == Scene.CUSTOM);
		BooleanVisibility.when(particles, particleCount);
	}
}
