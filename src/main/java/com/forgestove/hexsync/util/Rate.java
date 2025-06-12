package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;
public class Rate {
	public final long value;
	public final Unit unit;
	public final long bps;
	public Rate(long value, Unit unit) {
		this.value = value;
		this.unit = unit;
		bps = this.value * (long) Math.pow(1000, this.unit.ordinal()) / 8;
	}
	public Rate(@NotNull String input) {
		var tempValue = (long) 1;
		var tempUnit = Unit.Mbps;
		var parts = input.trim().split("\\s+");
		if (parts.length == 2 && parts[0].matches("\\d+")) {
			var parsedUnit = Unit.fromString(parts[1]);
			if (parsedUnit != null && !SettingUtil.isInvalidLong(parts[0])) {
				tempValue = Long.parseLong(parts[0]);
				tempUnit = parsedUnit;
			}
		}
		value = tempValue;
		unit = tempUnit;
		bps = value * (long) Math.pow(1000, unit.ordinal()) / 8;
	}
	public String toString() {
		return value + " " + unit.name();
	}
	public enum Unit {
		bps,
		Kbps,
		Mbps,
		Gbps;
		public static @Nullable Unit fromString(String unit) {
			for (var rateUnit : values()) if (rateUnit.name().equalsIgnoreCase(unit)) return rateUnit;
			return null;
		}
	}
}
