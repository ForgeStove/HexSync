package com.forgestove.hexsync.client;
import com.forgestove.hexsync.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
public class Downloader {
	// 从服务端同步文件夹下载客户端缺少的文件
	public static void downloadMissingFiles(@NotNull Map<String, String> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			Log.info("模组已经是最新版本");
			return;
		}
		Log.info("开始下载 [%d] 个文件", toDownloadMap.size());
		var count = new AtomicInteger(0);
		var threadCount = Math.min(8, toDownloadMap.size());
		var executor = Executors.newFixedThreadPool(threadCount);
		var futures = new ArrayList<Future<?>>();
		for (var entry : toDownloadMap.entrySet()) {
			var filePath = Config.clientSyncDirectory + File.separator + entry.getKey();
			futures.add(executor.submit(() -> {
				if (downloadAndCheckFile(filePath, entry.getValue())) {
					var finished = count.incrementAndGet();
					Log.info("已下载: [%d/%d] %s", finished, toDownloadMap.size(), filePath);
				} else {
					Log.error("下载失败: " + filePath);
					Client.errorDownload = true;
				}
			}));
		}
		for (var future : futures) try {future.get();} catch (Exception ignored) {} // 等待所有任务完成
		executor.shutdown();
		Log.info("%s: [%d/%d]", Client.errorDownload ? "下载失败" : "下载完成", count.get(), toDownloadMap.size());
		if (Client.clientAutoStart) System.exit(0);
	}
	private static boolean downloadAndCheckFile(String filePath, String requestSHA1) {
		if (Client.clientThread.get() == null) return false;
		var clientFile = new File(filePath);
		if (requestSHA1 == null) {
			Log.error("无法获取请求的校验码: " + clientFile);
			return false;
		}
		var downloadURL = ModAPI.getURL(requestSHA1);
		if (downloadURL == null) {
			downloadURL = String.format(
				"%s:%d/%s/%s",
				Settings.formatHTTP(Config.serverAddress),
				Client.clientPort,
				Config.DOWNLOAD,
				requestSHA1
			);
		}
		try {
			var response = HttpUtil.sendGet(downloadURL, BodyHandlers.ofInputStream());
			if (response.statusCode() != HttpURLConnection.HTTP_OK) {
				Log.error("下载失败,错误代码: " + response.statusCode());
				return false;
			}
			if (!FileUtil.writeToFile(response.body(), clientFile)) return false;
		} catch (Exception error) {
			Log.error("下载失败: %s %s", filePath, error.getMessage());
			return false;
		}
		if (!requestSHA1.equals(FileUtil.calculateSHA1(clientFile))) {
			Log.error("校验失败,文件可能已损坏: " + clientFile);
			FileUtil.deleteFile(clientFile);
			return false;
		}
		return true;
	}
	// 从服务器获取文件名和校验码列表
	public static Map<String, String> fetchFileSHA1List() {
		var url = String.format("%s:%d/%s", Settings.formatHTTP(Config.serverAddress), Client.clientPort, Config.LIST);
		Log.info("正在连接到: " + url);
		Map<String, String> requestMap = new HashMap<>();
		try {
			var response = HttpUtil.sendGet(url, BodyHandlers.ofString());
			if (response.statusCode() != HttpURLConnection.HTTP_OK) {
				if (Client.clientThread.get() != null) Log.error("请求列表失败,错误代码: " + response.statusCode());
				Client.errorDownload = true;
				return requestMap;
			}
			try (var bufferedReader = new BufferedReader(new StringReader(response.body()))) {
				String fileName;
				while ((fileName = bufferedReader.readLine()) != null) {
					var sha1 = bufferedReader.readLine();
					if (sha1 != null) requestMap.put(fileName, sha1);
				}
			}
		} catch (Exception error) {
			Log.error("读取响应时出错: " + error.getMessage());
			Client.errorDownload = true;
		}
		Log.info("获取到 [%d] 个文件", requestMap.size());
		return requestMap;
	}
}
