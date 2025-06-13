package com.forgestove.hexsync.util.object;
import org.jetbrains.annotations.*;
public class Rate {
	public final long value;
	public final Unit unit;
	public final long bps;
	@Contract(pure = true)
	public Rate(long value, @NotNull Unit unit) {
		this.value = value;
		this.unit = unit;
		bps = value * unit.multiplier;
	}
	public Rate(@NotNull String input) {
		var parts = input.trim().split("\\s+");
		if (parts.length == 2 && parts[0].matches("\\d+")) {
			var parsedUnit = Unit.fromString(parts[1]);
			if (parsedUnit != null) {
				value = Long.parseLong(parts[0]);
				unit = parsedUnit;
				bps = value * unit.multiplier;
				return;
			}
		}
		value = 1;
		unit = Unit.Mbps;
		bps = value * unit.multiplier;
	}
	@Contract(pure = true)
	public Rate(@NotNull String string, @NotNull Unit unit) {
		value = Long.parseLong(string);
		this.unit = unit;
		bps = value * unit.multiplier;
	}
	@Override
	public String toString() {
		return value + " " + unit;
	}
	public enum Unit {
		bps(1),
		Kbps(1000),
		Mbps(1000000),
		Gbps(1000000000);
		public final long multiplier;
		Unit(long multiplier) {
			this.multiplier = multiplier;
		}
		public static @Nullable Unit fromString(String unit) {
			for (var rateUnit : values()) if (rateUnit.name().equalsIgnoreCase(unit)) return rateUnit;
			return null;
		}
	}
}
