package io.github.forgestove.hexsync.client;
import io.github.forgestove.hexsync.config.Data;
import io.github.forgestove.hexsync.util.*;
import io.github.forgestove.hexsync.util.network.*;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap.Entry;
import org.jetbrains.annotations.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
/**
 * 文件下载器
 * 负责从服务器下载文件并验证文件完整性
 */
public class Downloader {
	// 下载进度监听器
	public static final List<ProgressListener> progressListeners = new ArrayList<>();
	// 线程池参数
	public static final int MAX_DOWNLOAD_THREADS = 4; // 最大并行下载线程数
	// 从服务端同步文件夹下载客户端缺少的文件
	public static void downloadMissingFiles(@NotNull Object2ObjectMap<String, String> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			Log.info("没有需要下载的文件");
			return;
		}
		final var size = toDownloadMap.size();
		Log.info("开始下载 [%d] 个文件".formatted(size));
		// 通知所有监听器下载开始
		progressListeners.forEach(listener -> listener.onDownloadStart(size));
		// 创建一个包含文件路径和SHA1对的列表
		List<Entry<String, String>> downloadList = new ArrayList<>(toDownloadMap.object2ObjectEntrySet());
		// 成功下载的文件数量计数器
		final var successCount = new AtomicInteger(0);
		// 创建线程池，用于并行下载文件
		var threadCount = Math.min(MAX_DOWNLOAD_THREADS, size);
		var downloadExecutor = Executors.newFixedThreadPool(threadCount);
		// 用于协调完成状态的锁
		var completionLock = new Object();
		// 已提交的任务计数
		final var submittedTasks = new AtomicInteger(0);
		// 已完成的任务计数
		final var completedTasks = new AtomicInteger(0);
		try {
			// 遍历下载列表，提交下载任务
			IntStream.iterate(0, i -> i < downloadList.size() && Client.isRunning, i -> i + 1).forEach(i -> {
				final var entry = downloadList.get(i);
				final var fileIndex = i + 1;
				final var fileName = entry.getKey();
				final var filePath = Data.clientSyncPath.get().resolve(fileName);
				// 提交下载任务到线程池
				downloadExecutor.submit(() -> {
					// 通知监听器文件开始下载
					synchronized (progressListeners) {
						progressListeners.forEach(listener -> listener.onFileDownloadStart(fileName, fileIndex));
					}
					// 执行下载
					var success = downloadAndCheckFile(filePath, entry.getValue(), fileName, fileIndex);
					// 通知监听器文件下载完成
					synchronized (progressListeners) {
						progressListeners.forEach(listener -> listener.onFileDownloadComplete(fileName, fileIndex, success));
					}
					if (!success) {
						Log.error("下载失败: " + filePath);
						Client.errorDownload = true;
					} else {
						Log.info("已下载: [%d/%d] %s".formatted(fileIndex, size, filePath));
						successCount.incrementAndGet();
					}
					// 更新完成任务计数，并在所有任务完成时通知主线程
					var completed = completedTasks.incrementAndGet();
					if (completed == submittedTasks.get()) synchronized (completionLock) {
						completionLock.notify();
					}
				});
				submittedTasks.incrementAndGet();
			});
			// 等待所有下载任务完成
			synchronized (completionLock) {
				try {
					if (completedTasks.get() < submittedTasks.get()) completionLock.wait();
				} catch (InterruptedException e) {
					Log.warn("下载等待被中断");
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			// 关闭线程池
			downloadExecutor.shutdown();
		}
		// 通知监听器所有下载完成
		synchronized (progressListeners) {
			progressListeners.forEach(listener -> listener.onDownloadComplete(successCount.get(), size));
		}
		if (Client.errorDownload) Log.warn("下载失败: [%d/%d]".formatted(successCount.get(), size));
		else Log.info("下载完成: [%d/%d]".formatted(successCount.get(), size));
		if (Data.clientAuto.get()) System.exit(0);
	}
	private static boolean downloadAndCheckFile(Path filePath, String requestSHA1, String fileName, int fileIndex) {
		if (!Client.isRunning) return false;
		var clientFile = filePath.toFile();
		if (requestSHA1 == null) {
			Log.error("无法获取请求的校验码: " + clientFile);
			return false;
		}
		// 获取下载URL
		var downloadURL = ModAPI.getURL(requestSHA1);
		if (downloadURL == null) downloadURL = "%s:%d/%s/%s".formatted(HttpUtil.formatHTTP(Data.remoteAddress.get()),
			Data.clientPort.get().getValue(),
			HttpUtil.DOWNLOAD,
			requestSHA1);
		try {
			// 使用进度监控器下载文件
			var progressInput = downloadWithProgress(downloadURL, fileName, fileIndex);
			if (progressInput == null) return false;
			// 写入文件
			if (!FileUtil.writeToFile(progressInput, clientFile)) return false;
			// 验证文件SHA1
			if (!requestSHA1.equals(HashUtil.calculateSHA1(clientFile))) {
				Log.error("校验失败, 文件可能已损坏: " + clientFile);
				FileUtil.deleteFile(clientFile);
				return false;
			}
		} catch (Exception e) {
			Log.error("下载或校验文件时出错: " + e.getMessage());
			return false;
		}
		return true;
	}
	/**
	 * 带进度监控的文件下载方法
	 */
	private static @Nullable ProgressInputStream downloadWithProgress(String url, String fileName, int fileIndex) {
		try {
			var response = HttpUtil.sendGet(url, BodyHandlers.ofInputStream());
			if (response.statusCode() != HttpURLConnection.HTTP_OK) {
				Log.error("下载失败, 错误代码: " + response.statusCode());
				return null;
			}
			// 获取文件大小
			var contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
			// 创建带进度监控的输入流
			return new ProgressInputStream(response.body(), contentLength, (bytesRead, totalBytes) -> {
				var progress = totalBytes > 0 ? (int) (bytesRead * 100 / totalBytes) : 0;
				// 通知所有监听器文件下载进度更新，包括已下载的MB和总MB
				synchronized (progressListeners) {
					for (var listener : progressListeners)
						listener.onFileDownloadProgress(fileName, fileIndex, progress, bytesRead, totalBytes);
				}
			});
		} catch (Exception e) {
			Log.error("获取下载流时出错: " + e.getMessage());
			return null;
		}
	}
	// 从服务器获取文件名和校验码列表
	public static Object2ObjectOpenHashMap<String, String> fetchFileSHA1List() {
		var url = "%s:%d/%s".formatted(HttpUtil.formatHTTP(Data.remoteAddress.get()), Data.clientPort.get().getValue(), HttpUtil.LIST);
		Log.info("正在连接至: " + url);
		var requestMap = new Object2ObjectOpenHashMap<String, String>();
		HttpResponse<String> response;
		try {
			response = HttpUtil.sendGet(url, BodyHandlers.ofString(StandardCharsets.UTF_8));
		} catch (Exception e) {
			Log.error("获取文件列表时出错: " + e.getMessage());
			Client.errorDownload = true;
			return requestMap;
		}
		if (response.statusCode() != HttpURLConnection.HTTP_OK) {
			if (Client.isRunning) Log.error("请求列表失败, 错误代码: " + response.statusCode());
			Client.errorDownload = true;
			return requestMap;
		}
		try (var bufferedReader = new BufferedReader(new StringReader(response.body()))) {
			String fileName;
			while ((fileName = bufferedReader.readLine()) != null) {
				var sha1 = bufferedReader.readLine();
				if (sha1 != null) requestMap.put(fileName, sha1);
			}
		} catch (Exception error) {
			Log.error("读取响应时出错: " + (error.getMessage() != null ? error.getMessage() : "无响应内容"));
			Client.errorDownload = true;
		}
		Log.info("获取到 [%d] 个文件".formatted(requestMap.size()));
		return requestMap;
	}
}
