package com.forgestove.hexsync.client;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.Contract;
/**
 * HexSync 客户端管理类<p>
 * 负责客户端的启动、停止和状态管理
 */
public class Client implements Runnable {
	public static volatile boolean errorDownload; // 下载错误标志
	private static volatile Thread clientThread; // 客户端线程
	private static volatile boolean isRunning = false; // 客户端运行状态
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
	public static void start() {
		if (isRunning()) {
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
		if (!isRunning && clientThread == null) return;
		// 中断客户端线程
		if (clientThread != null) try {
			clientThread.interrupt();
			clientThread = null;
		} catch (Exception error) {
			Log.warn("客户端线程中断失败: %s".formatted(error.getMessage()));
		}
		// 重置 HttpClient
		HttpUtil.resetClient();
		isRunning = false;
		Log.info(HexSync.NAME + "Client 已关闭");
		if (Data.clientAuto.get() && !errorDownload) System.exit(0); // 如果设置了自动启动且没有下载错误，则退出程序
	}
	/**
	 * 检查客户端是否正在运行
	 *
	 * @return true 如果客户端正在运行
	 */
	public static boolean isRunning() {
		return isRunning;
	}
	/**
	 * 启动客户端 (Runnable 接口实现)
	 */
	@Override
	public void run() {
		if (isRunning()) {
			Log.info(HexSync.NAME + "Client 已经在运行中");
			return;
		}
		isRunning = true;
		Log.info(HexSync.NAME + "Client 正在启动...");
		// 初始化
		FileUtil.makeDirectory(Data.clientOnlyPath.get());
		FileUtil.makeDirectory(Data.clientSyncPath.get());
		try {
			var requestMap = Downloader.fetchFileSHA1List(); // 获取服务器文件列表
			if (!requestMap.isEmpty()) {
				FileUtil.deleteFilesNotInMaps(requestMap, FileUtil.initMap(Data.clientOnlyPath.get())); // 删除不在服务器列表中的文件
				var clientSHA1Set = new ObjectOpenHashSet<>(FileUtil.initMap(Data.clientSyncPath.get()).values()); // 获取客户端已有文件的 SHA1 列表
				requestMap.entrySet().removeIf(entry -> clientSHA1Set.contains(entry.getValue())); // 过滤掉客户端已有的文件
				Downloader.downloadMissingFiles(requestMap); // 下载缺失的文件
				FileUtil.copyDirectory(Data.clientOnlyPath.get(), Data.clientSyncPath.get()); // 复制文件到客户端同步目录
			}
		} catch (Exception error) {
			Log.error("客户端启动失败: " + error.getMessage());
			errorDownload = true; // 标记下载错误
		} finally {
			stop();
		}
	}
}
