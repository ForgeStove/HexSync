package com.forgestove.hexsync.config;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.ConfigEntry.*;
import com.forgestove.hexsync.util.*;
import com.forgestove.hexsync.util.Rate.Unit;

import java.util.List;
public class Config {
	public static final String //
		CONFIG_PATH = FileUtil.path(HexSync.NAME, "config.properties"), // 配置文件路径
		LOG_PATH = FileUtil.path(HexSync.NAME, "latest.log"), // 日志文件路径
		SERVER_PORT = "serverPort", // 服务端端口配置项
		SERVER_SYNC_DIRECTORY = "serverSyncDirectory", // 服务端同步文件夹路径配置项
		SERVER_UPLOAD_RATE = "serverUploadRate", // 上传速率限制配置项
		SERVER_AUTO_START = "serverAutoStart", // 服务端自动启动配置项
		CLIENT_PORT = "clientPort", // 客户端端口配置项
		REMOTE_ADDRESS = "remoteAddress", // 服务器地址配置项
		CLIENT_SYNC_DIRECTORY = "clientSyncDirectory", // 客户端同步文件夹路径配置项
		CLIENT_ONLY_DIRECTORY = "clientOnlyDirectory", // 仅客户端文件夹路径配置项
		CLIENT_AUTO_START = "clientAutoStart"; // 客户端自动启动配置项
	public static String //
		serverSyncDirectory = "mods", // 服务端同步文件夹路径，默认值mods
		clientSyncDirectory = "mods", // 客户端同步文件夹路径，默认值mods
		clientOnlyDirectory = "clientMods", // 仅客户端文件夹路径，默认值clientMods
		remoteAddress = "localhost"; // 服务器地址，默认值localhost
	public static Rate serverUploadRate = new Rate(1, Unit.Mbps); // 上传速率限制，默认值1 Mbps
	public static boolean clientAutoStart = false, serverAutoStart = false; // 客户端和服务端自动启动配置，默认值false
	public static int serverPort = 65535, clientPort = 65535; // 服务端和客户端端口，默认��65535
	// 配置项结构
	public static final List<ConfigEntry> CONFIG_ENTRIES = List.of(
		new HeaderEntry("# 服务端配置"),
		new ValueEntry<>(SERVER_PORT, () -> serverPort, v -> serverPort = Integer.parseInt(v)),
		new ValueEntry<>(SERVER_UPLOAD_RATE, () -> serverUploadRate, SettingUtil::setRate),
		new ValueEntry<>(SERVER_SYNC_DIRECTORY, () -> serverSyncDirectory, v -> serverSyncDirectory = v),
		new ValueEntry<>(SERVER_AUTO_START, () -> serverAutoStart, v -> serverAutoStart = Boolean.parseBoolean(v)),
		new HeaderEntry("# 客户端配置"),
		new ValueEntry<>(CLIENT_PORT, () -> clientPort, v -> clientPort = Integer.parseInt(v)),
		new ValueEntry<>(REMOTE_ADDRESS, () -> remoteAddress, v -> remoteAddress = v),
		new ValueEntry<>(CLIENT_SYNC_DIRECTORY, () -> clientSyncDirectory, v -> clientSyncDirectory = v),
		new ValueEntry<>(CLIENT_ONLY_DIRECTORY, () -> clientOnlyDirectory, v -> clientOnlyDirectory = v),
		new ValueEntry<>(CLIENT_AUTO_START, () -> clientAutoStart, v -> clientAutoStart = Boolean.parseBoolean(v))
	);
}
