package com.forgestove.hexsync.config;
/**
 * 通用配置类，用于包装和管理可变的配置值。
 *
 * @param <T> 配置值的类型
 */
public class Config<T> {
	/**
	 * 保存配置值的字段。
	 */
	private T value;
	/**
	 * 构造方法，用于初始化配置值。
	 *
	 * @param value 初始配置值
	 */
	public Config(T value) {
		this.value = value;
	}
	/**
	 * 返回配置值的字符串表示形式。
	 *
	 * @return 配置值的字符串表示
	 */
	public String toString() {
		return value.toString();
	}
	/**
	 * 获取当前配置值。
	 *
	 * @return 当前配置值
	 */
	public T get() {
		return value;
	}
	/**
	 * 设置新的配置值。
	 *
	 * @param value 新的配置值
	 */
	public void set(T value) {
		this.value = value;
	}
}
