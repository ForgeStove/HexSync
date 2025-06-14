package com.forgestove.hexsync.util.network;
import org.jetbrains.annotations.*;

import java.math.BigInteger;
public class Rate implements Comparable<Rate> {
	public final long value;
	public final Unit unit;
	public final long bps;
	@Contract(pure = true)
	public Rate(long value, @NotNull Unit unit) {
		this.value = value;
		this.unit = unit;
		bps = multiplyUnsigned(value, unit.multiplier);
	}
	public Rate(@NotNull String input) {
		var parts = input.trim().split("\\s+");
		if (parts.length == 2 && parts[0].matches("\\d+")) {
			var parsedUnit = Unit.fromString(parts[1]);
			if (parsedUnit != null) {
				value = Long.parseUnsignedLong(parts[0]);
				unit = parsedUnit;
				bps = multiplyUnsigned(value, unit.multiplier);
				return;
			}
		}
		value = 1L;
		unit = Unit.Mbps;
		bps = multiplyUnsigned(value, unit.multiplier);
	}
	@Contract(pure = true)
	public Rate(@NotNull String string, @NotNull Unit unit) {
		value = Long.parseUnsignedLong(string);
		this.unit = unit;
		bps = multiplyUnsigned(value, unit.multiplier);
	}
	public static long multiplyUnsigned(long a, long b) {
		var bigA = new BigInteger(Long.toUnsignedString(a));
		var bigB = new BigInteger(Long.toUnsignedString(b));
		var result = bigA.multiply(bigB);
		if (result.compareTo(new BigInteger("18446744073709551615")) > 0) return -1;
		return result.longValue();
	}
	public String toString() {return Long.toUnsignedString(value) + " " + unit;}
	public int compareTo(@NotNull Rate rate) {return Long.compareUnsigned(bps, rate.bps);}
	public enum Unit {
		bps(1L),
		Kbps(1000L),
		Mbps(1000000L),
		Gbps(1000000000L);
		public final long multiplier;
		@Contract(pure = true)
		Unit(long multiplier) {
			this.multiplier = multiplier;
		}
		public static @Nullable Unit fromString(String unit) {
			for (var rateUnit : values()) if (rateUnit.name().equalsIgnoreCase(unit)) return rateUnit;
			return null;
		}
	}
}
