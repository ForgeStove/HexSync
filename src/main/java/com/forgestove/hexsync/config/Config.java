package com.forgestove.hexsync.config;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;
import org.jetbrains.annotations.NotNull;

import java.awt.GraphicsEnvironment;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
public class Config {
	public static final String //
		CONFIG_PATH = HexSync.NAME + File.separator + "config.properties", // 配置文件路径
		LOG_PATH = HexSync.NAME + File.separator + "latest.log", // 日志文件路径
		SERVER_PORT = "serverPort", // 服务端端口配置项
		SERVER_SYNC_DIRECTORY = "serverSyncDirectory", // 服务端同步文件夹路径配置项
		SERVER_UPLOAD_RATE_LIMIT = "serverUploadRateLimit", // 上传速率限制配置项
		SERVER_AUTO_START = "serverAutoStart", // 服务端自动启动配置项
		CLIENT_PORT = "clientPort", // 客户端端口配置项
		SERVER_ADDRESS = "serverAddress", // 服务器地址配置项
		CLIENT_SYNC_DIRECTORY = "clientSyncDirectory", // 客户端同步文件夹路径配置项
		CLIENT_ONLY_DIRECTORY = "clientOnlyDirectory", // 仅客户端文件夹路径配置项
		CLIENT_AUTO_START = "clientAutoStart"; // 客户端自动启动配置项
	public static final boolean //
		HEADLESS = GraphicsEnvironment.isHeadless(), // 是否处于无头模式
		ANSI = System.getProperty("ansi", "true").equalsIgnoreCase("false"), // 是否启用ANSI控制台输出
		LOG = System.getProperty("log", "true").equalsIgnoreCase("true"); // 是否记录日志
	public static String //
		serverSyncDirectory = "mods", // 服务端同步文件夹路径，默认值mods
		clientSyncDirectory = "mods", // 客户端同步文件夹路径，默认值mods
		clientOnlyDirectory = "clientMods", // 仅客户端文件夹路径，默认值clientMods
		serverAddress = "localhost"; // 服务器地址，默认值localhost
	public static RateUnit serverUploadRateLimitUnit = RateUnit.MBPS; // 上传速率限制单位，默认MBps
	public static long serverUploadRateLimit = 1, maxUploadRateInBytes; // 上传速率限制值以及对应的字节数
	// 加载配置
	public static void loadConfig() {
		var configFile = new File(CONFIG_PATH);
		if (!configFile.isFile()) {
			saveConfig();
			return;
		}
		try (var bufferedReader = new BufferedReader(new FileReader(configFile))) {
			var configMap = createConfigMap();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (!line.matches("^[a-zA-Z].*")) continue; // 仅当首字符不是字母时跳过
				var parts = line.trim().split("=");
				if (parts.length != 2) {
					Log.warn("配置格式错误: " + line);
					continue;
				}
				var action = configMap.get(parts[0]);
				if (action != null) action.accept(parts[1]);
				else Log.warn("配置项错误: " + line);
			}
		} catch (IOException error) {
			Log.error("配置读取失败: " + error.getMessage());
		}
	}
	// 创建配置映射
	public static @NotNull Map<String, Consumer<String>> createConfigMap() {
		Map<String, Consumer<String>> configMap = new HashMap<>();
		configMap.put(SERVER_PORT, input -> Server.serverPort = Integer.parseInt(input));
		configMap.put(SERVER_UPLOAD_RATE_LIMIT, Settings::setRate);
		configMap.put(SERVER_SYNC_DIRECTORY, input -> serverSyncDirectory = input);
		configMap.put(SERVER_AUTO_START, input -> Server.serverAutoStart = Boolean.parseBoolean(input));
		configMap.put(CLIENT_PORT, input -> Client.clientPort = Integer.parseInt(input));
		configMap.put(SERVER_ADDRESS, input -> serverAddress = input);
		configMap.put(CLIENT_SYNC_DIRECTORY, input -> clientSyncDirectory = input);
		configMap.put(CLIENT_ONLY_DIRECTORY, input -> clientOnlyDirectory = input);
		configMap.put(CLIENT_AUTO_START, input -> Client.clientAutoStart = Boolean.parseBoolean(input));
		return configMap;
	}
	// 保存配置
	public static void saveConfig() {
		var configEntries = new Object[][]{
			{"# 服务端配置"},
			{SERVER_PORT, Server.serverPort},
			{SERVER_UPLOAD_RATE_LIMIT, serverUploadRateLimit + " " + serverUploadRateLimitUnit.name()},
			{SERVER_SYNC_DIRECTORY, serverSyncDirectory},
			{SERVER_AUTO_START, Server.serverAutoStart},
			{"# 客户端配置"},
			{CLIENT_PORT, Client.clientPort},
			{SERVER_ADDRESS, serverAddress},
			{CLIENT_SYNC_DIRECTORY, clientSyncDirectory},
			{CLIENT_ONLY_DIRECTORY, clientOnlyDirectory},
			{CLIENT_AUTO_START, Client.clientAutoStart}
		};
		var joiner = new StringJoiner(System.lineSeparator());
		for (var config : configEntries) {
			if (config[0].toString().startsWith("#")) joiner.add(config[0].toString());
			else joiner.add(String.format("%s=%s", config[0], config.length > 1 ? config[1] : ""));
		}
		var configFile = new File(CONFIG_PATH);
		try (var bufferedWriter = new BufferedWriter(new FileWriter(configFile))) {
			bufferedWriter.write(joiner.toString());
			Log.info("配置已保存: %s%s", System.lineSeparator(), joiner);
		} catch (IOException error) {
			Log.error("配置保存失败: " + error.getMessage());
		}
	}
}
