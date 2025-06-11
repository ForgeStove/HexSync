package com.forgestove.hexsync.client;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public class Downloader {
	// 从服务端同步文件夹下载客户端缺少的文件
	public static void downloadMissingFiles(@NotNull Map<String, String> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			Log.info("没有需要下载的文件");
			return;
		}
		var size = toDownloadMap.size();
		Log.info("开始下载 [%d] 个文件", size);
		var count = new AtomicInteger(0);
		toDownloadMap.entrySet().parallelStream().forEach(entry -> {
			var filePath = FileUtil.path(Data.clientSyncDirectory.get(), entry.getKey());
			if (downloadAndCheckFile(filePath, entry.getValue())) Log.info("已下载: [%d/%d] %s", count.incrementAndGet(), size, filePath);
			else {
				Log.error("下载失败: " + filePath);
				Client.errorDownload = true;
			}
		});
		Log.info("%s: [%d/%d]", Client.errorDownload ? "下载失败" : "下载完成", count.get(), size);
		if (Data.clientAutoStart.get()) System.exit(0);
	}
	private static boolean downloadAndCheckFile(String filePath, String requestSHA1) {
		if (Client.clientThread.get() == null) return false;
		var clientFile = new File(filePath);
		if (requestSHA1 == null) {
			Log.error("无法获取请求的校验码: " + clientFile);
			return false;
		}
		var downloadURL = ModAPI.getURL(requestSHA1);
		if (downloadURL == null) downloadURL = "%s:%d/%s/%s".formatted(HttpUtil.formatHTTP(Data.remoteAddress.get()),
			Data.clientPort.get().getValue(),
			HttpUtil.DOWNLOAD,
			requestSHA1);
		var response = HttpUtil.sendGet(downloadURL, BodyHandlers.ofInputStream());
		if (response == null) {
			Log.error("下载请求失败: " + downloadURL);
			return false;
		}
		if (response.statusCode() != HttpURLConnection.HTTP_OK) {
			Log.error("下载失败,错误代码: " + response.statusCode());
			return false;
		}
		if (!FileUtil.writeToFile(response.body(), clientFile)) return false;
		if (!requestSHA1.equals(HashUtil.calculateSHA1(clientFile))) {
			Log.error("校验失败,文件可能已损坏: " + clientFile);
			FileUtil.deleteFile(clientFile);
			return false;
		}
		return true;
	}
	// 从服务器获取文件名和校验码列表
	public static Map<String, String> fetchFileSHA1List() {
		var url = "%s:%d/%s".formatted(HttpUtil.formatHTTP(Data.remoteAddress.get()), Data.clientPort.get().getValue(), HttpUtil.LIST);
		Log.info("正在连接至: " + url);
		var requestMap = new HashMap<String, String>();
		var response = HttpUtil.sendGet(url, BodyHandlers.ofString());
		if (response == null) {
			Log.error("获取文件列表失败: " + url);
			Client.errorDownload = true;
			return requestMap;
		}
		if (response.statusCode() != HttpURLConnection.HTTP_OK) {
			if (Client.clientThread.get() != null) Log.error("请求列表失败,错误代码: " + response.statusCode());
			Client.errorDownload = true;
			return requestMap;
		}
		try (var bufferedReader = new BufferedReader(new StringReader(response.body()))) {
			String fileName;
			while ((fileName = bufferedReader.readLine()) != null) {
				var sha1 = bufferedReader.readLine();
				if (sha1 != null && sha1.matches("^[a-fA-F0-9]{40}$")) requestMap.put(fileName, sha1);
			}
		} catch (Exception error) {
			Log.error("读取响应时出错: " + (error.getMessage() != null ? error.getMessage() : "无响应内容"));
			Client.errorDownload = true;
		}
		Log.info("获取到 [%d] 个文件", requestMap.size());
		return requestMap;
	}
}
