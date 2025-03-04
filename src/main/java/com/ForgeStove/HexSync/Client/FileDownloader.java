package com.ForgeStove.HexSync.Client;
import java.io.*;
import java.net.*;
import java.util.*;

import static com.ForgeStove.HexSync.Client.Client.*;
import static com.ForgeStove.HexSync.Util.Config.*;
import static com.ForgeStove.HexSync.Util.Files.calculateCRC;
import static com.ForgeStove.HexSync.Util.Log.*;
import static com.ForgeStove.HexSync.Util.Settings.formatHTTP;
import static java.io.File.separator;
import static java.lang.System.exit;
import static java.net.HttpURLConnection.HTTP_OK;
public class FileDownloader {
	// 从服务端同步文件夹下载客户端缺少的文件
	public static void downloadMissingFiles(Map<String, Long> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			log(INFO, "模组已经是最新版本");
			return;
		}
		log(INFO, "开始下载 [" + toDownloadMap.size() + "] 个文件");
		int count = 0;
		int toDownloadMapSize = toDownloadMap.size();
		for (Map.Entry<String, Long> entry : toDownloadMap.entrySet()) {
			String filePath = clientSyncDirectory + separator + entry.getKey(); // 设置下载路径
			if (hasDownloadedFile(filePath, toDownloadMap)) {
				count++; // 成功下载时增加计数
				log(INFO, "已下载: [" + count + "/" + toDownloadMapSize + "] " + filePath);
			} else {
				log(SEVERE, "下载失败: " + filePath);
				errorDownload = true; // 记录下载失败
			}
		}
		log(INFO, (errorDownload ? "下载失败" : "下载完成") + ": [" + count + "/" + toDownloadMapSize + "]");
		if (clientAutoStart) exit(0); // 自动退出
	}
	// 从服务器下载文件
	public static boolean hasDownloadedFile(String filePath, Map<String, Long> toDownloadMap) {
		if (clientThread == null) return false; // 客户端线程已关闭
		File clientFile = new File(filePath); // 目标本地文件
		Long requestCRC = toDownloadMap.get(filePath.substring(clientSyncDirectory.length() + 1));
		if (requestCRC == null) {
			log(SEVERE, "无法获取请求的校验码: " + clientFile);
			return false;
		}
		try {
			int responseCode = getResponseCode(String.format(
					"%s:%d/%s/%s",
					formatHTTP(serverAddress),
					clientPort,
					DOWNLOAD,
					requestCRC
			));
			if (responseCode != HTTP_OK) {
				log(SEVERE, "下载失败,错误代码: " + responseCode);
				return false;
			}
		} catch (Exception error) {
			log(SEVERE, "无法连接至服务器: " + error.getMessage());
			return false;
		}
		// 读取输入流并写入本地文件
		try (
				InputStream inputStream = HTTPURLConnection.getInputStream();
				FileOutputStream outputStream = new FileOutputStream(clientFile)
		) {
			byte[] buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
		} catch (IOException error) {
			log(SEVERE, "读取响应时出错: " + error.getMessage());
			return false;
		}
		// 校验下载的文件
		if (!requestCRC.equals(calculateCRC(clientFile))) {
			log(SEVERE, "校验失败,文件可能已损坏: " + clientFile);
			if (!clientFile.delete()) log(SEVERE, "无法删除损坏的文件: " + clientFile);
			return false;
		}
		return true; // 下载成功且校验通过
	}
	// 获取响应码
	public static int getResponseCode(String requestURL) throws IOException {
		HTTPURLConnection = (HttpURLConnection) new URL(requestURL).openConnection(); // 打开连接
		HTTPURLConnection.setRequestMethod(POST); // 设置请求方式为POST
		HTTPURLConnection.setConnectTimeout(5000); // 设置连接超时为5秒
		HTTPURLConnection.setReadTimeout(5000); // 设置读取超时为5秒
		return HTTPURLConnection.getResponseCode(); // 返回响应码
	}
	// 从服务器获取文件名和校验码列表
	public static Map<String, Long> fetchFileCRCList() {
		String URL = String.format("%s:%d/%s", formatHTTP(serverAddress), clientPort, LIST); // 服务器地址
		log(INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, Long> requestMap = new HashMap<>(); // 复制请求列表
		try {
			int responseCode = getResponseCode(URL);
			if (responseCode != HTTP_OK) {
				if (clientThread != null) log(SEVERE, "请求列表失败,错误代码: " + responseCode);
				errorDownload = true;
				return requestMap;
			}
		} catch (Exception error) {
			if (clientThread != null) log(SEVERE, "无法连接至服务器: " + error.getMessage());
			errorDownload = true;
			return requestMap;
		}
		try (
				BufferedReader bufferedReader =
						new BufferedReader(new InputStreamReader(HTTPURLConnection.getInputStream()))
		) {
			String fileName;
			while ((fileName = bufferedReader.readLine()) != null) { // 读取文件名
				requestMap.put(fileName, Long.parseLong(bufferedReader.readLine())); // 将文件名与校验码放入Map
			}
		} catch (IOException error) {
			log(SEVERE, "读取响应时出错: " + error.getMessage());
			errorDownload = true;
		}
		log(INFO, "获取到 [" + requestMap.size() + "] 个文件"); // 记录请求成功日志
		return requestMap;
	}
}
