package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
public class ModAPI {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>(); // SHA1 -> URL 缓存
	public static @Nullable String getURL(@NotNull String SHA1) {
		// 缓存命中直接返回
		if (CACHE.containsKey(SHA1)) return CACHE.get(SHA1);
		HttpResponse<String> response;
		try {
			response = HttpUtil.sendGet(("https://api.modrinth.com/v2/version_file/" + SHA1).replaceAll("\"", "%22"),
				BodyHandlers.ofString());
		} catch (Exception error) {
			Log.error("获取 Modrinth 资源时出错: " + error.getMessage());
			return null;
		}
		if (response.statusCode() != HttpURLConnection.HTTP_OK) return null;
		var pattern = Pattern.compile("\"url\":\"(https://cdn\\.modrinth\\.com/[^\"]+)\""); // 简单提取 url 字段
		var matcher = pattern.matcher(response.body());
		if (!matcher.find()) return null;
		var url = matcher.group(1);
		CACHE.put(SHA1, url); // 写入缓存
		return url;
	}
}
