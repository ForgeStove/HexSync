package ForgeStove.HexSync.Server;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.util.Map;

import static ForgeStove.HexSync.Server.ResponseSender.responseSender;
import static ForgeStove.HexSync.Server.Server.serverMap;
import static ForgeStove.HexSync.Util.Config.serverSyncDirectory;
import static ForgeStove.HexSync.Util.Log.*;
import static java.io.File.separator;
import static java.nio.file.Files.newInputStream;
public class RequestHandler {
	// 处理请求
	public static void requestHandler(HttpExchange exchange) {
		if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) return;
		String requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/download/")) {
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
				responseSender(exchange, inputStream, file.length());
				log(INFO, "发送文件: " + file);
			} catch (IOException error) {
				log(SEVERE, "发送文件时出错: " + error.getMessage());
			}
		} else if (requestURI.startsWith("/list")) {
			StringBuilder responseBuilder = new StringBuilder();
			for (Map.Entry<String, Long> entry : serverMap.entrySet())
				responseBuilder.append(String.format("%s%n%s%n", entry.getKey(), entry.getValue()));
			byte[] bytes = responseBuilder.toString().getBytes();
			try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
				responseSender(exchange, inputStream, bytes.length);
				log(INFO, "发送列表");
			} catch (IOException error) {
				log(SEVERE, "发送列表时出错: " + error.getMessage());
			}
		}
	}
}
