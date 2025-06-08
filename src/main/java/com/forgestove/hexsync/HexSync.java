package com.forgestove.hexsync;
import com.forgestove.hexsync.cli.CLI;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.gui.GUI;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;
public class HexSync {
	public static final String //
		NAME = "HexSync", // 程序名称
		GITHUB_URL = "https://github.com/ForgeStove/HexSync"; // 项目GitHub地址
	public static void main(String[] args) {
		if (Config.LOG) Log.initLog();
		Config.loadConfig();
		if (Server.serverAutoStart) Server.runServer();
		if (Client.clientAutoStart) Client.runClient();
		if (Config.HEADLESS) CLI.initCLI();
		else GUI.initGUI();
	}
}
