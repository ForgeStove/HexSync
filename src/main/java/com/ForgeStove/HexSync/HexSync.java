package com.ForgeStove.HexSync;
import static com.ForgeStove.HexSync.CLI.CLI.initCLI;
import static com.ForgeStove.HexSync.Client.Client.*;
import static com.ForgeStove.HexSync.GUI.GUI.initGUI;
import static com.ForgeStove.HexSync.Server.Server.*;
import static com.ForgeStove.HexSync.Util.Config.*;
import static com.ForgeStove.HexSync.Util.Log.initLog;
public class HexSync {
	public static final String HEX_SYNC_NAME = "HexSync"; // 程序名称
	public static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync"; // 项目GitHub地址
	public static void main(String[] args) {
		initLog();
		loadConfig();
		if (serverAutoStart) runServer();
		if (clientAutoStart) runClient();
		if (HEADLESS) initCLI();
		else initGUI();
	}
}
