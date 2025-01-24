package ForgeStove.HexSync.Util;
import java.io.*;
import java.util.*;

import static ForgeStove.HexSync.Client.Client.errorDownload;
import static ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static ForgeStove.HexSync.Server.Server.serverMap;
import static ForgeStove.HexSync.Util.Checksum.calculateCRC;
import static ForgeStove.HexSync.Util.Config.*;
import static ForgeStove.HexSync.Util.Log.*;
import static java.lang.String.valueOf;
import static java.nio.file.Files.copy;
public class Files {
	// 初始化文件
	public static void initFiles(boolean isServer) {
		makeDirectory(isServer ? serverSyncDirectory : clientSyncDirectory);
		makeDirectory(HEX_SYNC_NAME);
		loadConfig();
		if (isServer) serverMap = initMap(serverSyncDirectory);
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
			if (file.isFile()) map.put(file.getName(), calculateCRC(file));
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
					copyDirectory(valueOf(file), valueOf(targetFile));
				} else if (!targetFile.exists()) {
					copy(file.toPath(), targetFile.toPath());
					log(INFO, "已复制: " + file + " -> " + target);
				}
			}
		} catch (IOException error) {
			log(SEVERE, "复制失败: " + error.getMessage());
		}
	}
}
