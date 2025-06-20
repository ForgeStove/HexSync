package com.forgestove.hexsync.server;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.Log;
import com.forgestove.hexsync.util.network.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import it.unimi.dsi.fastutil.io.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.StringJoiner;
/**
 * 请求处理器类，负责处理来自 HttpExchange 的请求，包括文件下载和文件列表请求。
 */
public class RequestHandler {
	/**
	 * 处理传入的 HTTP 请求，根据 URI 路径分发到相应的处理方法。
	 *
	 * @param exchange 当前的 HTTP 交换对象
	 */
	public static void handleRequest(@NotNull HttpExchange exchange) {
		var requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/" + HttpUtil.DOWNLOAD + "/")) sendFile(exchange, requestURI);
		else if (requestURI.startsWith("/" + HttpUtil.LIST)) sendList(exchange);
	}
	/**
	 * 根据请求的 SHA1，从服务器映射中查找对应文件并发送给客户端。
	 *
	 * @param exchange   当前的 HTTP 交换对象
	 * @param requestURI 请求的 URI 路径
	 */
	public static void sendFile(HttpExchange exchange, @NotNull String requestURI) {
		var requestSHA1 = requestURI.substring(requestURI.lastIndexOf("/") + 1);
		var fileName = Server.serverMap.object2ObjectEntrySet()
			.stream()
			.filter(entry -> entry.getValue().equals(requestSHA1))
			.findFirst()
			.map(Object2ObjectMap.Entry::getKey)
			.orElse(null);
		if (fileName == null) return;
		var file = Data.serverSyncPath.get().resolve(fileName).toFile();
		InputStream in;
		try {
			in = Files.newInputStream(file.toPath());
		} catch (IOException e) {
			Log.error("读取文件时出错: " + e.getMessage());
			return;
		}
		ResponseSender.sendResponse(exchange, new FastBufferedInputStream(in), file.length(), "application/java-archive");
		Log.info("发送文件: %s 至: %s".formatted(file, HttpUtil.getHostAddress(exchange)));
	}
	/**
	 * 发送服务器文件列表到客户端，每行为文件名和对应的 SHA1。
	 *
	 * @param exchange 当前的 HTTP 交换对象
	 */
	public static void sendList(@NotNull HttpExchange exchange) {
		var separator = System.lineSeparator();
		var joiner = new StringJoiner(separator);
		Server.serverMap.object2ObjectEntrySet().forEach(entry -> joiner.add(entry.getKey() + separator + entry.getValue()));
		var bytes = joiner.toString().getBytes();
		ResponseSender.sendResponse(exchange, new FastByteArrayInputStream(bytes), bytes.length, "application/json");
		Log.info("发送列表至: " + HttpUtil.getHostAddress(exchange));
	}
}
