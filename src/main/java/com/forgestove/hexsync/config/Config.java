package com.forgestove.hexsync.config;
/**
 * 配置值包装类，用于保存可变的配置值引用
 *
 * @param <T> 配置值的类型
 */
public class Config<T> {
	private T value;
	public Config(T value) {
		this.value = value;
	}
	public String toString() {
		return value.toString();
	}
	public T get() {
		return value;
	}
	public void set(T value) {
		this.value = value;
	}
}
