package com.forgestove.hexsync.util;
import org.jetbrains.annotations.*;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class ModAPI {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>(); // SHA1 -> URL 缓存
	public static @Nullable String getURL(@NotNull String SHA1) {
		if (CACHE.containsKey(SHA1)) return CACHE.get(SHA1); // 缓存命中直接返回
		try {
			var response = HttpUtil.sendGet(("https://api.modrinth.com/v2/version_file/" + SHA1).replaceAll("\"", "%22"),
				BodyHandlers.ofString());
			if (response.statusCode() != HttpURLConnection.HTTP_OK) return null;
			var jsonObject = new JSONObject(response.body());
			if (jsonObject.has("files") && !jsonObject.getJSONArray("files").isEmpty()) {
				var file = jsonObject.getJSONArray("files").getJSONObject(0);
				var url = file.getString("url");
				CACHE.put(SHA1, url); // 写入缓存
				return url;
			}
		} catch (Exception error) {
			Log.error("获取 Modrinth 资源时出错: " + error.getMessage());
		}
		return null;
	}
}
