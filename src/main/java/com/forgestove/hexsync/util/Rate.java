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
	@Contract("_ -> new")
	public static @NotNull Rate fromString(@NotNull String input) {
		var parts = input.split("\\s+");
		if (parts.length != 2 || !parts[0].matches("\\d+")) return new Rate(1, Unit.Mbps);
		var rateUnit = Unit.fromString(parts[1]);
		if (rateUnit == null || SettingUtil.isInvalidLong(parts[0])) return new Rate(1, Unit.Mbps);
		return new Rate(Long.parseLong(parts[0]), rateUnit);
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
