package com.forgestove.hexsync.config;
import org.jetbrains.annotations.*;

import java.util.function.*;
/**
 * 配置项基类接口，定义了将配置项转换为字符串数组的方法。
 */
public interface ConfigEntry {
	/**
	 * 将配置项转换为字符串数组。
	 *
	 * @return 包含配置项内容的字符串数组
	 */
	String[] toStringArray();
	/**
	 * 标题配置项，用于表示配置的分组或标题。
	 *
	 * @param header 标题内容
	 */
	record HeaderEntry(String header) implements ConfigEntry {
		/**
		 * 将标题配置项转换为字符串数组。
		 *
		 * @return 仅包含标题的字符串数组
		 */
		@Contract(value = " -> new", pure = true)
		public String @NotNull [] toStringArray() {return new String[]{header};}
	}
	/**
	 * 值配置项，包含键、获取器、设置器和序列化器。
	 *
	 * @param key        配置项的键名
	 * @param getter     获取配置项值的函数
	 * @param setter     设置配置项值的函数
	 * @param serializer 配置项值的序列化函数
	 * @param <T>        配置项的值类型
	 */
	record ValueEntry<T>(String key, Supplier<T> getter, Consumer<String> setter, Function<T, String> serializer) implements ConfigEntry {
		/**
		 * 创建一个需要解析的泛型配置项。
		 *
		 * @param key    配置项的键名
		 * @param config 泛型配置
		 * @param parser 从 {@link String} 转换到 {@link T} 的解析器
		 * @param <T>    配置项的值类型
		 * @return 创建的泛型配置项
		 * @implNote 默认使用 {@link T#toString()} 作为序列化器
		 */
		@Contract("_, _, _ -> new")
		public static <T> @NotNull ValueEntry<T> value(String key, @NotNull Config<T> config, Function<String, T> parser) {
			return value(key, config, parser, T::toString);
		}
		/**
		 * 创建一个需要解析和序列化的泛型配置项。
		 *
		 * @param key        配置项的键名
		 * @param config     泛型配置
		 * @param parser     从 {@link String} 转换到 {@link T} 的解析器
		 * @param serializer 从 {@link T} 转换到 {@link String} 的序列化器
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
		/**
		 * 将值配置项转换为字符串数组。
		 *
		 * @return 包含键和序列化后值的字符串数组
		 */
		@Contract(" -> new")
		public String @NotNull [] toStringArray() {
			return new String[]{key, serializer.apply(getter.get())};
		}
	}
}

