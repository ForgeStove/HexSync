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
		if (requestURI.startsWith("/" + Config.DOWNLOAD + "/")) {
			var requestSHA1 = requestURI.substring(requestURI.lastIndexOf("/") + 1);
			for (var entry : Server.serverMap.entrySet())
				if (entry.getValue().equals(requestSHA1)) {
					var file = new File(String.format("%s%s%s", Config.serverSyncDirectory, File.separator, entry.getKey()));
					try (var inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
						ResponseSender.sendResponse(exchange, inputStream, file.length());
						Log.info("发送文件: " + file);
					} catch (IOException error) {
						Log.error("发送文件时出错: " + error.getMessage());
					}
					break;
				}
		} else if (requestURI.startsWith("/" + Config.LIST)) {
			// 支持GET和POST请求
			if (HttpUtil.POST.equalsIgnoreCase(exchange.getRequestMethod()) || HttpUtil.GET.equalsIgnoreCase(exchange.getRequestMethod())) {
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
			} else try (exchange) {
				exchange.sendResponseHeaders(405, -1);
			} catch (IOException e) {
				Log.error("发送405错误时出错: " + e.getMessage());
			}
		}
	}
}
