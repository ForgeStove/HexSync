package com.forgestove.hexsync.config;
import org.jetbrains.annotations.*;

import java.util.function.*;
// 配置项基类
public interface ConfigEntry {
	Object[] toObjectArray();
	// 标题配置项
	record HeaderEntry(String header) implements ConfigEntry {
		@Contract(value = " -> new", pure = true)
		public Object @NotNull [] toObjectArray() {return new Object[]{header};}
	}
	// 值配置项
	record ValueEntry<T>(String key, Supplier<T> getter, Consumer<String> setter) implements ConfigEntry {
		@Contract(" -> new")
		public Object @NotNull [] toObjectArray() {return new Object[]{key, getter.get()};}
	}
}
