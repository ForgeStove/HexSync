package ForgeStove.HexSync.Server;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;

import static ForgeStove.HexSync.Util.Config.serverSyncDirectory;
import static ForgeStove.HexSync.Util.Log.*;
import static java.io.File.separator;
import static java.lang.System.lineSeparator;
public class RequestHandler {
	// 处理请求
	public static void requestHandler(HttpExchange exchange) {
		if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) return;
		String requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/download/")) {
			long requestCRC = Long.parseLong(requestURI.substring(requestURI.lastIndexOf("/") + 1));
			String filePath = null;
			for (Map.Entry<String, Long> entry : Server.serverMap.entrySet())
				if (entry.getValue() == requestCRC) {
					filePath = serverSyncDirectory + separator + entry.getKey();
					break;
				}
			if (filePath == null) return;
			File file = new File(filePath);
			try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
				ResponseSender.responseSender(exchange, inputStream, file.length());
				log(INFO, "发送文件: " + file);
			} catch (IOException error) {
				log(SEVERE, "发送文件时出错: " + error.getMessage());
			}
		} else if (requestURI.startsWith("/list")) {
			StringBuilder responseBuilder = new StringBuilder();
			for (Map.Entry<String, Long> entry : Server.serverMap.entrySet())
				responseBuilder.append(entry.getKey())
						.append(lineSeparator())
						.append(entry.getValue())
						.append(lineSeparator());
			byte[] bytes = responseBuilder.toString().getBytes();
			try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
				ResponseSender.responseSender(exchange, inputStream, bytes.length);
				log(INFO, "发送列表");
			} catch (IOException error) {
				log(SEVERE, "发送列表时出错: " + error.getMessage());
			}
		}
	}
}
