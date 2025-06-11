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
		@Contract("_, _, _ -> new")
		public static <T> @NotNull ValueEntry<T> value(String key, @NotNull Config<T> config, Function<String, T> parser) {
			return new ValueEntry<>(key, config::get, v -> config.set(parser.apply(v)));
		}
		@Contract("_, _ -> new")
		public static @NotNull ValueEntry<String> value(String key, @NotNull Config<String> config) {
			return new ValueEntry<>(key, config::get, config::set);
		}
		@Contract(" -> new")
		public Object @NotNull [] toObjectArray() {return new Object[]{key, getter.get()};}
	}
}
