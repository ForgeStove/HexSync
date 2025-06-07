package com.forgestove.hexsync.server;
import com.forgestove.hexsync.util.*;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
public class RequestHandler {
	// 处理请求
	public static void handleRequest(@NotNull HttpExchange exchange) {
		var requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/" + Config.DOWNLOAD + "/")) sendFile(exchange, requestURI);
		else if (requestURI.startsWith("/" + Config.LIST)) sendList(exchange);
	}
	public static void sendFile(HttpExchange exchange, @NotNull String requestURI) {
		var requestSHA1 = requestURI.substring(requestURI.lastIndexOf("/") + 1);
		for (var entry : Server.serverMap.entrySet()) {
			if (!entry.getValue().equals(requestSHA1)) continue;
			var file = new File("%s%s%s".formatted(Config.serverSyncDirectory, File.separator, entry.getKey()));
			try (var inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
				ResponseSender.sendResponse(exchange, inputStream, file.length());
				Log.info("发送文件: " + file);
			} catch (IOException error) {
				Log.error("发送文件时出错: " + error.getMessage());
			}
			break;
		}
	}
	public static void sendList(@NotNull HttpExchange exchange) {
		var responseBuilder = new StringBuilder();
		for (var entry : Server.serverMap.entrySet())
			responseBuilder.append(String.format("%s%n%s%n", entry.getKey(), entry.getValue()));
		var bytes = responseBuilder.toString().getBytes();
		try (var inputStream = new ByteArrayInputStream(bytes)) {
			ResponseSender.sendResponse(exchange, inputStream, bytes.length);
			Log.info("发送列表");
		} catch (IOException error) {
			Log.error("发送列表时出错: " + error.getMessage());
		}
	}
}
