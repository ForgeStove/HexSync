package com.forgestove.hexsync.server;
import com.forgestove.hexsync.util.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.net.HttpURLConnection;
public class ResponseSender {
	// 发送数据
	public static void sendResponse(HttpExchange exchange, InputStream inputStream, long responseBytesLength) {
		try (inputStream; var outputStream = exchange.getResponseBody()) {
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytesLength); // 设置响应头
			var buffer = new byte[16384];
			long totalBytesSent = 0; // 记录已发送字节数
			var lastFillTime = System.currentTimeMillis(); // 最近一次填充时间
			while (totalBytesSent < responseBytesLength) {
				if (Config.serverUploadRateLimit == 0) { // 无限制
					var bytesRead = inputStream.read(buffer);
					if (bytesRead == -1) break;
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					continue;
				}
				var currentTime = System.currentTimeMillis();
				Server.AVAILABLE_TOKENS.addAndGet((currentTime - lastFillTime) * Config.maxUploadRateInBytes / 1000);
				lastFillTime = currentTime; // 更新时间
				var bytesToSend = Math.min(16384, responseBytesLength - totalBytesSent);
				if (Server.AVAILABLE_TOKENS.get() >= bytesToSend) {
					var bytesRead = inputStream.read(buffer, 0, (int) bytesToSend);
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					totalBytesSent += bytesRead; // 更新已发送字节数
					Server.AVAILABLE_TOKENS.addAndGet(-bytesRead); // 减少可用令牌
				} else Thread.sleep((bytesToSend - Server.AVAILABLE_TOKENS.get()) * 1000 / Config.maxUploadRateInBytes);
			}
		} catch (Exception error) {
			Log.error("发送响应时出错: " + error.getMessage());
		}
	}
}
