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
	@SuppressWarnings("unused")
	record ValueEntry<T>(String key, Supplier<T> getter, Consumer<String> setter, Function<T, String> serializer) implements ConfigEntry {
		/**
		 * 创建一个需要解析的泛型配置项
		 *
		 * @param key    配置项的键名
		 * @param config 泛型配置
		 * @param parser 从String转换到泛型T的解析器
		 * @param <T>    配置项的值类型
		 * @return 创建的泛型配置项
		 * @implNote 默认使用 T::toString 作为序列化器
		 */
		@Contract("_, _, _ -> new")
		public static <T> @NotNull ValueEntry<T> value(String key, @NotNull Config<T> config, Function<String, T> parser) {
			return value(key, config, parser, T::toString);
		}
		/**
		 * 创建一个需要解析和序列化的泛型配置项
		 *
		 * @param key        配置项的键名
		 * @param config     泛型配置
		 * @param parser     从String转换到泛型T的解析器
		 * @param serializer 从泛型T转换到String的序列化器
		 * @param <T>        配置项的值类型
		 * @return 创建的泛型配置项
		 */
		@Contract("_, _, _, _ -> new")
		public static <T> @NotNull ValueEntry<T> value(
			String key,
			@NotNull Config<T> config,
			Function<String, T> parser,
			Function<T, String> serializer
		) {
			return new ValueEntry<>(key, config::get, string -> config.set(parser.apply(string)), serializer);
		}
		@Contract(" -> new")
		public Object @NotNull [] toObjectArray() {
			var value = getter.get();
			return new Object[]{key, serializer != null ? serializer.apply(value) : value};
		}
	}
}
