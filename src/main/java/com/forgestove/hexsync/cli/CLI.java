package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;

import java.util.*;
public class CLI {
	// 命令行界面
	public static void initCLI() {
		System.out.println("欢迎使用" + HexSync.HEX_SYNC + "!\n输入HELP以获取帮助.");
		System.out.println();
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", Server::runServer);
		map.put("RC", Client::runClient);
		map.put("SS", Server::stopServer);
		map.put("SC", Client::stopClient);
		map.put("SET", HeadlessSettings::headlessSettings);
		map.put("GITHUB", () -> System.out.println(HexSync.GITHUB_URL));
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
					System.out.println(message);
			}
		);
		map.put("EXIT", () -> System.exit(0));
		var scanner = new Scanner(System.in);
		while (true) try {
			System.out.print(HexSync.HEX_SYNC + ">");
			var line = scanner.nextLine().toUpperCase();
			if (line.isEmpty()) continue;
			map.getOrDefault(line, () -> System.err.println("无效命令,输入HELP以获取帮助.")).run();
		} catch (Exception error) {
			Log.error("命令处理时出错: " + error.getMessage());
			break;
		}
	}
}
