package HexSync;
import static HexSync.CLI.CLI.initCLI;
import static HexSync.Client.Client.*;
import static HexSync.GUI.GUI.initGUI;
import static HexSync.Server.Server.*;
import static HexSync.Util.Config.loadConfig;
import static HexSync.Util.Log.initLog;
import static HexSync.Util.Settings.HEADLESS;
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
