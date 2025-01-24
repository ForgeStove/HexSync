package HexSync.Client;
import java.io.*;
import java.net.*;
import java.util.Map;

import static HexSync.Client.Client.*;
import static HexSync.Util.Checksum.calculateCRC;
import static HexSync.Util.Config.*;
import static HexSync.Util.Log.*;
import static HexSync.Util.Settings.formatHTTP;
import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.System.exit;
public class FileDownloader {
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
			if (canDownloadFile(filePath, toDownloadMap)) {
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
	public static boolean canDownloadFile(String filePath, Map<String, Long> toDownloadMap) {
		if (clientThread == null) return false; // 客户端线程已关闭
		File clientFile = new File(filePath); // 目标本地文件
		Long requestCRC = toDownloadMap.get(filePath.substring(clientSyncDirectory.length() + 1));
		if (requestCRC == null) {
			log(SEVERE, "无法获取请求的校验码: " + clientFile);
			return false;
		}
		try {
			int responseCode = getResponseCode(new URI(format(
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
}
