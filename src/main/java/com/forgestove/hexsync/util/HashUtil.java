package com.forgestove.hexsync.util;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class HashUtil {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>(); // 文件路径和最后修改时间的组合键 -> SHA1 哈希值缓存
	/**
	 * 获取文件已缓存的SHA1哈希值
	 *
	 * @param file 文件对象
	 * @return SHA1哈希值，如果文件不存在或不是文件则返回null
	 */
	public static String getSHA1(@NotNull File file) {
		return CACHE.get(file.getAbsolutePath() + ":" + file.lastModified());
	}
	/**
	 * 将SHA1哈希值放入缓存
	 *
	 * @param file 文件对象
	 * @param sha1 SHA1哈希值
	 */
	public static void putSHA1(@NotNull File file, String sha1) {
		CACHE.put(file.getAbsolutePath() + ":" + file.lastModified(), sha1);
	}
	// 计算文件SHA1哈希值（带缓存）
	public static String calculateSHA1(File file) {
		var cache = getSHA1(file);
		if (cache != null) return cache;
		try (var fileInputStream = new FileInputStream(file)) {
			var digest = MessageDigest.getInstance("SHA-1");
			var buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) digest.update(buffer, 0, bytesRead);
			var sha1 = HexFormat.of().formatHex(digest.digest());
			putSHA1(file, sha1); // 写入缓存
			return sha1;
		} catch (Exception error) {
			throw new RuntimeException("SHA1计算错误", error.getCause());
		}
	}
}
