package com.forgestove.hexsync.server;
import com.forgestove.hexsync.util.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.net.HttpURLConnection;
public class ResponseSender {
	// 发送数据
	public static void sendResponse(HttpExchange exchange, InputStream inputStream, long responseBytesLength) {
		try (var outputStream = exchange.getResponseBody()) {
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytesLength); // 设置响应头
			var buffer = new byte[16384];
			long totalBytesSent = 0; // 记录已发送字节数
			if (Config.serverUploadRateLimit == 0) { // 无限制
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1 && totalBytesSent < responseBytesLength) {
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					outputStream.flush();
					totalBytesSent += bytesRead;
				}
				return;
			}
			var lastFillTime = System.currentTimeMillis(); // 最近一次填充时间
			while (totalBytesSent < responseBytesLength) {
				var bytesToSend = (int) Math.min(16384, responseBytesLength - totalBytesSent);
				refillTokens(lastFillTime);
				if (Server.AVAILABLE_TOKENS.get() < bytesToSend) {
					var sleepMillis = (bytesToSend - Server.AVAILABLE_TOKENS.get()) * 1000L / Config.maxUploadRateInBytes;
					try {
						Thread.sleep(Math.max(sleepMillis, 1));
					} catch (InterruptedException error) {
						Thread.currentThread().interrupt();
						Log.error("发送响应时线程被中断: " + error.getMessage());
						break;
					}
					continue;
				}
				var bytesRead = inputStream.read(buffer, 0, bytesToSend);
				if (bytesRead == -1) break;
				outputStream.write(buffer, 0, bytesRead); // 写入数据
				outputStream.flush();
				totalBytesSent += bytesRead; // 更新已发送字节数
				Server.AVAILABLE_TOKENS.addAndGet(-bytesRead); // 减少可用令牌
				lastFillTime = System.currentTimeMillis(); // 仅在有速率限制时更新
			}
		} catch (Exception error) {
			Log.error("发送响应时出错: " + error);
		}
	}
	// 令牌桶补充逻辑，带最大值限制
	private static void refillTokens(long lastFillTime) {
		var tokensToAdd = (System.currentTimeMillis() - lastFillTime) * Config.maxUploadRateInBytes / 1000;
		if (tokensToAdd <= 0) return;
		var maxTokens = Config.maxUploadRateInBytes * 2L; // 令牌最大值为带宽2秒
		var newTokens = Math.min(Server.AVAILABLE_TOKENS.get() + tokensToAdd, maxTokens);
		Server.AVAILABLE_TOKENS.set(newTokens);
	}
}
