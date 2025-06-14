package com.forgestove.hexsync.util.network;
import org.jetbrains.annotations.*;

import java.math.BigInteger;
public class Rate {
	public final long value; // 存储为普通 long
	public final Unit unit;
	public final long bps;   // 存储为普通 long
	@Contract(pure = true)
	public Rate(long value, @NotNull Unit unit) {
		this.value = value; // 按无符号方式处理
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
	// 安全处理无符号乘法溢出
	public static long multiplyUnsigned(long a, long b) {
		var bigA = new BigInteger(Long.toUnsignedString(a));
		var bigB = new BigInteger(Long.toUnsignedString(b));
		var result = bigA.multiply(bigB);
		// 如果结果超过无符号 long 范围，将返回最大无符号 long 值
		if (result.compareTo(new BigInteger("18446744073709551615")) > 0) return -1; // 表示最大无符号 long
		return result.longValue();
	}
	@Override
	public String toString() {
		return Long.toUnsignedString(value) + " " + unit;
	}
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