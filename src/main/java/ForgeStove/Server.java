package src.main.java.ForgeStove;
import com.sun.net.httpserver.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.io.File.separator;
import static java.lang.System.*;
import static src.main.java.ForgeStove.Config.*;
import static src.main.java.ForgeStove.HexSync.HEX_SYNC_NAME;
import static src.main.java.ForgeStove.Log.*;
import static src.main.java.ForgeStove.Utils.*;
public class Server {
	public static final AtomicLong AVAILABLE_TOKENS = new AtomicLong(0); // 当前可用令牌数量
	public static Thread serverThread; // 服务器线程
	public static HttpServer HTTPServer; // 存储服务器实例
	public static Map<String, Long> serverMap; // 存储服务端文件名和对应的校验码数据
	public static long serverUploadRateLimit = 1; // 上传速率限制值，默认限速1MB
	public static long maxUploadRateInBytes; // 上传速率限制值对应的字节数
	public static boolean serverAutoStart; // 服务端自动启动，默认不自动启动
	public static int serverPort = 65535; // 服务端端口，默认值65535
	// 启动服务端
	public static void startServer() {
		if (serverThread != null) return;
		serverThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Server正在启动...");
			initFiles(true);
			if (serverMap.isEmpty()) {
				log(WARNING, serverSyncDirectory + "没有文件,无法启动服务器");
				stopServer();
				return;
			}
			try {
				ExecutorService executorService = Executors.newFixedThreadPool(8);
				maxUploadRateInBytes = convertToBytes(serverUploadRateLimit, serverUploadRateLimitUnit);
				HTTPServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
				HTTPServer.setExecutor(executorService);
				HTTPServer.createContext("/", exchange -> executorService.submit(() -> processRequest(exchange)));
				HTTPServer.start();
			} catch (IOException error) {
				log(SEVERE, HEX_SYNC_NAME + "Server无法启动: " + error.getMessage());
				return;
			}
			log(INFO, HEX_SYNC_NAME + "Server正在运行...端口号为: " + serverPort);
		});
		serverThread.start();
	}
	// 停止服务端
	public static void stopServer() {
		if (serverThread == null || HTTPServer == null) return;
		serverMap.clear();
		HTTPServer.stop(0);
		serverThread = null;
		log(INFO, HEX_SYNC_NAME + "Server已关闭");
	}
	// 处理请求
	public static void processRequest(HttpExchange exchange) {
		if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) return;
		String requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/download/")) {
			long requestCRC = Long.parseLong(requestURI.substring(requestURI.lastIndexOf("/") + 1));
			String filePath = null;
			for (Map.Entry<String, Long> entry : serverMap.entrySet())
				if (entry.getValue() == requestCRC) {
					filePath = serverSyncDirectory + separator + entry.getKey();
					break;
				}
			if (filePath == null) return;
			File file = new File(filePath);
			try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
				sendData(exchange, inputStream, file.length());
				log(INFO, "发送文件: " + file);
			} catch (IOException error) {
				log(SEVERE, "发送文件时出错: " + error.getMessage());
			}
		} else if (requestURI.startsWith("/list")) {
			StringBuilder responseBuilder = new StringBuilder();
			for (Map.Entry<String, Long> entry : serverMap.entrySet())
				responseBuilder.append(entry.getKey())
						.append(lineSeparator())
						.append(entry.getValue())
						.append(lineSeparator());
			byte[] bytes = responseBuilder.toString().getBytes();
			try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
				sendData(exchange, inputStream, bytes.length);
				log(INFO, "发送列表");
			} catch (IOException error) {
				log(SEVERE, "发送列表时出错: " + error.getMessage());
			}
		}
	}
	// 发送数据
	public static void sendData(HttpExchange exchange, InputStream inputStream, long responseBytesLength) {
		if (inputStream == null) return;
		try (OutputStream outputStream = exchange.getResponseBody()) {
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytesLength); // 设置响应头
			byte[] buffer = new byte[16384];
			long totalBytesSent = 0; // 记录已发送字节数
			long lastFillTime = currentTimeMillis(); // 最近一次填充时间
			while (totalBytesSent < responseBytesLength) {
				if (serverUploadRateLimit == 0) { // 无限制
					int bytesRead = inputStream.read(buffer);
					if (bytesRead == -1) break;
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					continue;
				}
				long currentTime = currentTimeMillis();
				AVAILABLE_TOKENS.addAndGet((currentTime - lastFillTime) * maxUploadRateInBytes / 1000);
				lastFillTime = currentTime; // 更新时间
				long bytesToSend = Math.min(16384, responseBytesLength - totalBytesSent);
				if (AVAILABLE_TOKENS.get() >= bytesToSend) {
					int bytesRead = inputStream.read(buffer, 0, (int) bytesToSend);
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					totalBytesSent += bytesRead; // 更新已发送字节数
					AVAILABLE_TOKENS.addAndGet(-bytesRead); // 减少可用令牌
				} else Thread.sleep((bytesToSend - AVAILABLE_TOKENS.get()) * 1000 / maxUploadRateInBytes);
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