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
package ForgeStove.HexSync.Client;
import ForgeStove.HexSync.Util.Settings;

import java.io.*;
import java.net.*;
import java.util.*;

import static ForgeStove.HexSync.Util.Config.serverAddress;
import static ForgeStove.HexSync.Util.Log.*;
public class FileCRCFetcher {
	// 从服务器获取文件名和校验码列表
	public static Map<String, Long> fetchFileCRCList() {
		String URL = Settings.formatHTTP(serverAddress) + ":" + Client.clientPort + "/list"; // 服务器地址
		log(INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, Long> requestMap = new HashMap<>(); // 复制请求列表
		try {
			int responseCode = Client.getResponseCode(new URI(URL).toURL());
			if (responseCode != HttpURLConnection.HTTP_OK) {
				if (Client.clientThread != null) log(SEVERE, "请求列表失败,错误代码: " + responseCode);
				Client.errorDownload = true;
				return requestMap;
			}
		} catch (Exception error) {
			if (Client.clientThread != null) log(SEVERE, "无法连接至服务器: " + error.getMessage());
			Client.errorDownload = true;
			return requestMap;
		}
		try (
				BufferedReader bufferedReader =
						new BufferedReader(new InputStreamReader(Client.HTTPURLConnection.getInputStream()))
		) {
			String fileName;
			while ((fileName = bufferedReader.readLine()) != null) { // 读取文件名
				requestMap.put(fileName, Long.parseLong(bufferedReader.readLine())); // 将文件名与校验码放入Map
			}
		} catch (IOException error) {
			log(SEVERE, "读取响应时出错: " + error.getMessage());
			Client.errorDownload = true;
		}
		log(INFO, "获取到 [" + requestMap.size() + "] 个文件"); // 记录请求成功日志
		return requestMap;
	}
}
