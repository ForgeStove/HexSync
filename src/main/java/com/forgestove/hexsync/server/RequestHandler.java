package com.forgestove.hexsync.server;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.Log;
import com.forgestove.hexsync.util.network.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.Map.Entry;
import java.util.StringJoiner;
public class RequestHandler {
	// 处理请求
	public static void handleRequest(@NotNull HttpExchange exchange) {
		var requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/" + HttpUtil.DOWNLOAD + "/")) sendFile(exchange, requestURI);
		else if (requestURI.startsWith("/" + HttpUtil.LIST)) sendList(exchange);
	}
	public static void sendFile(HttpExchange exchange, @NotNull String requestURI) {
		var requestSHA1 = requestURI.substring(requestURI.lastIndexOf("/") + 1);
		var fileName = Server.serverMap.entrySet()
			.stream()
			.filter(entry -> entry.getValue().equals(requestSHA1))
			.findFirst()
			.map(Entry::getKey)
			.orElse(null);
		if (fileName == null) return;
		var file = Data.serverSyncPath.get().resolve(fileName).toFile();
		try (var inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
			ResponseSender.sendResponse(exchange, inputStream, file.length());
			Log.info("发送文件: %s 至: %s".formatted(file, HttpUtil.getHostAddress(exchange)));
		} catch (IOException error) {
			Log.error("发送文件时出错: " + error.getMessage());
		}
	}
	public static void sendList(@NotNull HttpExchange exchange) {
		var separator = System.lineSeparator();
		var joiner = new StringJoiner(separator);
		Server.serverMap.entrySet().stream().map(entry -> entry.getKey() + separator + entry.getValue()).forEach(joiner::add);
		var bytes = joiner.toString().getBytes();
		try (var inputStream = new ByteArrayInputStream(bytes)) {
			ResponseSender.sendResponse(exchange, inputStream, bytes.length);
			Log.info("发送列表至: " + HttpUtil.getHostAddress(exchange));
		} catch (IOException error) {
			Log.error("发送列表至 %s 时出错: %s".formatted(HttpUtil.getHostAddress(exchange), error.getMessage()));
		}
	}
}
