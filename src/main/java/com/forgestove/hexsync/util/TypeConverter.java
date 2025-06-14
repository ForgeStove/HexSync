package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;

import java.util.function.Function;
/**
 * 类型转换工具类，用于安全地将字符串转换为各种数据类型
 */
@SuppressWarnings("unused")
public class TypeConverter {
	/**
	 * 将字符串转换为指定类型
	 *
	 * @param value        要转换的字符串
	 * @param converter    转换函数
	 * @param defaultValue 转换失败时的默认值
	 * @param <T>          目标类型
	 * @return 转换后的值，如果转换失败则返回默认值
	 */
	@Contract("null, _, _ -> param3")
	public static <T> T convert(String value, Function<String, T> converter, T defaultValue) {
		if (value == null || value.trim().isEmpty()) return defaultValue;
		try {
			return converter.apply(value);
		} catch (Exception e) {
			Log.warn("Type conversion failed: " + e.getMessage());
			return defaultValue;
		}
	}
	/**
	 * 将字符串转换为整数
	 */
	@Contract("null, _ -> param2")
	public static int toInt(String value, int defaultValue) {
		return convert(value, Integer::parseInt, defaultValue);
	}
	/**
	 * 将字符串转换为长整型
	 */
	@Contract("null, _ -> param2")
	public static long toLong(String value, long defaultValue) {
		return convert(value, Long::parseLong, defaultValue);
	}
	/**
	 * 将字符串转换为双精度浮点型
	 */
	@Contract("null, _ -> param2")
	public static double toDouble(String value, double defaultValue) {
		return convert(value, Double::parseDouble, defaultValue);
	}
	/**
	 * 将字符串转换为布尔型
	 */
	@Contract("null, true -> true; null, false -> false")
	public static boolean toBoolean(String value, boolean defaultValue) {
		if (value == null || value.trim().isEmpty()) return defaultValue;
		value = value.toLowerCase().trim();
		if (value.equals("true") || value.equals("yes") || value.equals("1")) return true;
		else if (value.equals("false") || value.equals("no") || value.equals("0")) return false;
		else return defaultValue;
	}
	/**
	 * 将字符串转换为枚举类型
	 */
	@Contract("null, _, _ -> param3")
	public static <T extends Enum<T>> T toEnum(String value, Class<T> enumType, T defaultValue) {
		if (value == null || value.trim().isEmpty()) return defaultValue;
		try {
			return Enum.valueOf(enumType, value.toUpperCase());
		} catch (IllegalArgumentException e) {
			Log.warn("Invalid enum value: " + value + " for type " + enumType.getSimpleName());
			return defaultValue;
		}
	}
	/**
	 * 尝试将字符串转换为指定类型，如果失败则返回null
	 */
	@Contract("null, _ -> null")
	@Nullable
	public static <T> T tryConvert(String value, Function<String, T> converter) {
		return convert(value, converter, null);
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
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to convert value: " + value, e);
		}
	}
	/**
	 * 尝试将字符串转换为指定类型，返回转换结果包装类
	 */
	@Contract("null, _ -> !null")
	public static <T> ConversionResult<T> tryConvertWithResult(String value, Function<String, T> converter) {
		if (value == null || value.trim().isEmpty()) return ConversionResult.failure("输入字符串为空");
		try {
			var result = converter.apply(value);
			return ConversionResult.success(result);
		} catch (Exception e) {
			return ConversionResult.failure("转换失败: " + e.getMessage());
		}
	}
	/**
	 * 尝试将字符串转换为整数，返回结果包装类
	 */
	@Contract("null -> !null")
	public static ConversionResult<Integer> tryToInt(String value) {
		return tryConvertWithResult(value, Integer::parseInt);
	}
	/**
	 * 尝试将字符串转换为长整型，返回结果包装类
	 */
	@Contract("null -> !null")
	public static ConversionResult<Long> tryToLong(String value) {
		return tryConvertWithResult(value, Long::parseLong);
	}
	/**
	 * 尝试将字符串转换为双精度浮点型，返回结果包装类
	 */
	@Contract("null -> !null")
	public static ConversionResult<Double> tryToDouble(String value) {
		return tryConvertWithResult(value, Double::parseDouble);
	}
	/**
	 * 尝试将字符串转换为布尔值，返回结果包装类
	 */
	@Contract("null -> !null")
	public static ConversionResult<Boolean> tryToBoolean(String value) {
		if (value == null || value.trim().isEmpty()) return ConversionResult.failure("输入字符串为空");
		value = value.toLowerCase().trim();
		if (value.equals("true") || value.equals("yes") || value.equals("1")) return ConversionResult.success(true);
		else if (value.equals("false") || value.equals("no") || value.equals("0")) return ConversionResult.success(false);
		else return ConversionResult.failure("无效的布尔值: " + value);
	}
	/**
	 * 尝试将字符串转换为枚举类型，返回结果包装类
	 */
	@Contract("null, _ -> !null")
	public static <T extends Enum<T>> ConversionResult<T> tryToEnum(String value, Class<T> enumType) {
		if (value == null || value.trim().isEmpty()) return ConversionResult.failure("输入字符串为空");
		try {
			return ConversionResult.success(Enum.valueOf(enumType, value.toUpperCase()));
		} catch (IllegalArgumentException e) {
			return ConversionResult.failure("无效的枚举值: " + value + "，对于类型: " + enumType.getSimpleName());
		}
	}
	/**
	 * 尝试将字符串转换为浮点型，返回结果包装类
	 */
	@Contract("null -> !null")
	public static ConversionResult<Float> tryToFloat(String value) {
		return tryConvertWithResult(value, Float::parseFloat);
	}
	/**
	 * 尝试将字符串转换为字节，返回结果包装类
	 */
	@Contract("null -> !null")
	public static ConversionResult<Byte> tryToByte(String value) {
		return tryConvertWithResult(value, Byte::parseByte);
	}
	/**
	 * 获取转换值，如果转换失败则返回默认值
	 */
	@Contract(pure = true)
	public static <T> T getValueOrDefault(@NotNull ConversionResult<T> result, T defaultValue) {
		return result.isSuccess ? result.value : defaultValue;
	}
	/**
	 * 类型转换结果包装类，用于封装类型转换的结果。
	 * 包括转换是否成功、转换后的值以及错误信息。
	 *
	 * @param <T> 转换结果的类型
	 */
	public static class ConversionResult<T> {
		/**
		 * 表示转换是否成功。
		 */
		public final boolean isSuccess;
		/**
		 * 转换后的值。如果转换失败，则为null。
		 */
		public final T value;
		/**
		 * 错误信息。如果转换成功，则为null。
		 */
		public final String errorMessage;
		/**
		 * 构造函数，用于创建一个类型转换结果对象。
		 *
		 * @param isSuccess    转换是否成功
		 * @param value        转换后的值
		 * @param errorMessage 错误信息
		 */
		@Contract(pure = true)
		private ConversionResult(boolean isSuccess, T value, String errorMessage) {
			this.isSuccess = isSuccess;
			this.value = value;
			this.errorMessage = errorMessage;
		}
		/**
		 * 创建一个成功的类型转换结果对象。
		 *
		 * @param value 转换后的值
		 * @param <T>   转换结果的类型
		 * @return 成功的类型转换结果对象
		 */
		@Contract(value = "_ -> new", pure = true)
		public static <T> @NotNull ConversionResult<T> success(T value) {
			return new ConversionResult<>(true, value, null);
		}
		/**
		 * 创建一个失败的类型转换结果对象。
		 *
		 * @param errorMessage 错误信息
		 * @param <T>          转换结果的类型
		 * @return 失败的类型转换结果对象
		 */
		@Contract(value = "_ -> new", pure = true)
		public static <T> @NotNull ConversionResult<T> failure(String errorMessage) {
			return new ConversionResult<>(false, null, errorMessage);
		}
	}
}
