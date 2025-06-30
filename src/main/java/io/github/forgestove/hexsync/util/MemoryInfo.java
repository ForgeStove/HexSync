package io.github.forgestove.hexsync.util;
/**
 * 用于获取和展示当前JVM内存使用情况的工具类。
 * 该类提供了已使用内存、总内存、使用百分比以及格式化信息的字段。
 */
public class MemoryInfo {
	/** 已使用的内存量（以字节为单位） */
	public final long used;
	/** JVM分配的总内存量（以字节为单位） */
	public final long total;
	/** 内存使用百分比（0-100） */
	public final int percentage;
	/** 格式化后的内存使用信息，格式为："XX% (YMB/ZMB)" */
	public final String info;
	/**
	 * 构造函数，创建时会立即计算当前JVM的内存使用情况。
	 * 初始化所有内存相关字段（已用内存、总内存、使用百分比和格式化信息）。
	 */
	public MemoryInfo() {
		var runtime = Runtime.getRuntime();
		total = runtime.totalMemory();
		used = total - runtime.freeMemory();
		percentage = (int) (used * 100 / total);
		info = "%d%% (%dMB/%dMB)".formatted(percentage, used / 1024 / 1024, total / 1024 / 1024);
	}
}
