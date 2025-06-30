package io.github.forgestove.hexsync.server;
import com.sun.net.httpserver.HttpServer;
import io.github.forgestove.hexsync.HexSync;
import io.github.forgestove.hexsync.config.Data;
import io.github.forgestove.hexsync.util.*;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Contract;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
/**
 * HexSync 服务端管理类<p>
 * 负责服务器的启动、停止和状态管理
 */
public class Server implements Runnable {
	public static volatile Object2ObjectMap<String, String> serverMap = new Object2ObjectOpenHashMap<>(); // 服务端文件名和校验码映射
	public static volatile Thread serverThread; // 服务器线程
	public static volatile HttpServer httpServer; // 服务器实例
	public static volatile boolean isRunning; // 服务器运行状态
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
	public static synchronized void start() {
		if (isRunning) {
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
		if (!isRunning && serverThread == null && httpServer == null) {
			Log.info(HexSync.NAME + "Server 未在运行中");
			return;
		}
		isRunning = false;
		serverMap = null;
		if (httpServer != null) try {
			httpServer.stop(0); // 停止HTTP服务器
		} catch (Exception e) {
			Log.warn("%sServer 停止过程中出现异常: %s".formatted(HexSync.NAME, e.getMessage()));
		} finally {
			httpServer = null;
		}
		if (serverThread != null) try {
			serverThread.interrupt(); // 中断服务器线程
			serverThread = null;
		} catch (Exception e) {
			Log.warn("服务器线程中断失败: %s".formatted(e.getMessage()));
		}
		Log.info(HexSync.NAME + "Server 已关闭");
	}
	/**
	 * 启动服务器 (Runnable 接口实现)
	 */
	@Override
	public synchronized void run() {
		if (isRunning) {
			Log.info(HexSync.NAME + "Server 已经在运行中");
			return;
		}
		Log.info(HexSync.NAME + "Server 正在启动...");
		// 初始化
		FileUtil.makeDirectory(Data.serverSyncPath.get());
		serverMap = FileUtil.initMap(Data.serverSyncPath.get());
		// 检查是否有文件可以同步
		if (serverMap == null || serverMap.isEmpty()) {
			Log.warn(Data.serverSyncPath.get() + " 没有文件，无法启动服务器");
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
			Log.info("%sServer 正在运行...端口号为: %d".formatted(HexSync.NAME, Data.serverPort.get().getValue()));
		} catch (Exception e) {
			Log.error("%sServer 无法启动: %s".formatted(HexSync.NAME, e.getMessage()));
			stop();
		}
	}
}
