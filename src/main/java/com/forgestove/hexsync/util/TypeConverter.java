package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;

import java.util.function.Function;
/**
 * 类型转换工具类，用于安全地将字符串转换为各种数据类型
 */
@SuppressWarnings("unused")
public class TypeConverter {
	/**
	 * 尝试将字符串转换为指定类型
	 *
	 * @param value     要转换的字符串
	 * @param converter 转换函数
	 * @param <T>       目标类型
	 * @return 转换后的值，如果转换失败则返回{@code null}
	 */
	@Contract("null, _ -> null")
	@Nullable
	public static <T> T tryConvert(String value, Function<String, T> converter) {
		if (value == null || value.trim().isEmpty()) return null;
		try {
			return converter.apply(value);
		} catch (Exception error) {
			Log.warn("Type conversion failed: " + error.getMessage());
			return null;
		}
	}
	/**
	 * 尝试将字符串转换为指定类型，如果失败则抛出异常
	 */
	@Contract("null, _ -> fail")
	@NotNull
	public static <T> T convertOrThrow(String value, Function<String, T> converter) throws IllegalArgumentException {
		if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Input string is null or empty");
		try {
			return converter.apply(value);
		} catch (Exception error) {
			throw new IllegalArgumentException("Failed to convert value: " + value, error);
		}
	}
	/**
	 * 尝试将字符串转换为指定类型，返回转换结果包装类
	 */
	@Contract("null, _ -> !null")
	public static <T> Result<T> tryConvertWithResult(String value, Function<String, T> converter) {
		if (value == null || value.trim().isEmpty()) return Result.failure("输入字符串为空");
		try {
			var result = converter.apply(value);
			return Result.success(result);
		} catch (Exception error) {
			return Result.failure("转换失败: " + error.getMessage());
		}
	}
	/**
	 * 尝试将字符串转换为整数，返回结果包装类
	 */
	@Contract("null -> !null")
	public static Result<Integer> tryToInt(String value) {
		return tryConvertWithResult(value, Integer::parseInt);
	}
	/**
	 * 尝试将字符串转换为长整型，返回结果包装类
	 */
	@Contract("null -> !null")
	public static Result<Long> tryToLong(String value) {
		return tryConvertWithResult(value, Long::parseLong);
	}
	/**
	 * 尝试将字符串转换为双精度浮点型，返回结果包装类
	 */
	@Contract("null -> !null")
	public static Result<Double> tryToDouble(String value) {
		return tryConvertWithResult(value, Double::parseDouble);
	}
	/**
	 * 尝试将字符串转换为布尔值，返回结果包装类
	 */
	@Contract("null -> !null")
	public static Result<Boolean> tryToBoolean(String value) {
		if (value == null || value.trim().isEmpty()) return Result.failure("输入字符串为空");
		value = value.toLowerCase().trim();
		if (value.equals("true") || value.equals("yes") || value.equals("1")) return Result.success(true);
		else if (value.equals("false") || value.equals("no") || value.equals("0")) return Result.success(false);
		else return Result.failure("无效的布尔值: " + value);
	}
	/**
	 * 尝试将字符串转换为枚举类型，返回结果包装类
	 */
	@Contract("null, _ -> !null")
	public static <T extends Enum<T>> Result<T> tryToEnum(String value, Class<T> enumType) {
		if (value == null || value.trim().isEmpty()) return Result.failure("输入字符串为空");
		try {
			return Result.success(Enum.valueOf(enumType, value.toUpperCase()));
		} catch (Exception error) {
			return Result.failure("无效的枚举值: " + value + "，对于类型: " + enumType.getSimpleName());
		}
	}
	/**
	 * 尝试将字符串转换为浮点型，返回结果包装类
	 */
	@Contract("null -> !null")
	public static Result<Float> tryToFloat(String value) {
		return tryConvertWithResult(value, Float::parseFloat);
	}
	/**
	 * 尝试将字符串转换为字节，返回结果包装类
	 */
	@Contract("null -> !null")
	public static Result<Byte> tryToByte(String value) {
		return tryConvertWithResult(value, Byte::parseByte);
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
