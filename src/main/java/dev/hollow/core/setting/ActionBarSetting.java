package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import java.util.List;

/** A row of related buttons, such as Save / Rename / Delete. Never persisted. */
public class ActionBarSetting extends Setting<String> {
	public record Button(String label, Runnable action) {
	}

	private final List<Button> buttons;

	public ActionBarSetting(String id, String name, String description, Button... buttons) {
		super(id, name, description, "");
		this.buttons = List.of(buttons);
	}

	public List<Button> buttons() {
		return buttons;
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
