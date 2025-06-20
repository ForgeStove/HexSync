package com.forgestove.hexsync.server;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.Log;
import com.forgestove.hexsync.util.network.Rate;
import com.sun.net.httpserver.HttpExchange;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
/**
 * 响应发送器，负责处理HTTP响应发送，支持速率限制
 */
public class ResponseSender {
	private static final int BUFFER_SIZE = 8192;
	private static final AtomicLong availableTokens = new AtomicLong(0); // 当前可用令牌数量
	private static volatile long lastRefillTime = Instant.now().toEpochMilli(); // 最近一次令牌补充时间
	/**
	 * 发送HTTP响应
	 *
	 * @param exchange            HTTP交换对象
	 * @param inputStream         输入流
	 * @param responseBytesLength 响应体长度
	 */
	public static void sendResponse(@NotNull HttpExchange exchange, InputStream inputStream, long responseBytesLength,
		String contentType) {
		try (var in = inputStream; var outputStream = exchange.getResponseBody()) {
			exchange.getResponseHeaders().set("Content-Type", contentType);
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytesLength);
			var buffer = new byte[BUFFER_SIZE];
			long totalBytesSent = 0; // 记录已发送字节数
			if (Data.serverUploadRate.get().value == 0) { // 无限制
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1 && totalBytesSent < responseBytesLength) {
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					outputStream.flush();
					totalBytesSent += bytesRead;
				}
				return;
			}
			while (totalBytesSent < responseBytesLength) {
				var bytesToSend = (int) Math.min(BUFFER_SIZE, responseBytesLength - totalBytesSent);
				refillTokens();
				if (availableTokens.get() < bytesToSend) {
					var sleepMillis = (bytesToSend - availableTokens.get()) * 1000L / Data.serverUploadRate.get().bps;
					try {
						Thread.sleep(Math.max(sleepMillis, 1));
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						Log.error("发送响应时线程被中断: " + e.getMessage());
						break;
					}
					continue;
				}
				var bytesRead = in.read(buffer, 0, bytesToSend);
				if (bytesRead == -1) break;
				outputStream.write(buffer, 0, bytesRead); // 写入数据
				outputStream.flush();
				totalBytesSent += bytesRead; // 更新已发送字节数
				availableTokens.addAndGet(-bytesRead); // 减少可用令牌
			}
		} catch (Exception e) {
			Log.error("发送响应时出错: " + e);
		}
	}
	/**
	 * 补充令牌桶中的令牌
	 */
	private static void refillTokens() {
		var now = Instant.now().toEpochMilli();
		var elapsed = now - lastRefillTime;
		if (elapsed <= 0) return;
		var rateLimit = Data.serverUploadRate.get();
		var tokensToAdd = elapsed * rateLimit.bps / 1000L;
		if (tokensToAdd <= 0) return;
		// 限制最大令牌数为速率的2秒容量
		var maxTokens = Rate.multiplyUnsigned(rateLimit.bps, 2L);
		// 更新令牌数和最后补充时间
		var newTokens = Math.min(availableTokens.get() + tokensToAdd, maxTokens);
		availableTokens.set(newTokens);
		lastRefillTime = now;
	}
}