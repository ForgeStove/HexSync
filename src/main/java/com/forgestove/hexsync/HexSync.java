package com.forgestove.hexsync;
import com.forgestove.hexsync.cli.CLI;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Config;
import com.forgestove.hexsync.gui.GUI;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;

import java.util.*;
public class HexSync {
	public static final String //
		NAME = "HexSync", // 程序名称
		LICENSE = "MIT", // 程序许可证
		GITHUB_URL = "https://github.com/ForgeStove/HexSync", // GitHub地址
		LICENSE_URL = GITHUB_URL + "/blob/main/LICENSE"; // 许可证地址
	public static final ResourceBundle lang = ResourceBundle.getBundle("lang/" + NAME, Locale.getDefault());
	public static void main(String[] args) {
		if (Config.LOG) Log.initLog();
		Config.loadConfig();
		if (Server.serverAutoStart) Server.runServer();
		if (Client.clientAutoStart) Client.runClient();
		if (Config.HEADLESS) CLI.runCLI();
		else GUI.runGUI();
	}
}
