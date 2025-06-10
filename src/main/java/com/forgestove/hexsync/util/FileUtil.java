package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Config;
import com.forgestove.hexsync.server.Server;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
public class FileUtil {
	// 初始化文件
	public static void initFiles(boolean isServer) {
		makeDirectory(isServer ? Config.serverSyncDirectory : Config.clientSyncDirectory);
		makeDirectory(HexSync.NAME);
		Config.loadConfig();
		if (isServer) Server.serverMap = initMap(Config.serverSyncDirectory);
		else {
			makeDirectory(Config.clientOnlyDirectory);
			Client.errorDownload = false;
		}
	}
	// 初始化文件名校验码键值对表
	public static @NotNull Map<String, String> initMap(String directory) {
		Map<String, String> map = new HashMap<>();
		var fileList = new File(directory).listFiles(); // 获取文件夹下的所有文件
		if (fileList != null) for (var file : fileList) {
			if (!file.isFile()) continue;
			map.put(file.getName(), calculateSHA1(file));
		}
		return map;
	}
	// 计算文件SHA1哈希值（带缓存）
	public static @Nullable String calculateSHA1(File file) {
		var cache = HashCache.getSHA1(file);
		if (cache != null) return cache;
		try (var fileInputStream = new FileInputStream(file)) {
			var digest = MessageDigest.getInstance("SHA-1");
			var buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) digest.update(buffer, 0, bytesRead);
			var hashBytes = digest.digest();
			var hexString = new StringBuilder();
			for (var b : hashBytes) hexString.append(String.format("%02x", b));
			var sha1 = hexString.toString();
			HashCache.putSHA1(file, sha1); // 写入缓存
			return sha1;
		} catch (Exception error) {
			Log.error("SHA1计算错误: " + error.getMessage());
			return null;
		}
	}
	// 创建文件夹
	public static void makeDirectory(String directoryPath) {
		var directory = new File(directoryPath);
		if (directory.isDirectory()) return;
		if (directory.mkdirs()) Log.info("文件夹已创建: " + directoryPath);
		else Log.error("无法创建文件夹: " + directoryPath);
	}
	// 删除指定路径下的文件
	public static void deleteFilesNotInMaps(Map<String, String> requestMap, Map<String, String> clientOnlyMap) {
		var fileList = new File(Config.clientSyncDirectory).listFiles();
		if (fileList == null) return;
		for (var file : fileList) {
			if (!file.isFile()) continue;
			var SHA1 = calculateSHA1(file);
			if (requestMap.containsValue(SHA1) || clientOnlyMap.containsValue(SHA1)) continue;
			deleteFile(file);
		}
	}
	// 删除指定文件
	public static void deleteFile(@NotNull File file) {
		if (!file.exists() || !file.isFile()) {
			Log.error("文件不存在: " + file);
			return;
		}
		if (file.delete()) Log.info("已删除文件: " + file);
		else Log.error("删除文件失败: " + file);
	}
	// 复制文件夹
	public static void copyDirectory(String source, String target) {
		makeDirectory(target);
		var fileList = new File(source).listFiles();
		if (fileList == null) return;
		for (var file : fileList) {
			var targetFileName = file.getName();
			var targetFile = new File(target, targetFileName);
			if (new File(target, targetFileName + ".disable").exists()) continue; // 跳过此文件
			if (file.isDirectory()) copyDirectory(String.valueOf(file), String.valueOf(targetFile));
			else if (!targetFile.exists()) try {
				Files.copy(file.toPath(), targetFile.toPath());
				Log.info("已复制: %s -> %s", file, target);
			} catch (IOException error) {
				Log.error("复制失败: " + error.getMessage());
			}
		}
	}
	// 将输入流写入文件
	public static boolean writeToFile(@NotNull InputStream inputStream, File targetFile) {
		try (var outputStream = new BufferedOutputStream(new FileOutputStream(targetFile))) {
			var buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
			return true;
		} catch (IOException error) {
			Log.error("写入文件失败: %s %s", targetFile, error.getMessage());
			return false;
		}
	}
}
