package dev.hollow.combat;

import dev.hollow.core.setting.BoolSetting;
import dev.hollow.core.setting.EnumSetting;
import dev.hollow.core.setting.Setting;
import java.util.List;

/** Target filters shared by the combat modules. Each module owns its own instance. */
public final class TargetSettings {
	public enum Priority implements EnumSetting.Displayable {
		CROSSHAIR("Closest to Crosshair"),
		NEAREST("Nearest"),
		LOWEST_HEALTH("Lowest Health");

		private final String label;

		Priority(String label) {
			this.label = label;
		}

		@Override
		public String displayName() {
			return label;
		}
	}

	public final BoolSetting players;
	public final BoolSetting hostile;
	public final BoolSetting neutral;
	public final BoolSetting passive;
	public final BoolSetting ignoreTeammates = new BoolSetting("ignore_teammates", "Ignore Teammates",
		"Skip players on your scoreboard team.", true);
	public final BoolSetting requireSight = new BoolSetting("require_sight", "Require Line of Sight",
		"Only targets you could actually see; never through walls.", true);
	public final EnumSetting<Priority> priority;

	public TargetSettings(boolean playersByDefault, Priority defaultPriority) {
		players = new BoolSetting("target_players", "Players", "", playersByDefault);
		hostile = new BoolSetting("target_hostile", "Hostile Mobs", "", true);
		neutral = new BoolSetting("target_neutral", "Neutral Mobs", "Villagers, wolves, endermen and similar.", false);
		passive = new BoolSetting("target_passive", "Passive Mobs & Animals", "", false);
		priority = new EnumSetting<>("priority", "Target Priority", "Which eligible target is chosen first.", defaultPriority);
	}

	/** Settings in display order, for adding to a module's "Targets" section. */
	public List<Setting<?>> all() {
		return List.of(priority, players, hostile, neutral, passive, ignoreTeammates, requireSight);
	}
}
