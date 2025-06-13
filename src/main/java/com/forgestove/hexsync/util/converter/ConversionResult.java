package com.forgestove.hexsync.util.converter;
import org.jetbrains.annotations.*;
/**
 * 类型转换结果包装类
 */
public class ConversionResult<T> {
	public final boolean isSuccess;
	public final T value;
	public final String errorMessage;
	@Contract(pure = true)
	private ConversionResult(boolean isSuccess, T value, String errorMessage) {
		this.isSuccess = isSuccess;
		this.value = value;
		this.errorMessage = errorMessage;
	}
	@Contract(value = "_ -> new", pure = true)
	public static <T> @NotNull ConversionResult<T> success(T value) {
		return new ConversionResult<>(true, value, null);
	}
	@Contract(value = "_ -> new", pure = true)
	public static <T> @NotNull ConversionResult<T> failure(String errorMessage) {
		return new ConversionResult<>(false, null, errorMessage);
	}
}