package io.github.forgestove.hexsync.config;
import org.jetbrains.annotations.*;

import java.util.function.*;
/**
 * 配置项基类接口，定义了将配置项转换为字符串的方法。
 */
public interface ConfigEntry {
	/**
	 * 将配置项转换为字符串。
	 *
	 * @return 包含配置项内容的字符串
	 */
	String toString();
	/**
	 * 标题配置项，用于表示配置的分组或标题。
	 *
	 * @param header 标题内容
	 */
	record Header(String header) implements ConfigEntry {
		/**
		 * 将标题配置项转换为字符串。
		 *
		 * @return 仅包含标题的字符串
		 */
		@Contract(
			value = " -> new",
			pure = true
		)
		public @NotNull String toString() {return header;}
	}
	/**
	 * 值配置项，包含键、Config对象、解析器和序列化器。
	 *
	 * @param key        配置项的键名
	 * @param config     泛型配置
	 * @param parser     从 {@link String} 转换到 {@link T} 的解析器
	 * @param serializer 从 {@link T} 转换到 {@link String} 的序列化器
	 * @param <T>        配置项的值类型
	 */
	record Value<T>(String key, Config<T> config, Function<String, T> parser, Function<T, String> serializer)
		implements ConfigEntry, Consumer<String> {
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
		public static <T> @NotNull Value<T> of(String key, @NotNull Config<T> config, Function<String, T> parser) {
			return of(key, config, parser, Object::toString);
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
		public static <T> @NotNull Value<T> of(
			String key,
			@NotNull Config<T> config,
			@NotNull Function<String, T> parser,
			@NotNull Function<T, String> serializer
		) {
			return new Value<>(key, config, parser, serializer);
		}
		/**
		 * 将值配置项转换为字符串。
		 *
		 * @return 包含键和序列化后值的字符串
		 */
		@Contract(" -> new")
		public @NotNull String toString() {
			var value = config.get();
			return key + "=" + (value == null ? "" : serializer.apply(value));
		}
		/**
		 * 设置配置项的值（从字符串）。
		 *
		 * @param value 字符串值
		 */
		@Override
		public void accept(String value) {
			config().set(parser().apply(value));
		}
	}
}
