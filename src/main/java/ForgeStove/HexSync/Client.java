// Copyright (C) 2025 ForgeStove
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package ForgeStove.HexSync;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;

import static ForgeStove.HexSync.Config.*;
import static ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static ForgeStove.HexSync.Log.*;
import static ForgeStove.HexSync.Util.*;
import static java.io.File.separator;
import static java.lang.System.exit;
public class Client {
	public static Thread clientThread; // 客户端线程
	public static HttpURLConnection HTTPURLConnection; // 存储客户端连接实例
	public static boolean errorDownload; // 客户端下载文件时是否发生错误，影响客户端是否自动关闭
	public static boolean clientAutoStart; // 客户端自动启动，默认不自动启动
	public static int clientPort = 65535; // 客户端端口，默认值65535
	// 从服务器请求文件名和校验码列表
	public static Map<String, Long> requestFileCRCList() {
		String URL = formatHTTP(serverAddress) + ":" + clientPort + "/list"; // 服务器地址
		log(INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, Long> requestMap = new HashMap<>(); // 复制请求列表
		try {
			int responseCode = getResponseCode(new URI(URL).toURL());
			if (responseCode != HttpURLConnection.HTTP_OK) {
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
	// 从服务器下载文件
	public static boolean successDownloadFile(String filePath, Map<String, Long> toDownloadMap) {
		if (clientThread == null) return false; // 客户端线程已关闭
		File clientFile = new File(filePath); // 目标本地文件
		Long requestCRC = toDownloadMap.get(filePath.substring(clientSyncDirectory.length() + 1));
		if (requestCRC == null) {
			log(SEVERE, "无法获取请求的校验码: " + clientFile);
			return false;
		}
		try {
			int responseCode = getResponseCode(new URI(String.format(
					"%s:%d/download/%s",
					formatHTTP(serverAddress),
					clientPort,
					requestCRC
			)).toURL());
			if (responseCode != HttpURLConnection.HTTP_OK) {
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
	public static int getResponseCode(URL requestURL) throws IOException {
		HTTPURLConnection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
		HTTPURLConnection.setRequestMethod("GET"); // 设置请求方式为GET
		HTTPURLConnection.setConnectTimeout(5000); // 设置连接超时为5秒
		HTTPURLConnection.setReadTimeout(5000); // 设置读取超时为5秒
		return HTTPURLConnection.getResponseCode(); // 返回响应码
	}    // 停止客户端
	public static void stopClient() {
		if (clientThread == null || HTTPURLConnection == null) return;
		HTTPURLConnection.disconnect();
		clientThread = null;
		log(INFO, HEX_SYNC_NAME + "Client已关闭");
		if (clientAutoStart && !errorDownload) exit(0);
	}
	// 启动客户端
	public static void startClient() {
		if (clientThread != null) return;
		clientThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Client正在启动...");
			initFiles(false);
			Map<String, Long> requestMap = requestFileCRCList();
			if (!requestMap.isEmpty()) {
				deleteFilesNotInMaps(requestMap, initMap(clientOnlyDirectory)); // 删除多余文件
				Map<String, Long> clientMap = initMap(clientSyncDirectory); // 初始化客户端文件列表
				requestMap.entrySet().removeIf(entry -> clientMap.containsValue(entry.getValue()));
				downloadMissingFiles(requestMap);// 下载文件
				copyDirectory(clientOnlyDirectory, clientSyncDirectory);// 复制仅客户端模组文件夹中的文件到客户端同步文件夹
			}
			stopClient();
		});
		clientThread.start();
	}
	// 删除指定路径下的文件
	public static void deleteFilesNotInMaps(Map<String, Long> requestMap, Map<String, Long> clientOnlyMap) {
		File[] fileList = new File(clientSyncDirectory).listFiles();
		if (fileList != null) for (File file : fileList)
			if (file.isFile()) {
				long CRC = calculateCRC(file);
				if (requestMap.containsValue(CRC) || clientOnlyMap.containsValue(CRC)) continue;
				if (file.delete()) log(INFO, "已删除文件: " + file);
				else log(SEVERE, "删除文件失败: " + file);
			}
	}
	// 复制文件夹
	public static void copyDirectory(String source, String target) {
		makeDirectory(target);
		File[] fileList = new File(source).listFiles();
		if (fileList == null) return;
		try {
			for (File file : fileList) {
				String targetFileName = file.getName();
				File targetFile = new File(target, targetFileName);
				if (new File(target, targetFileName + ".disable").exists()) continue; // 跳过此文件
				if (file.isDirectory()) {
					copyDirectory(String.valueOf(file), String.valueOf(targetFile));
				} else if (!targetFile.exists()) {
					Files.copy(file.toPath(), targetFile.toPath());
					log(INFO, "已复制: " + file + " -> " + targetFile);
				}
			}
		} catch (IOException error) {
			log(SEVERE, "复制失败: " + error.getMessage());
		}
	}
	// 从服务端同步文件夹下载客户端缺少的文件
	public static void downloadMissingFiles(Map<String, Long> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			log(INFO, "模组已经是最新版本");
			return;
		}
		log(INFO, "开始下载 " + toDownloadMap.size() + " 个文件");
		int count = 0;
		int toDownloadMapSize = toDownloadMap.size();
		for (Map.Entry<String, Long> entry : toDownloadMap.entrySet()) {
			String filePath = clientSyncDirectory + separator + entry.getKey(); // 设置下载路径
			if (successDownloadFile(filePath, toDownloadMap)) {
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
}