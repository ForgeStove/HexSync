package com.forgestove.hexsync.util;
import com.forgestove.hexsync.config.Data;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
public class FileUtil {
	// 初始化文件名校验码键值对表
	public static @NotNull Object2ObjectMap<String, String> initMap(@NotNull Path directory) {
		var fileList = directory.toFile().listFiles();
		if (fileList == null) return new Object2ObjectOpenHashMap<>();
		var resultMap = new Object2ObjectOpenHashMap<String, String>(fileList.length);
		Arrays.stream(fileList).filter(File::isFile).parallel().forEach(file -> {
			var fileName = file.getName();
			synchronized (resultMap) {if (!resultMap.containsKey(fileName)) resultMap.put(fileName, HashUtil.calculateSHA1(file));}
		});
		return resultMap;
	}
	// 创建文件夹
	public static void makeDirectory(@NotNull Path directoryPath) {
		try {
			if (Files.isDirectory(directoryPath)) return;
			Files.createDirectories(directoryPath);
			Log.info("文件夹已创建: " + directoryPath);
		} catch (IOException e) {
			Log.error("无法创建文件夹: %s, 原因: %s".formatted(directoryPath, e.getMessage()));
		}
	}
	// 删除指定路径下的文件
	public static void deleteFilesNotInMaps(Object2ObjectMap<String, String> requestMap, Object2ObjectMap<String, String> clientMap) {
		var fileList = Data.clientSyncPath.get().toFile().listFiles();
		if (fileList == null) return;
		// 预先收集所有有效的SHA1值
		var validHashes = new HashSet<String>();
		validHashes.addAll(requestMap.values());
		validHashes.addAll(clientMap.values());
		Arrays.stream(fileList).parallel().filter(File::isFile).forEach(file -> {
			var sha1 = HashUtil.calculateSHA1(file);
			if (!validHashes.contains(sha1)) deleteFile(file);
		});
	}
	// 删除指定文件
	public static void deleteFile(@NotNull File file) {
		try {
			Files.delete(file.toPath());
			Log.info("已删除文件: " + file);
		} catch (NoSuchFileException e) {
			Log.error("文件不存在: " + file);
		} catch (DirectoryNotEmptyException e) {
			Log.error("目标是非空目录: " + file);
		} catch (Exception e) {
			Log.error("删除文件失败: %s, 原因: %s".formatted(file, e.getMessage()));
		}
	}
	// 复制文件夹
	public static void copyDirectory(@NotNull Path source, Path target) {
		makeDirectory(target);
		var fileList = source.toFile().listFiles();
		if (fileList == null) return;
		new ObjectArrayList<>(fileList).parallelStream().forEach(file -> {
			var targetFileName = file.getName();
			var targetPath = target.resolve(targetFileName);
			if (Files.exists(target.resolve(targetFileName + ".disable"))) return; // 跳过此文件
			try {
				if (file.isDirectory()) copyDirectory(file.toPath(), targetPath);
				else if (!Files.exists(targetPath)) {
					Files.copy(file.toPath(), targetPath, StandardCopyOption.COPY_ATTRIBUTES);
					Log.info("已复制: %s -> %s".formatted(file, targetPath));
				}
			} catch (IOException e) {
				Log.error("复制失败: %s -> %s, 原因: %s".formatted(file, targetPath, e.getMessage()));
			}
		});
	}
	/**
	 * 将输入流写入文件
	 *
	 * @param inputStream 输入流
	 * @param targetFile  目标文件
	 * @return 是否成功写入
	 */
	public static boolean writeToFile(@NotNull InputStream inputStream, File targetFile) {
		try (var in = inputStream; var outputStream = new FastBufferedOutputStream(new FileOutputStream(targetFile))) {
			var buffer = new byte[8192];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
			return true;
		} catch (IOException e) {
			Log.error("写入文件失败: %s %s".formatted(targetFile, e.getMessage()));
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
			reader.lines().forEach(consumer);
		} catch (Exception e) {
			Log.error("文件 %s 处理失败: %s".formatted(file, e.getMessage()));
		}
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
		} catch (Exception e) {Log.error("文件写入失败: " + e.getMessage());}
	}
	/**
	 * 执行指定的脚本文件<p>
	 * 根据操作系统类型选择适当的执行方式：<p>
	 * - Windows系统使用cmd命令执行<p>
	 * - Linux/Unix/Mac系统使用sh命令执行<p>
	 * 执行时会使用脚本所在目录作为工作目录，完全隔离IO
	 *
	 * @see Data#script
	 */
	public static void runScript() {
		try {
			var script = new File(Data.script.get().toString());
			ProcessBuilder processBuilder;
			var osName = System.getProperty("os.name").toLowerCase();
			if (osName.contains("win")) processBuilder = new ProcessBuilder("cmd", "/c", script.getName());
			else if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac"))
				processBuilder = new ProcessBuilder("sh", script.getName());
			else {
				Log.error("不支持的操作系统，无法执行预设脚本文件: " + osName);
				return;
			}
			var parentDir = script.getParentFile();
			if (parentDir != null) {
				processBuilder.directory(parentDir);
				Log.info("设置工作目录: " + parentDir.getAbsolutePath());
			}
			processBuilder.start();
			Log.info("执行脚本文件: " + script.getAbsolutePath());
		} catch (Exception e) {
			Log.error("脚本文件执行失败: " + e.getMessage());
		}
	}
}
