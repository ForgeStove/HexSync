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
package ForgeStove.HexSync.Util;
import ForgeStove.HexSync.Server.Server;

import java.io.*;
import java.util.*;

import static ForgeStove.HexSync.Client.Client.errorDownload;
import static ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static ForgeStove.HexSync.Util.Config.*;
import static ForgeStove.HexSync.Util.Log.*;
public class Files {
	// 初始化文件
	public static void initFiles(boolean isServer) {
		makeDirectory(isServer ? serverSyncDirectory : clientSyncDirectory);
		makeDirectory(HEX_SYNC_NAME);
		loadConfig();
		if (isServer) Server.serverMap = initMap(serverSyncDirectory);
		else {
			makeDirectory(clientOnlyDirectory);
			errorDownload = false;
		}
	}
	// 初始化文件名校验码键值对表
	public static Map<String, Long> initMap(String directory) {
		Map<String, Long> map = new HashMap<>();
		File[] fileList = new File(directory).listFiles(); // 获取文件夹下的所有文件
		if (fileList != null) for (File file : fileList)
			if (file.isFile()) map.put(file.getName(), Checksum.calculateCRC(file));
		return map;
	}
	// 创建文件夹
	public static void makeDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.isDirectory()) return;
		if (directory.mkdirs()) log(INFO, "文件夹已创建: " + directoryPath);
		else log(SEVERE, "无法创建文件夹: " + directoryPath);
	}
	// 删除指定路径下的文件
	public static void deleteFilesNotInMaps(Map<String, Long> requestMap, Map<String, Long> clientOnlyMap) {
		File[] fileList = new File(clientSyncDirectory).listFiles();
		if (fileList != null) for (File file : fileList)
			if (file.isFile()) {
				long CRC = Checksum.calculateCRC(file);
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
					java.nio.file.Files.copy(file.toPath(), targetFile.toPath());
					log(INFO, "已复制: " + file + " -> " + targetFile);
				}
			}
		} catch (IOException error) {
			log(SEVERE, "复制失败: " + error.getMessage());
		}
	}
}
