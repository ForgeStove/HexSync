package com.ForgeStove.HexSync.Server;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.*;

import static com.ForgeStove.HexSync.Server.ResponseSender.sendResponse;
import static com.ForgeStove.HexSync.Server.Server.serverMap;
import static com.ForgeStove.HexSync.Util.Config.*;
import static com.ForgeStove.HexSync.Util.Log.*;
import static java.io.File.separator;
import static java.nio.file.Files.newInputStream;
public class RequestHandler {
	// 处理请求
	public static void handleRequest(@NotNull HttpExchange exchange) {
		if (!exchange.getRequestMethod().equalsIgnoreCase(POST)) return;
		var requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/" + DOWNLOAD + "/")) {
			var requestCRC = Long.parseLong(requestURI.substring(requestURI.lastIndexOf("/") + 1));
			for (var entry : serverMap.entrySet())
				if (entry.getValue() == requestCRC) {
					var file = new File(String.format("%s%s%s", serverSyncDirectory, separator, entry.getKey()));
					try (var inputStream = new BufferedInputStream(newInputStream(file.toPath()))) {
						sendResponse(exchange, inputStream, file.length());
						log(INFO, "发送文件: " + file);
					} catch (IOException error) {
						log(SEVERE, "发送文件时出错: " + error.getMessage());
					}
					break;
				}
		} else if (requestURI.startsWith("/" + LIST)) {
			var responseBuilder = new StringBuilder();
			for (var entry : serverMap.entrySet())
				responseBuilder.append(String.format("%s%n%s%n", entry.getKey(), entry.getValue()));
			var bytes = responseBuilder.toString().getBytes();
			try (var inputStream = new ByteArrayInputStream(bytes)) {
				sendResponse(exchange, inputStream, bytes.length);
				log(INFO, "发送列表");
			} catch (IOException error) {
				log(SEVERE, "发送列表时出错: " + error.getMessage());
			}
		}
	}
}
