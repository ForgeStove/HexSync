package com.forgestove.hexsync.util.network;
import org.jetbrains.annotations.*;

import java.math.BigInteger;
/**
 * 表示网络传输速率的类，支持不同单位(bps, Kbps, Mbps, Gbps)之间的转换和比较。
 * 该类是不可变的，所有操作都会返回新的实例。
 */
public class Rate implements Comparable<Rate> {
	/** 速率数值 */
	public final long value;
	/** 速率单位 */
	public final Unit unit;
	/** 统一转换为bps单位的速率值 */
	public final long bps;
	/**
	 * 使用指定数值和单位创建速率对象
	 *
	 * @param value 速率数值
	 * @param unit  速率单位
	 */
	@Contract(pure = true)
	public Rate(long value, @NotNull Unit unit) {
		this.value = value;
		this.unit = unit;
		bps = multiplyUnsigned(value, unit.multiplier);
	}
	/**
	 * 从字符串解析速率对象，格式为"数值 单位"，例如"10 Mbps"
	 * 如果解析失败，将默认为1 Mbps
	 *
	 * @param input 包含速率信息的字符串
	 */
	public Rate(@NotNull String input) {
		var parts = input.trim().split("\\s+");
		if (parts.length == 2 && parts[0].matches("\\d+")) {
			var parsedUnit = Unit.from(parts[1]);
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
	/**
	 * 从字符串的数值和指定单位创建速率对象
	 *
	 * @param string 表示速率数值的字符串
	 * @param unit   速率单位
	 */
	@Contract(pure = true)
	public Rate(@NotNull String string, @NotNull Unit unit) {
		value = Long.parseUnsignedLong(string);
		this.unit = unit;
		bps = multiplyUnsigned(value, unit.multiplier);
	}
	/**
	 * 执行无符号长整数乘法，防止溢出
	 * 如果结果超过无符号长整数最大值，返回-1
	 *
	 * @param a 第一个无符号长整数
	 * @param b 第二个无符号长整数
	 * @return 乘法结果，如果溢出则返回-1
	 */
	public static long multiplyUnsigned(long a, long b) {
		var bigA = new BigInteger(Long.toUnsignedString(a));
		var bigB = new BigInteger(Long.toUnsignedString(b));
		var result = bigA.multiply(bigB);
		if (result.compareTo(new BigInteger("18446744073709551615")) > 0) return -1;
		return result.longValue();
	}
	/**
	 * 返回速率的字符串表示，格式为"数值 单位"
	 *
	 * @return 速率的字符串表示
	 */
	public String toString() {
		return Long.toUnsignedString(value) + " " + unit;
	}
	/**
	 * 基于bps值比较两个速率大小
	 *
	 * @param rate 要比较的另一个速率对象
	 * @return 如果当前速率小于参数速率，返回负数；相等返回0；大于返回正数
	 */
	public int compareTo(@NotNull Rate rate) {
		return Long.compareUnsigned(bps, rate.bps);
	}
	/**
	 * 速率单位枚举，定义了不同单位与基本单位bps的换算关系
	 */
	public enum Unit {
		/** 比特每秒 - 基本单位 */
		bps(1L),
		/** 千比特每秒 (10^3 bps) */
		Kbps(1000L),
		/** 兆比特每秒 (10^6 bps) */
		Mbps(1000000L),
		/** 吉比特每秒 (10^9 bps) */
		Gbps(1000000000L);
		/** 该单位相对于bps的乘数因子 */
		public final long multiplier;
		/**
		 * 创建速率单位
		 *
		 * @param multiplier 相对于bps的乘数因子
		 */
		@Contract(pure = true)
		Unit(long multiplier) {
			this.multiplier = multiplier;
		}
		/**
		 * 从字符串解析速率单位
		 *
		 * @param unit 表示单位的字符串
		 * @return 对应的单位枚举值，如果无法解析则返回null
		 */
		public static @Nullable Unit from(String unit) {
			for (var rateUnit : values()) if (rateUnit.name().equalsIgnoreCase(unit)) return rateUnit;
			return null;
		}
	}
}
