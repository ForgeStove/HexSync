package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;

import java.util.function.Function;
/**
 * 类型转换工具类，用于安全地将字符串转换为各种数据类型
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Converter {
	/**
	 * 尝试将字符串转换为指定类型，如果失败则抛出异常
	 */
	@Contract("null, _ -> fail")
	public static <T> @NotNull Result<T> toOrThrow(String value, Function<String, T> converter) throws IllegalArgumentException {
		try {
			return Result.success(converter.apply(value));
		} catch (Throwable error) {
			throw new IllegalArgumentException(error.getMessage());
		}
	}
	/**
	 * 尝试将字符串转换为指定类型
	 */
	@Contract("null, _ -> !null")
	public static <T> Result<T> to(String value, Function<String, T> converter) {
		try {
			return Result.success(converter.apply(value));
		} catch (Throwable error) {
			return Result.failure(error.getMessage());
		}
	}
	/**
	 * 尝试将指定类型转换为字符串
	 */
	@Contract("null, _ -> !null")
	public static <T> Result<String> from(T value, Function<T, String> converter) {
		try {
			return Result.success(converter.apply(value));
		} catch (Throwable error) {
			return Result.failure(error.getMessage());
		}
	}
	/**
	 * 获取转换值，如果转换失败则返回默认值
	 */
	@Contract(pure = true)
	public static <T> T getValueOrDefault(@NotNull Result<T> result, T defaultValue) {
		return result.isSuccess ? result.value : defaultValue;
	}
	/**
	 * 类型转换结果包装类。<p>
	 * 此记录类封装了类型转换操作的结果
	 *
	 * @param isSuccess    转换是否成功
	 * @param value        转换后的值，转换失败时为{@code null}
	 * @param errorMessage 错误信息，转换成功时为{@code null}
	 * @param <T>          转换结果的类型
	 */
	public record Result<T>(boolean isSuccess, T value, String errorMessage) {
		/**
		 * 创建一个成功的类型转换结果对象。
		 *
		 * @param value 转换后的值
		 * @param <T>   转换结果的类型
		 * @return 成功的类型转换结果对象
		 */
		@Contract(value = "_ -> new", pure = true)
		public static <T> @NotNull Result<T> success(T value) {
			return new Result<>(true, value, null);
		}
		/**
		 * 创建一个失败的类型转换结果对象。
		 *
		 * @param errorMessage 错误信息
		 * @param <T>          转换结果的类型
		 * @return 失败的类型转换结果对象
		 */
		@Contract(value = "_ -> new", pure = true)
		public static <T> @NotNull Result<T> failure(String errorMessage) {
			return new Result<>(false, null, errorMessage);
		}
	}
}
