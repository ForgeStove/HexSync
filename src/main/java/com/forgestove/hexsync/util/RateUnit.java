package com.forgestove.hexsync.util;
import org.jetbrains.annotations.NotNull;
public enum RateUnit {
	BPS("B/s"),
	KBPS("K" + BPS.unit),
	MBPS("M" + BPS.unit),
	GBPS("G" + BPS.unit);
	public final String unit;
	RateUnit(String unit) {
		this.unit = unit;
	}
	public static @NotNull RateUnit fromUnit(String unit) {
		for (var rateUnit : values()) if (rateUnit.unit.equals(unit)) return rateUnit;
		throw new IllegalArgumentException("无效的速率单位: " + unit);
	}
}
