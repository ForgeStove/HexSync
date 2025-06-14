package com.forgestove.hexsync.util;
import com.forgestove.hexsync.config.Data;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
public class FileUtil {
	// 初始化文件名校验码键值对表
	public static @NotNull Map<String, String> initMap(@NotNull Path directory) {
		var fileList = directory.toFile().listFiles();
		var resultMap = new Object2ObjectOpenHashMap<String, String>();
		if (fileList == null) return resultMap;
		var concurrentResults = Arrays.stream(fileList).filter(File::isFile).parallel()
			.collect(Collectors.toConcurrentMap(File::getName, HashUtil::calculateSHA1, (existing, replacement) -> existing));
		resultMap.putAll(concurrentResults);
		return resultMap;
	}
	// 创建文件夹
	public static void makeDirectory(@NotNull Path directoryPath) {
		try {
			if (Files.isDirectory(directoryPath)) return;
			Files.createDirectories(directoryPath);
			Log.info("文件夹已创建: " + directoryPath);
		} catch (IOException error) {
			Log.error("无法创建文件夹: %s, 原因: %s".formatted(directoryPath, error.getMessage()));
		}
	}
	// 删除指定路径下的文件
	public static void deleteFilesNotInMaps(Map<String, String> requestMap, Map<String, String> clientOnlyMap) {
		var fileList = Data.clientSyncPath.get().toFile().listFiles();
		if (fileList == null) return;
		Arrays.stream(fileList).parallel().filter(File::isFile).forEach(file -> {
			var SHA1 = HashUtil.calculateSHA1(file);
			if (requestMap.containsValue(SHA1) || clientOnlyMap.containsValue(SHA1)) return;
			deleteFile(file);
		});
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
	public static void copyDirectory(@NotNull Path source, Path target) {
		makeDirectory(target);
		var fileList = source.toFile().listFiles();
		if (fileList == null) return;
		Arrays.stream(fileList).parallel().forEach(file -> {
			var targetFileName = file.getName();
			var targetFile = target.resolve(targetFileName).toFile();
			if (target.resolve(targetFileName + ".disable").toFile().exists()) return; // 跳过此文件
			if (file.isDirectory()) copyDirectory(file.toPath(), targetFile.toPath());
			else if (!targetFile.exists()) try {
				Files.copy(file.toPath(), targetFile.toPath());
				Log.info("已复制: %s -> %s".formatted(file, target));
			} catch (IOException error) {
				Log.error("复制失败: " + error.getMessage());
			}
		});
	}
	// 将输入流写入文件
	public static boolean writeToFile(@NotNull InputStream inputStream, File targetFile) {
		try (var outputStream = new BufferedOutputStream(new FileOutputStream(targetFile))) {
			var buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
			return true;
		} catch (IOException error) {
			Log.error("写入文件失败: %s %s".formatted(targetFile, error.getMessage()));
			return false;
		}
	}
	/**
	 * 按行处理文件内容
	 *
	 * @param file     要读取的文件
	 * @param consumer 处理每一行的函数
	 */
	public static void readLine(@NotNull File file, @NotNull Consumer<String> consumer) {
		try (var reader = Files.newBufferedReader(file.toPath())) {
			String line;
			while ((line = reader.readLine()) != null) consumer.accept(line);
		} catch (Exception error) {Log.error("文件读取失败: " + error.getMessage());}
	}
	/**
	 * 将字符串内容写入文件
	 *
	 * @param file    目标文件
	 * @param content 要写入的内容
	 */
	public static void writeFile(@NotNull File file, @NotNull String content) {
		try {
			Files.writeString(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (Exception error) {Log.error("文件写入失败: " + error.getMessage());}
	}
}
