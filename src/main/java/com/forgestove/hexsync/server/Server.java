package com.forgestove.hexsync.server;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;
public class Server {
	public static Thread serverThread; // 服务器线程
	public static HttpServer HTTPServer; // 存储服务器实例
	public static Map<String, String> serverMap; // 存储服务端文件名和对应的校验码数据
	// 启动服务端
	public static void runServer() {
		if (serverThread != null) return;
		serverThread = new Thread(() -> {
			Log.info(HexSync.NAME + "Server正在启动...");
			FileUtil.initFiles(true);
			if (serverMap.isEmpty()) {
				Log.warn(Data.serverSyncDirectory + "没有文件,无法启动服务器");
				stopServer();
				return;
			}
			try {
				HTTPServer = HttpServer.create(new InetSocketAddress(Data.serverPort.get().getValue()), 0);
				HTTPServer.setExecutor(Executors.newCachedThreadPool());
				HTTPServer.createContext("/", RequestHandler::handleRequest);
				HTTPServer.start();
			} catch (Exception error) {
				Log.error("%sServer无法启动: %s", HexSync.NAME, error.getMessage());
				return;
			}
			Log.info("%sServer正在运行...端口号为: %d", HexSync.NAME, Data.serverPort.get().getValue());
		});
		serverThread.start();
	}
	// 停止服务端
	public static void stopServer() {
		if (serverThread == null && HTTPServer == null) return;
		if (serverMap != null) serverMap.clear();
		if (serverThread != null) serverThread = null;
		if (HTTPServer != null) {
			HTTPServer.stop(0);
			HTTPServer = null;
		}
		Log.info(HexSync.NAME + "Server已关闭");
	}
}
