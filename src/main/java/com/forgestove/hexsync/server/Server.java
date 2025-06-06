package com.forgestove.hexsync.server;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.util.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
public class Server {
	public static final AtomicLong AVAILABLE_TOKENS = new AtomicLong(0); // 当前可用令牌数量
	public static Thread serverThread; // 服务器线程
	public static HttpServer HTTPServer; // 存储服务器实例
	public static Map<String, String> serverMap; // 存储服务端文件名和对应的校验码数据
	public static boolean serverAutoStart; // 服务端自动启动，默认不自动启动
	public static int serverPort = 65535; // 服务端端口，默认值65535
	// 启动服务端
	public static void runServer() {
		if (serverThread != null) return;
		serverThread = new Thread(() -> {
			Log.info(HexSync.NAME + "Server正在启动...");
			FileUtil.initFiles(true);
			if (serverMap.isEmpty()) {
				Log.warn(Config.serverSyncDirectory + "没有文件,无法启动服务器");
				stopServer();
				return;
			}
			try {
				HTTPServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
				HTTPServer.setExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2));
				HTTPServer.createContext("/", RequestHandler::handleRequest);
				HTTPServer.start();
			} catch (Exception error) {
				Log.error("%sServer无法启动: %s", HexSync.NAME, error.getMessage());
				return;
			}
			Log.info("%sServer正在运行...端口号为: %d", HexSync.NAME, serverPort);
		});
		serverThread.start();
	}
	// 停止服务端
	public static void stopServer() {
		if (serverMap != null) serverMap.clear();
		if (serverThread != null) serverThread = null;
		if (HTTPServer != null) HTTPServer.stop(0);
		Log.info(HexSync.NAME + "Server已关闭");
	}
}
