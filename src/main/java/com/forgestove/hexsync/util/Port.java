package com.forgestove.hexsync.util;
import org.jetbrains.annotations.Contract;
public class Port {
	private final short value;
	@Contract(pure = true)
	public Port(int value) {
		if (value >= 0 && value <= 65535) this.value = (short) value;
		else throw new IllegalArgumentException("Port value must be between 0 and 65535");
	}
	@Contract(pure = true)
	public Port(String value) {
		this(getValue(value));
	}
	private static int getValue(String value) {
		try {return Integer.parseInt(value);} catch (Exception error) {
			throw new IllegalArgumentException("Port value must be an integer", error);
		}
	}
	public int getValue() {
		return value & 0xFFFF;
	}
	@Override
	public String toString() {
		return String.valueOf(getValue());
	}
}
