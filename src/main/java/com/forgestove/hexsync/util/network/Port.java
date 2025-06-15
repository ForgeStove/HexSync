package com.forgestove.hexsync.util.network;
import org.jetbrains.annotations.*;
public class Port extends Number implements Comparable<Port> {
	public static final int MAX_VALUE = 65535;
	private final short value;
	/**
	 * 使用整数值创建端口。
	 *
	 * @param value 端口的整数值，必须在 0 到{@link #MAX_VALUE}之间
	 * @throws IllegalArgumentException 如果端口值不在有效范围内
	 */
	public Port(int value) {
		if (value >= 0 && value <= MAX_VALUE) this.value = (short) value;
		else throw new IllegalArgumentException("Port value must be between 0 and " + MAX_VALUE);
	}
	/**
	 * 使用字符串值创建端口。
	 *
	 * @param value 字符串形式的端口值
	 * @throws NumberFormatException 如果字符串不包含可解析的数字
	 */
	public Port(String value) {
		this(Integer.parseInt(value));
	}
	/**
	 * 获取端口值。
	 *
	 * @return 0 到 {@link #MAX_VALUE} 范围内的端口整数值
	 */
	public int getValue() {return value & MAX_VALUE;}
	/**
	 * 返回此端口的字符串表示形式。
	 *
	 * @return 表示端口值的非空字符串
	 */
	public @NotNull String toString() {return String.valueOf(getValue());}
	public int compareTo(@NotNull Port port) {return Integer.compare(getValue(), port.getValue());}
	@Contract(pure = true)
	public int intValue() {return getValue();}
	@Contract(pure = true)
	public long longValue() {return getValue();}
	@Contract(pure = true)
	public float floatValue() {return getValue();}
	@Contract(pure = true)
	public double doubleValue() {return getValue();}
}
