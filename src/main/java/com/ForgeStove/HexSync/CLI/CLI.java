package com.ForgeStove.HexSync.CLI;
import com.ForgeStove.HexSync.Client.Client;
import com.ForgeStove.HexSync.Server.Server;

import java.util.*;

import static com.ForgeStove.HexSync.HexSync.*;
import static com.ForgeStove.HexSync.Util.Log.*;
import static java.lang.System.*;
public class CLI {
	// 命令行界面
	public static void initCLI() {
		out.println("欢迎使用" + HEX_SYNC_NAME + "!\n输入HELP以获取帮助.");
		out.println();
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", Server::runServer);
		map.put("RC", Client::runClient);
		map.put("SS", Server::stopServer);
		map.put("SC", Client::stopClient);
		map.put("SET", HeadlessSettings::headlessSettings);
		map.put("GITHUB", () -> out.println(GITHUB_URL));
		map.put(
				"HELP", () -> {
					for (var message : new String[]{
							"RS     |启动服务端",
							"RC     |启动客户端",
							"SS     |停止服务端",
							"SC     |停止客户端",
							"SET    |设置",
							"GITHUB |仓库",
							"EXIT   |退出",
							"HELP   |帮助"
					})
						out.println(message);
				}
		);
		map.put("EXIT", () -> exit(0));
		var scanner = new Scanner(in);
		while (true) try {
			out.print(HEX_SYNC_NAME + ">");
			var line = scanner.nextLine().toUpperCase();
			if (line.isEmpty()) continue;
			map.getOrDefault(line, () -> err.println("无效命令,输入HELP以获取帮助.")).run();
		} catch (Exception error) {
			log(SEVERE, "命令处理时出错: " + error.getMessage());
			break;
		}
	}
}
