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
import ForgeStove.HexSync.Util.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static ForgeStove.HexSync.Util.Config.*;
import static ForgeStove.HexSync.Util.Log.*;
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
	public static void runServer() {
		if (serverThread != null) return;
		serverThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Server正在启动...");
			Files.initFiles(true);
			if (serverMap.isEmpty()) {
				log(WARNING, serverSyncDirectory + "没有文件,无法启动服务器");
				stopServer();
				return;
			}
			try {
				ExecutorService executorService = Executors.newFixedThreadPool(8);
				maxUploadRateInBytes = Unit.convertToBytes(serverUploadRateLimit, serverUploadRateLimitUnit);
				HTTPServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
				HTTPServer.setExecutor(executorService);
				HTTPServer.createContext(
						"/",
						exchange -> executorService.submit(() -> RequestHandler.requestHandler(exchange))
				);
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
		if (serverMap != null) serverMap.clear();
		if (serverThread != null) serverThread = null;
		if (HTTPServer != null) HTTPServer.stop(0);
		log(INFO, HEX_SYNC_NAME + "Server已关闭");
	}
}
