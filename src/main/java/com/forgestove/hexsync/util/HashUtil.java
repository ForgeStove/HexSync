package com.forgestove.hexsync.util;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
public class HashUtil {
	/** 文件路径和最后修改时间的组合键 -> SHA1 哈希值缓存 */
	public static final Object2ObjectMap<String, String> CACHE = new Object2ObjectOpenHashMap<>();
	/**
	 * 获取文件已缓存的SHA1哈希值
	 *
	 * @param file 文件对象
	 * @return SHA1哈希值，如果文件不存在或不是文件则返回null
	 */
	public static synchronized String getSHA1(@NotNull File file) {
		return CACHE.get(getID(file));
	}
	/**
	 * 将SHA1哈希值放入缓存
	 *
	 * @param file 文件对象
	 * @param sha1 SHA1哈希值
	 */
	public static synchronized void putSHA1(@NotNull File file, String sha1) {
		CACHE.put(getID(file), sha1);
	}
	/**
	 * 获取文件的唯一标识ID
	 * <p>
	 * 该方法根据文件的绝对路径和最后修改时间生成一个唯一标识字符串，用于在缓存中索引文件的SHA1哈希值。
	 *
	 * @param file 文件对象
	 * @return 由文件路径和最后修改时间组成的唯一标识字符串
	 */
	public static @NotNull String getID(@NotNull File file) {
		return file.getAbsolutePath() + ":" + file.lastModified();
	}
	/**
	 * 计算文件的SHA1哈希值，带缓存功能
	 * <p>
	 * 该方法首先检查内存缓存中是否已存在文件的哈希值，如果存在则直接返回缓存值。
	 * <p>
	 * 否则，会读取文件内容并计算SHA1哈希值，然后将结果存入缓存并返回。
	 *
	 * @param file 需要计算哈希值的文件
	 * @return 文件内容的SHA1哈希值（十六进制字符串格式）
	 * @throws RuntimeException 如果文件读取失败或哈希计算出错
	 */
	public static String calculateSHA1(File file) {
		var cache = getSHA1(file);
		if (cache != null) return cache;
		try (var fileInputStream = new FileInputStream(file)) {
			var digest = MessageDigest.getInstance("SHA-1");
			var buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) digest.update(buffer, 0, bytesRead);
			var sha1 = HexFormat.of().formatHex(digest.digest());
			putSHA1(file, sha1); // 写入缓存
			return sha1;
		} catch (Exception e) {
			throw new RuntimeException(e.getCause());
		}
	}
}
