package com.ForgeStove.HexSync.Server;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.util.Map;

import static com.ForgeStove.HexSync.Server.ResponseSender.sendResponse;
import static com.ForgeStove.HexSync.Server.Server.serverMap;
import static com.ForgeStove.HexSync.Util.Config.*;
import static com.ForgeStove.HexSync.Util.Log.*;
import static java.io.File.separator;
import static java.nio.file.Files.newInputStream;
public class RequestHandler {
	// 处理请求
	public static void handleRequest(HttpExchange exchange) {
		if (!exchange.getRequestMethod().equalsIgnoreCase(POST)) return;
		String requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/" + DOWNLOAD + "/")) {
			long requestCRC = Long.parseLong(requestURI.substring(requestURI.lastIndexOf("/") + 1));
			String filePath = null;
			for (Map.Entry<String, Long> entry : serverMap.entrySet())
				if (entry.getValue() == requestCRC) {
					filePath = String.format("%s%s%s", serverSyncDirectory, separator, entry.getKey());
					break;
				}
			if (filePath == null) return;
			File file = new File(filePath);
			try (InputStream inputStream = new BufferedInputStream(newInputStream(file.toPath()))) {
				sendResponse(exchange, inputStream, file.length());
				log(INFO, "发送文件: " + file);
			} catch (IOException error) {
				log(SEVERE, "发送文件时出错: " + error.getMessage());
			}
		} else if (requestURI.startsWith("/" + LIST)) {
			StringBuilder responseBuilder = new StringBuilder();
			for (Map.Entry<String, Long> entry : serverMap.entrySet())
				responseBuilder.append(String.format("%s%n%s%n", entry.getKey(), entry.getValue()));
			byte[] bytes = responseBuilder.toString().getBytes();
			try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
				sendResponse(exchange, inputStream, bytes.length);
				log(INFO, "发送列表");
			} catch (IOException error) {
				log(SEVERE, "发送列表时出错: " + error.getMessage());
			}
		}
	}
}
