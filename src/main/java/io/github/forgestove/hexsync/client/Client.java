package io.github.forgestove.hexsync.client;
import io.github.forgestove.hexsync.HexSync;
import io.github.forgestove.hexsync.config.Data;
import io.github.forgestove.hexsync.util.*;
import io.github.forgestove.hexsync.util.network.HttpUtil;
import org.jetbrains.annotations.Contract;
/**
 * HexSync 客户端管理类<p>
 * 负责客户端的启动、停止和状态管理
 */
public class Client implements Runnable {
	public static volatile boolean errorDownload; // 下载错误标志
	public static volatile boolean isRunning; // 客户端运行状态
	private static volatile Thread clientThread; // 客户端线程
	private static Client instance; // 单例实例
	@Contract(pure = true)
	private Client() {}
	/**
	 * 获取 Client 单例实例
	 *
	 * @return Client 实例
	 */
	public static synchronized Client getInstance() {
		if (instance == null) instance = new Client();
		return instance;
	}
	/**
	 * 在新线程中启动客户端
	 */
	public static synchronized void start() {
		if (isRunning) {
			Log.info(HexSync.NAME + "Client 已经在运行中");
			return;
		}
		clientThread = new Thread(getInstance());
		clientThread.setName("HexSync-ClientThread");
		clientThread.setDaemon(false); // 设置为用户线程，确保任务完成
		clientThread.start();
	}
	/**
	 * 停止客户端
	 */
	public static synchronized void stop() {
		if (!isRunning && clientThread == null) {
			Log.info(HexSync.NAME + "Client 未在运行中");
			return;
		}
		// 中断客户端线程
		if (clientThread != null) {
			clientThread.interrupt();
			clientThread = null;
		}
		// 重置 HttpClient
		HttpUtil.resetClient();
		isRunning = false;
		Log.info(HexSync.NAME + "Client 已关闭");
		if (Data.clientAuto.get() && !errorDownload) System.exit(0); // 如果设置了自动启动且没有下载错误，则退出程序
	}
	/**
	 * 启动客户端 (Runnable 接口实现)
	 */
	@Override
	public synchronized void run() {
		if (isRunning) {
			Log.info(HexSync.NAME + "Client 已经在运行中");
			return;
		}
		isRunning = true;
		errorDownload = false;
		Log.info(HexSync.NAME + "Client 正在启动...");
		// 添加GUI进度监听器
		if (!HexSync.HEADLESS) Downloader.progressListeners.add(new GUIProgressListener());
		try {
			// 初始化
			FileUtil.makeDirectory(Data.clientOnlyPath.get());
			FileUtil.makeDirectory(Data.clientSyncPath.get());
			var requestMap = Downloader.fetchFileSHA1List(); // 获取服务器文件列表
			if (!requestMap.isEmpty()) {
				FileUtil.deleteFilesNotInMaps(requestMap, FileUtil.initMap(Data.clientOnlyPath.get())); // 删除不在服务器列表中的文件
				requestMap.values().removeAll(FileUtil.initMap(Data.clientSyncPath.get()).values()); // 过滤掉客户端已有的文件
				Downloader.downloadMissingFiles(requestMap); // 下载缺失的文件
				FileUtil.copyDirectory(Data.clientOnlyPath.get(), Data.clientSyncPath.get()); // 复制文件到客户端同步目录
			}
		} catch (Exception e) {
			Log.error("客户端启动失败: " + e.getMessage());
			errorDownload = true; // 标记下载错误
		} finally {
			// 清除下载进度监听器
			if (!HexSync.HEADLESS) Downloader.progressListeners.clear();
			stop();
		}
	}
}
