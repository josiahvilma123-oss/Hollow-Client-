package dev.hollow.modules;

import dev.hollow.Hollow;
import dev.hollow.character.Cosmetic;
import dev.hollow.core.module.Category;
import dev.hollow.core.module.Module;
import dev.hollow.core.setting.ActionSetting;
import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.hands.HandPreset;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinated theme collections. Each pack bundles a sky, character, interface colors, weapon
 * styling and menu scene; users apply any subset. Nothing changes until an apply button is used.
 */
public final class ThemePacksModule extends Module {
	public enum Pack implements EnumSetting.Displayable {
		PHARAOH("Pharaoh Collection", "Gold-and-lapis regalia under Pharaoh's Night, with golden swing trails.",
			"Pharaoh's Night", Cosmetic.PHARAOH, 0xFFD9A441, 0x88E6B54A, HandPreset.SMOOTH, MainMenuModule.Scene.TEMPLE),
		WRAITH("Wraith Collection", "A shadow-hooded wraith in the Void Realm, with spectral violet trails.",
			"Void Realm", Cosmetic.WRAITH, 0xFF7B3FD0, 0x887B3FD0, HandPreset.SUBTLE, MainMenuModule.Scene.VOID),
		HOLLOW_KNIGHT("Hollow Knight Collection", "An original armored knight beneath the Anubis Eclipse, with stylized swings.",
			"Anubis Eclipse", Cosmetic.HOLLOW_KNIGHT, 0xFFB9A6FF, 0x99C9A0FF, HandPreset.STYLIZED, MainMenuModule.Scene.SKY),
		GRIM_REAPER("Reaper Collection", "A skull-masked reaper with a scythe on his back, under the starless Void Realm.",
			"Void Realm", Cosmetic.GRIM_REAPER, 0xFF9D4EDD, 0x889D4EDD, HandPreset.SMOOTH, MainMenuModule.Scene.VOID),
		SHADOW_NINJA("Shadow Collection", "A masked ninja with a katana and trailing scarf, beneath an endless crimson dusk.",
			"Crimson Dusk", Cosmetic.SHADOW_NINJA, 0xFFD0453F, 0x88E0503C, HandPreset.STYLIZED, MainMenuModule.Scene.SKY),
		FROST_WITCH("Frost Collection", "A frost witch in a starry cape and tall hat, under an aurora-lit frozen sky.",
			"Frozen Cosmos", Cosmetic.FROST_WITCH, 0xFF8FDCFF, 0x889FE6FF, HandPreset.SMOOTH, MainMenuModule.Scene.SKY),
		INFERNAL_DEMON("Infernal Collection", "Horns, wings and ember-cracked armor beneath a black sun ringed in fire.",
			"Hellfire Rift", Cosmetic.INFERNAL_DEMON, 0xFFFF7A2A, 0x88FF6A1A, HandPreset.STYLIZED, MainMenuModule.Scene.SKY),
		STAR_VOYAGER("Voyager Collection", "A space suit with a gold visor, drifting under ringed planets.",
			"Astral Horizon", Cosmetic.STAR_VOYAGER, 0xFFF0A040, 0x88FFC070, HandPreset.SUBTLE, MainMenuModule.Scene.SKY);

		private final String displayName;
		private final String description;
		private final String sky;
		private final Cosmetic cosmetic;
		private final int accent;
		private final int trail;
		private final HandPreset hands;
		private final MainMenuModule.Scene scene;

		Pack(String displayName, String description, String sky, Cosmetic cosmetic, int accent, int trail, HandPreset hands,
				MainMenuModule.Scene scene) {
			this.displayName = displayName;
			this.description = description;
			this.sky = sky;
			this.cosmetic = cosmetic;
			this.accent = accent;
			this.trail = trail;
			this.hands = hands;
			this.scene = scene;
		}

		@Override
		public String displayName() {
			return displayName;
		}

		public String description() {
			return description;
		}

		public String sky() {
			return sky;
		}

		public Cosmetic cosmetic() {
			return cosmetic;
		}

		public int accent() {
			return accent;
		}

		public int trail() {
			return trail;
		}
	}

	public final EnumSetting<Pack> pack = add("Collection", new EnumSetting<>("pack", "Collection", "", Pack.PHARAOH));
	public final BoolSetting applySky = add("Components", new BoolSetting("apply_sky", "Sky",
		"Switch Sky Studio to the collection's sky (and turn Sky Studio on).", true));
	public final BoolSetting applyCharacter = add("Components", new BoolSetting("apply_character", "Character", "Wear the collection's cosmetic set.", true));
	public final BoolSetting applyInterface = add("Components", new BoolSetting("apply_interface", "Interface & HUD Colors",
		"Use the collection's accent for menus, alerts and labels.", true));
	public final BoolSetting applyWeapon = add("Components", new BoolSetting("apply_weapon", "Weapon & Animations",
		"Hollow Hands preset and matching swing-trail color.", true));
	public final BoolSetting applyMenu = add("Components", new BoolSetting("apply_menu", "Main Menu Scene", "Menu background and featured character.", true));
	public final ActionSetting applySelected = add("Apply", new ActionSetting("apply_selected", "Apply Selected Components",
		"Only the components ticked above change.", "Apply selected", () -> apply(false)));
	public final ActionSetting applyAll = add("Apply", new ActionSetting("apply_all", "Apply Entire Collection",
		"Every component of the collection, regardless of the ticks above.", "Apply all", () -> apply(true)));

	private String lastApplied = "";

	public ThemePacksModule() {
		super("theme_packs", "Theme Packs", "Coordinated collections: matching sky, character, colors, weapon style and menu.",
			Category.STUDIO, true);
	}

	@Override
	public boolean isToggleable() {
		return false;
	}

	public String lastApplied() {
		return lastApplied;
	}

	/** Applies the selected pack's components. Returns the list of what changed. */
	public List<String> apply(boolean everything) {
		Pack p = pack.get();
		List<String> changed = new ArrayList<>();
		if (everything || applySky.get()) {
			SkyStudioModule sky = Hollow.module(SkyStudioModule.class);
			sky.applyPreset(p.sky());
			sky.setEnabled(true);
			changed.add("sky");
		}
		if (everything || applyCharacter.get()) {
			CharacterStudioModule studio = Hollow.module(CharacterStudioModule.class);
			studio.cosmetic.set(p.cosmetic());
			studio.setEnabled(true);
			changed.add("character");
		}
		if (everything || applyInterface.get()) {
			Hollow.module(InterfaceModule.class).accent.set(p.accent());
			changed.add("colors");
		}
		if (everything || applyWeapon.get()) {
			HollowHandsModule hands = Hollow.module(HollowHandsModule.class);
			hands.preset.set(p.hands);
			hands.trailColor.set(p.trail());
			changed.add("weapon");
		}
		if (everything || applyMenu.get()) {
			MainMenuModule menu = Hollow.module(MainMenuModule.class);
			menu.scene.set(p.scene);
			menu.character.set(MainMenuModule.Featured.CHOOSE);
			menu.featuredCharacter.set(p.cosmetic());
			changed.add("menu");
		}
		lastApplied = changed.isEmpty() ? "Nothing selected" : "Applied " + p.displayName() + ": " + String.join(", ", changed);
		return changed;
	}
}
