package ForgeStove.HexSync.Server;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.HttpURLConnection;

import static ForgeStove.HexSync.Util.Log.*;
import static java.lang.System.currentTimeMillis;
public class ResponseSender {
	// 发送数据
	public static void responseSender(HttpExchange exchange, InputStream inputStream, long responseBytesLength) {
		if (inputStream == null) return;
		try (OutputStream outputStream = exchange.getResponseBody()) {
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytesLength); // 设置响应头
			byte[] buffer = new byte[16384];
			long totalBytesSent = 0; // 记录已发送字节数
			long lastFillTime = currentTimeMillis(); // 最近一次填充时间
			while (totalBytesSent < responseBytesLength) {
				if (Server.serverUploadRateLimit == 0) { // 无限制
					int bytesRead = inputStream.read(buffer);
					if (bytesRead == -1) break;
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					continue;
				}
				long currentTime = currentTimeMillis();
				Server.AVAILABLE_TOKENS.addAndGet((currentTime - lastFillTime) * Server.maxUploadRateInBytes / 1000);
				lastFillTime = currentTime; // 更新时间
				long bytesToSend = Math.min(16384, responseBytesLength - totalBytesSent);
				if (Server.AVAILABLE_TOKENS.get() >= bytesToSend) {
					int bytesRead = inputStream.read(buffer, 0, (int) bytesToSend);
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					totalBytesSent += bytesRead; // 更新已发送字节数
					Server.AVAILABLE_TOKENS.addAndGet(-bytesRead); // 减少可用令牌
				} else Thread.sleep((bytesToSend - Server.AVAILABLE_TOKENS.get()) * 1000 / Server.maxUploadRateInBytes);
			}
		} catch (Exception error) {
			log(SEVERE, "发送响应时出错: " + error.getMessage());
		} finally {
			try {
				inputStream.close();
			} catch (IOException error) {
				log(WARNING, "关闭输入流时出错: " + error.getMessage());
			}
		}
	}
}
