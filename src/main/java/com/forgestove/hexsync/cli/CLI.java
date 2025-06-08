package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;
import org.jetbrains.annotations.NotNull;

import java.util.*;
public class CLI {
	// 命令行界面
	public static void initCLI() {
		System.out.printf("欢迎使用%s!\n输入 HELP 或 ? 以获取帮助%n", HexSync.NAME);
		var map = createCommandMap();
		var scanner = new Scanner(System.in);
		while (true) try {
			System.out.print(HexSync.NAME + ">");
			var line = scanner.nextLine().toUpperCase();
			if (line.isEmpty()) continue;
			map.getOrDefault(line, () -> System.err.println("无效命令,输入HELP以获取帮助")).run();
		} catch (Exception error) {
			Log.error("命令处理时出错: " + error.getMessage());
			break;
		}
	}
	public static @NotNull Map<String, Runnable> createCommandMap() {
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", Server::runServer);
		map.put("RC", Client::runClient);
		map.put("SS", Server::stopServer);
		map.put("SC", Client::stopClient);
		map.put("SET", HeadlessSettings::headlessSettings);
		map.put("GITHUB", () -> System.out.println(HexSync.GITHUB_URL));
		map.put(
			"HELP", () -> System.out.println("""
				RS\t|启动服务端
				RC\t|启动客户端
				SS\t|停止服务端
				SC\t|停止客户端
				SET\t|设置
				GITHUB\t|仓库
				EXIT\t|退出
				HELP\t|帮助
				""")
		);
		map.put("?", map.get("HELP"));
		map.put("EXIT", () -> System.exit(0));
		return map;
	}
}
