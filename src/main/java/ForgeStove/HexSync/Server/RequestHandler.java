// Copyright (C) 2025 ForgeStove
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package ForgeStove.HexSync.Server;
import ForgeStove.HexSync.Util.Config;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;

import static ForgeStove.HexSync.Util.Log.*;
import static java.io.File.separator;
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
					filePath = Config.serverSyncDirectory + separator + entry.getKey();
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
				responseBuilder.append(String.format("%s%n%s%n", entry.getKey(), entry.getValue()));
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
