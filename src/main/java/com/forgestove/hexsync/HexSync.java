package com.forgestove.hexsync;
import com.forgestove.hexsync.cli.CLI;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.gui.GUI;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;

import java.awt.GraphicsEnvironment;
import java.util.*;
public class HexSync {
	public static final String //
		NAME = "HexSync", // 程序名称
		LICENSE = "MIT", // 程序许可证
		GITHUB_URL = "https://github.com/ForgeStove/HexSync", // GitHub地址
		LICENSE_URL = GITHUB_URL + "/blob/main/LICENSE"; // 许可证地址
	public static final ResourceBundle lang = ResourceBundle.getBundle(NAME, Locale.getDefault()); // 语言资源包，默认使用系统语言
	public static final boolean //
		HEADLESS = GraphicsEnvironment.isHeadless(),// 是否处于无头模式
		ANSI = System.getProperty("ansi", "true").equalsIgnoreCase("false"),// 是否启用ANSI控制台输出
		LOG = System.getProperty("log", "true").equalsIgnoreCase("true"); // 是否记录日志
	public static void main(String[] args) {
		if (LOG) Log.initLog();
		ConfigUtil.loadConfig();
		if (Data.serverAutoStart.get()) Server.runServer();
		if (Data.clientAutoStart.get()) Client.runClient();
		if (HEADLESS) new CLI().run();
		else GUI.runGUI();
	}
}
