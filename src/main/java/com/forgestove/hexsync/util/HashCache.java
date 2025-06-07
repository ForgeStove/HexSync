package com.forgestove.hexsync.util;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class HashCache {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
	public static String getSHA1(File file) {
		if (file == null || !file.exists() || !file.isFile()) return null;
		var key = file.getAbsolutePath() + ":" + file.lastModified();
		return CACHE.get(key);
	}
	public static void putSHA1(File file, String sha1) {
		if (file == null || sha1 == null) return;
		var key = file.getAbsolutePath() + ":" + file.lastModified();
		CACHE.put(key, sha1);
	}
}
