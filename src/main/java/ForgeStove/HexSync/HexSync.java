package ForgeStove.HexSync;
import static ForgeStove.HexSync.CLI.CLI.initCLI;
import static ForgeStove.HexSync.Client.Client.*;
import static ForgeStove.HexSync.GUI.GUI.initGUI;
import static ForgeStove.HexSync.Server.Server.*;
import static ForgeStove.HexSync.Util.Config.loadConfig;
import static ForgeStove.HexSync.Util.Log.initLog;
import static ForgeStove.HexSync.Util.Settings.HEADLESS;
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
