package com.forgestove.hexsync.util;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class HashCache {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>(); // 文件路径和最后修改时间的组合键 -> SHA1 哈希值缓存
	public static String getSHA1(File file) {
		if (file == null || !file.exists() || !file.isFile()) return null;
		return CACHE.get(file.getAbsolutePath() + ":" + file.lastModified());
	}
	public static void putSHA1(File file, String sha1) {
		if (file == null || sha1 == null) return;
		CACHE.put(file.getAbsolutePath() + ":" + file.lastModified(), sha1);
	}
}
