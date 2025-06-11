package com.forgestove.hexsync.util;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
public class HttpUtil {
	public static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
	public static final String DOWNLOAD = "download", LIST = "list";
	// 发送GET请求
	public static <T> HttpResponse<T> sendGet(String url, BodyHandler<T> bodyHandler) {
		try {
			return CLIENT.send(HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(3)).GET().build(), bodyHandler);
		} catch (Exception error) {
			Log.error("发送GET请求失败: %s, 错误: %s", url, error.getMessage());
			return null;
		}
	}
	// 获取HTTP请求的远程地址
	public static String getHostAddress(@NotNull HttpExchange exchange) {
		return exchange.getRemoteAddress().getAddress().getHostAddress();
	}
	// 地址格式化,转换为HTTP协议
	public static @NotNull String formatHTTP(@NotNull String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("http://") ? address : "http://" + address; // 添加HTTP协议头
	}
}
