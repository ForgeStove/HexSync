package src.main.java.ForgeStove;
import static src.main.java.ForgeStove.Client.*;
import static src.main.java.ForgeStove.Config.loadConfig;
import static src.main.java.ForgeStove.HeadlessUI.headlessUI;
import static src.main.java.ForgeStove.Log.initLog;
import static src.main.java.ForgeStove.NormalUI.*;
import static src.main.java.ForgeStove.Server.*;
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