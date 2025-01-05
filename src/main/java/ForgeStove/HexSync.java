package ForgeStove;
import static ForgeStove.Client.*;
import static ForgeStove.Config.loadConfig;
import static ForgeStove.HeadlessUI.headlessUI;
import static ForgeStove.Log.initLog;
import static ForgeStove.NormalUI.*;
import static ForgeStove.Server.*;
public class HexSync {
	public static final String HEX_SYNC_NAME = "HexSync"; // 程序名称
	public static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync"; // 项目GitHub地址
	public static void main(String[] args) {
		initLog();
		loadConfig();
		if (serverAutoStart) startServer();
		if (clientAutoStart) startClient();
		if (HEADLESS) headlessUI();
		else normalUI();
	}
}