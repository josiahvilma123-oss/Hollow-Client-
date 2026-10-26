package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A button in the settings page, such as "Preview alert". Never persisted. */
public class ActionSetting extends Setting<String> {
	private final Runnable action;

	public ActionSetting(String id, String name, String description, String buttonLabel, Runnable action) {
		super(id, name, description, buttonLabel);
		this.action = action;
	}

	public String buttonLabel() {
		return get();
	}

	public void run() {
		action.run();
	}

	@Override
	public boolean persistent() {
		return false;
	}

	@Override
	public JsonElement toJson() {
		return JsonNull.INSTANCE;
	}

	@Override
	public void fromJson(JsonElement json) {
	}
}
