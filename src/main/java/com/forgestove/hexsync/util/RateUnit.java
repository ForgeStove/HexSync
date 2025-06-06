package com.forgestove.hexsync.util;
import org.jetbrains.annotations.NotNull;
public enum RateUnit {
	BPS("B/s", 0),
	KBPS("K" + BPS.unit, 1),
	MBPS("M" + BPS.unit, 2),
	GBPS("G" + BPS.unit, 3);
	public final String unit;
	public final int exponent;
	RateUnit(String unit, int exponent) {
		this.unit = unit;
		this.exponent = exponent;
	}
	public static @NotNull RateUnit fromUnit(String unit) {
		for (var rateUnit : values()) if (rateUnit.unit.equals(unit)) return rateUnit;
		throw new IllegalArgumentException("无效的速率单位: " + unit);
	}
}
