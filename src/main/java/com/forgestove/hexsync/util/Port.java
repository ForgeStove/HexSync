package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;
public class Port {
	private final short value;
	private Port(short value) {
		this.value = value;
	}
	@Contract("_ -> new")
	public static @NotNull Port of(int value) {
		if (value >= 0 && value <= 65535) return new Port((short) value);
		throw new IllegalArgumentException();
	}
	public static @NotNull Port fromString(String value) {
		return of(Integer.parseInt(value));
	}
	public int get() {
		return value & 0xFFFF;
	}
	@Override
	public String toString() {
		return String.valueOf(get());
	}
}
