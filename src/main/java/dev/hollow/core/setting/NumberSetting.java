package dev.hollow.core.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Locale;

/** A bounded numeric value rendered as a slider. Values snap to {@link #step()}. */
public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;
	private String unit = "";

	public NumberSetting(String id, String name, String description, double defaultValue, double min, double max, double step) {
		super(id, name, description, validateDefault(defaultValue, min, max, step));
		this.min = min;
		this.max = max;
		this.step = step;
	}

	private static double validateDefault(double value, double min, double max, double step) {
		if (!(min < max)) {
			throw new IllegalArgumentException("min must be < max");
		}
		if (step < 0) {
			throw new IllegalArgumentException("step must be >= 0");
		}
		if (value < min || value > max) {
			throw new IllegalArgumentException("default " + value + " outside [" + min + ", " + max + "]");
		}
		return value;
	}

	public NumberSetting unit(String unit) {
		this.unit = unit == null ? "" : unit;
		return this;
	}

	public String unit() {
		return unit;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}

	public double step() {
		return step;
	}

	public float getFloat() {
		return get().floatValue();
	}

	public int getInt() {
		return (int) Math.round(get());
	}

	/** Position of the current value between min and max, in [0, 1]. */
	public double fraction() {
		return (get() - min) / (max - min);
	}

	public void setFraction(double fraction) {
		set(min + Math.max(0, Math.min(1, fraction)) * (max - min));
	}

	@Override
	protected Double sanitize(Double candidate) {
		double v = candidate;
		if (Double.isNaN(v)) {
			return get();
		}
		v = Math.max(min, Math.min(max, v));
		if (step > 0) {
			v = min + Math.round((v - min) / step) * step;
			v = Math.max(min, Math.min(max, v));
			// Trim floating point noise such as 0.30000000000000004.
			v = Math.round(v * 1e6) / 1e6;
		}
		return v;
	}

	public String formatted() {
		double v = get();
		String number;
		if (step >= 1 || step == 0 && v == Math.rint(v)) {
			number = Long.toString(Math.round(v));
		} else if (step >= 0.1) {
			number = String.format(Locale.ROOT, "%.1f", v);
		} else {
			number = String.format(Locale.ROOT, "%.2f", v);
		}
		return number + unit;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsDouble());
		}
	}
}
