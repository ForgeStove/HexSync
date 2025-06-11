package com.forgestove.hexsync.config;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.ConfigEntry.*;
import com.forgestove.hexsync.util.*;
import com.forgestove.hexsync.util.Rate.Unit;

import java.util.List;
public class Data {
	public static final String CONFIG_PATH = FileUtil.path(HexSync.NAME, "config.properties"); // 配置文件路径
	public static final String LOG_PATH = FileUtil.path(HexSync.NAME, "latest.log"); // 日志文件路径
	public static final Config<String> serverSyncDirectory = new Config<>("mods"); // 服务端同步文件夹路径
	public static final Config<String> clientSyncDirectory = new Config<>("mods"); // 客户端同步文件夹路径
	public static final Config<String> clientOnlyDirectory = new Config<>("clientMods"); // 仅客户端文件夹路径
	public static final Config<String> remoteAddress = new Config<>("localhost"); // 服务器地址
	public static final Config<Rate> serverUploadRate = new Config<>(new Rate(1, Unit.Mbps)); // 上传速率限制
	public static final Config<Boolean> clientAutoStart = new Config<>(false); // 客户端自动启动
	public static final Config<Boolean> serverAutoStart = new Config<>(false); // 服务端自动启动
	public static final Config<Port> serverPort = new Config<>(new Port(65535)); // 服务端端口
	public static final Config<Port> clientPort = new Config<>(new Port(65535)); // 客户端端口
	// 配置文件结构
	public static final List<ConfigEntry> CONFIG_ENTRIES = List.of(
		new HeaderEntry("# 服务端配置"), ValueEntry.value("serverPort", serverPort, Port::new),
		ValueEntry.value("serverUploadRate", serverUploadRate, Rate::fromString),
		ValueEntry.value("serverSyncDirectory", serverSyncDirectory),
		ValueEntry.value("serverAutoStart", serverAutoStart, Boolean::parseBoolean),
		new HeaderEntry("# 客户端配置"), ValueEntry.value("clientPort", clientPort, Port::new),
		ValueEntry.value("remoteAddress", remoteAddress),
		ValueEntry.value("clientSyncDirectory", clientSyncDirectory),
		ValueEntry.value("clientOnlyDirectory", clientOnlyDirectory),
		ValueEntry.value("clientAutoStart", clientAutoStart, Boolean::parseBoolean)
	);
}
