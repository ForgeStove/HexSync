package com.forgestove.hexsync.util;
import org.jetbrains.annotations.Nullable;
public enum RateUnit {
	bps,
	Kbps,
	Mbps,
	Gbps;
	public static @Nullable RateUnit fromUnit(String unit) {
		for (var rateUnit : values()) if (rateUnit.name().equalsIgnoreCase(unit)) return rateUnit;
		return null;
	}
}
