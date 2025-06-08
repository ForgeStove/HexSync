package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;

import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
public class ModAPI {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>(); // SHA1 -> URL 缓存
	public static @Nullable String getURL(@NotNull String SHA1) {
		// 缓存命中直接返回
		if (CACHE.containsKey(SHA1)) return CACHE.get(SHA1);
		try {
			var requestUrl = ("https://api.modrinth.com/v2/version_file/" + SHA1).replaceAll("\"", "%22");
			var request = HttpRequest.newBuilder().uri(URI.create(requestUrl)).timeout(Duration.ofSeconds(5)).GET().build();
			var response = HttpUtil.CLIENT.send(request, BodyHandlers.ofString());
			if (response.statusCode() != HttpURLConnection.HTTP_OK) return null;
			var pattern = Pattern.compile("\"url\":\"(https://cdn\\.modrinth\\.com/[^\"]+)\""); // 简单提取 url 字段
			var matcher = pattern.matcher(response.body());
			if (!matcher.find()) return null;
			var url = matcher.group(1);
			CACHE.put(SHA1, url); // 写入缓存
			return url;
		} catch (Exception error) {
			Log.error(error.getMessage());
			return null;
		}
	}
}
