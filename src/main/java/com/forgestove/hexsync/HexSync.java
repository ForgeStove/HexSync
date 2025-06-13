package com.forgestove.hexsync;
import com.forgestove.hexsync.cli.CLI;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.gui.GUI;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;
import org.jetbrains.annotations.NotNull;

import java.awt.GraphicsEnvironment;
import java.util.*;
public class HexSync {
	public static final String //
		NAME = "HexSync", // 程序名称
		LICENSE = "MIT", // 程序许可证
		GITHUB_URL = "https://github.com/ForgeStove/HexSync", // GitHub地址
		LICENSE_URL = GITHUB_URL + "/blob/main/LICENSE"; // 许可证地址
	public static final boolean //
		HEADLESS = GraphicsEnvironment.isHeadless(),// 是否处于无头模式
		ANSI = System.getProperty("ansi", "true").equalsIgnoreCase("false");// 是否启用ANSI控制台输出
	public static final ResourceBundle lang = ResourceBundle.getBundle(NAME, Locale.getDefault()); // 语言
	public static void main(String[] args) {
		Log.initLog();
		ConfigUtil.loadConfig();
		if (Data.serverAuto.get()) Server.start();
		if (Data.clientAuto.get()) Client.start();
		if (HEADLESS) CLI.start();
		else GUI.start();
	}
	public static @NotNull String get(String key) {return lang.getString(key);}
}
