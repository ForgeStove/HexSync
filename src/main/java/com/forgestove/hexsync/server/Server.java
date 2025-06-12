package com.forgestove.hexsync.server;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.*;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.Contract;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executors;
/**
 * HexSync 服务端管理类<p>
 * 负责服务器的启动、停止和状态管理
 */
public class Server implements Runnable {
	public static volatile Map<String, String> serverMap; // 服务端文件名和校验码映射
	public static volatile Thread serverThread; // 服务器线程
	public static volatile HttpServer httpServer; // 服务器实例
	private static volatile boolean isRunning = false; // 服务器运行状态
	private static Server instance; // 单例实例
	@Contract(pure = true)
	private Server() {}
	/**
	 * 获取 Server 单例实例
	 *
	 * @return Server 实例
	 */
	public static synchronized Server getInstance() {
		if (instance == null) instance = new Server();
		return instance;
	}
	/**
	 * 在新线程中启动服务器
	 */
	public static void start() {
		if (isRunning()) {
			Log.info(HexSync.NAME + "Server 已经在运行中");
			return;
		}
		serverThread = new Thread(getInstance());
		serverThread.setName("HexSync-ServerThread");
		serverThread.setDaemon(true); // 设置为守护线程，不阻止JVM退出
		serverThread.start();
	}
	/**
	 * 停止服务端
	 */
	public static synchronized void stop() {
		if (!isRunning() && serverThread == null && httpServer == null) return;
		isRunning = false; // 先标记为非运行状态，避免新请求进入
		Optional.ofNullable(serverMap).ifPresent(Map::clear); // 清理资源
		if (httpServer != null) try {
			httpServer.stop(0); // 停止HTTP服务器
		} catch (Exception error) {
			Log.warn("%sServer 停止过程中出现异常: %s", HexSync.NAME, error.getMessage());
		} finally {
			httpServer = null;
		}
		if (serverThread != null) try {
			serverThread.interrupt(); // 中断服务器线程
			serverThread = null;
		} catch (Exception error) {
			Log.warn("服务器线程中断失败: %s", error.getMessage());
		}
		Log.info(HexSync.NAME + "Server 已关闭");
	}
	/**
	 * 检查服务器是否正在运行
	 *
	 * @return true 如果服务器正在运行
	 */
	public static boolean isRunning() {
		return isRunning && httpServer != null && serverThread != null && serverThread.isAlive(); // 判断服务器线程和HTTP服务器是否都有效且运行中
	}
	/**
	 * 启动服务器 (Runnable 接口实现)
	 */
	@Override
	public void run() {
		if (isRunning()) {
			Log.info(HexSync.NAME + "Server 已经在运行中");
			return;
		}
		Log.info(HexSync.NAME + "Server 正在启动...");
		// 初始化文件
		FileUtil.initFiles(true);
		// 检查是否有文件可以同步
		if (serverMap == null || serverMap.isEmpty()) {
			Log.warn(Data.serverSyncDirectory + " 没有文件，无法启动服务器");
			stop();
			return;
		}
		try {
			// 创建并配置服务器
			httpServer = HttpServer.create(new InetSocketAddress(Data.serverPort.get().getValue()), 0);
			httpServer.setExecutor(Executors.newCachedThreadPool());
			httpServer.createContext("/", RequestHandler::handleRequest);
			httpServer.start();
			isRunning = true;
			Log.info("%sServer 正在运行...端口号为: %d", HexSync.NAME, Data.serverPort.get().getValue());
		} catch (Exception error) {
			Log.error("%sServer 无法启动: %s", HexSync.NAME, error.getMessage());
			stop();
		}
	}
}
