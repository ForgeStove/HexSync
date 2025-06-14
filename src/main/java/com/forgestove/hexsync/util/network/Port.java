package com.forgestove.hexsync.util.network;
import org.jetbrains.annotations.Contract;
public class Port {
	public static final int MAX_VALUE = 65535;
	private final short value;
	@Contract(pure = true)
	public Port(int value) {
		if (value >= 0 && value <= MAX_VALUE) this.value = (short) value;
		else this.value = (short) MAX_VALUE;
	}
	public Port(String value) {
		this(getValue(value));
	}
	@Contract(pure = true)
	private static int getValue(String value) {
		try {return Integer.parseInt(value);} catch (Exception error) {return MAX_VALUE;}
	}
	public int getValue() {
		return value & MAX_VALUE;
	}
	@Override
	public String toString() {
		return String.valueOf(getValue());
	}
}
