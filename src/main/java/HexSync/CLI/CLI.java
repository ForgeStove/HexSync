package HexSync.CLI;
import HexSync.Client.Client;
import HexSync.Server.Server;

import java.util.*;

import static HexSync.HexSync.*;
import static HexSync.Util.Log.*;
import static HexSync.Util.Println.printlnStrings;
import static java.lang.System.*;
public class CLI {
	// 命令行界面
	public static void initCLI() {
		out.println("欢迎使用" + HEX_SYNC_NAME + "!");
		out.println("输入HELP以获取帮助.");
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", Server::runServer);
		map.put("RC", Client::runClient);
		map.put("SS", Server::stopServer);
		map.put("SC", Client::stopClient);
		map.put("SET", HeadlessSettings::headlessSettings);
		map.put("GITHUB", () -> out.println(GITHUB_URL));
		map.put(
				"HELP", () -> printlnStrings(new String[]{
						"RS     |启动服务端",
						"RC     |启动客户端",
						"SS     |停止服务端",
						"SC     |停止客户端",
						"SET    |设置",
						"GITHUB |仓库",
						"EXIT   |退出",
						"HELP   |帮助"
				})
		);
		map.put("EXIT", () -> exit(0));
		Scanner scanner = new Scanner(in);
		while (true) try {
			out.print(HEX_SYNC_NAME + ">");
			String line = scanner.nextLine().toUpperCase();
			if (line.isEmpty()) continue;
			map.getOrDefault(line, () -> err.println("无效命令,输入HELP以获取帮助.")).run();
		} catch (Exception error) {
			log(SEVERE, "命令处理时出错: " + error.getMessage());
			break;
		}
	}
}
