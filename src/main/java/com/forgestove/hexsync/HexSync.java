package com.forgestove.hexsync;
import com.forgestove.hexsync.cli.CLI;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.gui.GUI;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
/**
 * HexSync主类 - 程序的入口点。<p>
 * 提供核心功能并控制服务器、客户端和用户界面组件的初始化和启动。
 */
public class HexSync {
	public static final String NAME = "HexSync";
	/** 是否在无图形界面模式下运行 */
	public static final boolean HEADLESS = Boolean.getBoolean("java.awt.headless");
	public static final ResourceBundle lang = ResourceBundle.getBundle(NAME, Locale.getDefault());
	/**
	 * 程序入口点，初始化日志、配置，并根据配置启动服务器、客户端和用户界面。
	 *
	 * @param args 命令行参数
	 */
	public static void main(String[] args) {
		Log.init();
		ConfigUtil.load();
		if (Data.serverAuto.get()) Server.start();
		if (Data.clientAuto.get()) Client.start();
		if (HEADLESS) CLI.start();
		else GUI.start();
		if (Data.script.get() != null) FileUtil.runScript();
	}
	/**
	 * 获取指定键的国际化字符串。
	 *
	 * @param key 资源键
	 * @return 对应的国际化字符串
	 */
	public static @NotNull String get(String key) {
		try {
			return lang.getString(key);
		} catch (Exception error) {
			Log.warn("Missing resource for key: " + key);
			return key;
		}
	}
}
