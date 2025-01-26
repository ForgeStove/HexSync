package ForgeStove.HexSync.Client;
import java.io.*;
import java.net.URI;
import java.util.*;

import static ForgeStove.HexSync.Client.Client.*;
import static ForgeStove.HexSync.Util.Config.serverAddress;
import static ForgeStove.HexSync.Util.Log.*;
import static ForgeStove.HexSync.Util.Settings.formatHTTP;
import static java.net.HttpURLConnection.HTTP_OK;
public class FileCRCFetcher {
	// 从服务器获取文件名和校验码列表
	public static Map<String, Long> fetchFileCRCList() {
		String URL = formatHTTP(serverAddress) + ":" + clientPort + "/list"; // 服务器地址
		log(INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, Long> requestMap = new HashMap<>(); // 复制请求列表
		try {
			int responseCode = getResponseCode(new URI(URL).toURL());
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
