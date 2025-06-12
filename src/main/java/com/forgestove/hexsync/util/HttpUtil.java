package com.forgestove.hexsync.util;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
/**
 * HTTP 工具类，用于处理 HTTP 请求和相关工具方法
 */
public class HttpUtil {
	public static final String DOWNLOAD = "download", LIST = "list";
	private static volatile HttpClient httpClient;
	/**
	 * 获取 HttpClient 实例，使用惰性初始化
	 *
	 * @return HttpClient 实例
	 */
	public static synchronized HttpClient getClient() {
		if (httpClient == null) httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
		return httpClient;
	}
	/**
	 * 关闭并重置 HttpClient
	 */
	public static synchronized void resetClient() {
		httpClient = null;
	}
	/**
	 * 发送 GET 请求
	 *
	 * @param url         请求地址
	 * @param bodyHandler 响应体处理器
	 * @return HTTP 响应
	 * @throws IOException          如果发生 I/O 错误
	 * @throws InterruptedException 如果请求被中断
	 */
	public static <T> HttpResponse<T> sendGet(String url, BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
		return getClient().send(HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(3)).build(), bodyHandler);
	}
	/**
	 * 获取 HTTP 请求的远程地址
	 *
	 * @param exchange HTTP 交换对象
	 * @return 远程主机地址
	 */
	public static String getHostAddress(@NotNull HttpExchange exchange) {
		return exchange.getRemoteAddress().getAddress().getHostAddress();
	}
	/**
	 * 地址格式化，转换为 HTTP 协议
	 *
	 * @param address 需要格式化的地址
	 * @return 格式化后的地址
	 */
	public static @NotNull String formatHTTP(@NotNull String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("http://") ? address : "http://" + address; // 添加 HTTP 协议头
	}
}
