package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;

import java.util.*;
import java.util.function.Consumer;
public class HeadlessSettings {
	// 无头模式设置
	public static void headlessSettings() {
		Map<String, Consumer<String[]>> map = new HashMap<>();
		map.put("SP", args -> Settings.canSetPort(args[1], true));
		map.put("SL", args -> Settings.setRate(args[1] + " " + args[2]));
		map.put("SD", args -> Settings.setDirectory(args[1], "服务端同步", value -> Config.serverSyncDirectory = value));
		map.put("SS", args -> Settings.setAutoStart(args[1], true, value -> Server.serverAutoStart = value));
		map.put("CP", args -> Settings.canSetPort(args[1], false));
		map.put(
			"SA", args -> {
				Config.serverAddress = args[1];
				System.out.println("服务器地址已设置为: " + Config.serverAddress);
			}
		);
		map.put("CD", args -> Settings.setDirectory(args[1], "客户端同步", value -> Config.clientSyncDirectory = value));
		map.put("CO", args -> Settings.setDirectory(args[1], "仅客户端模组", value -> Config.clientOnlyDirectory = value));
		map.put("CS", args -> Settings.setAutoStart(args[1], false, value -> Client.clientAutoStart = value));
		map.put("SAVE", args -> Config.saveConfig());
		map.put(
			"HELP", args -> {
				for (var message : new String[]{
					"SP [端口]                      |设置服务端端口",
					"SL [整型] [B/s|KB/s|MB/s|GB/s] |设置服务端最大上传速率",
					"SD [目录]                      |设置服务端同步目录",
					"SS [Y|N]                       |设置服务端自动启动",
					"CP [端口]                      |设置客户端端口",
					"SA [地址]                      |设置服务器地址",
					"CD [目录]                      |设置客户端同步目录",
					"CO [目录]                      |设置客户端仅客户端目录",
					"CS [Y|N]                       |设置客户端自动启动",
					"SAVE                           |保存并退出设置",
					"EXIT                           |退出设置而不保存",
					"HELP                           |帮助"
				})
					System.out.println(message);
			}
		);
		System.out.println("进入设置模式,输入命令或输入HELP以获取帮助.");
		var scanner = new Scanner(System.in);
		while (true) try {
			System.out.print(HexSync.NAME + "Settings>");
			var parts = scanner.nextLine().split("\\s+");
			if (parts.length == 0) continue;
			if (parts[0].equalsIgnoreCase("EXIT")) break;
			map.getOrDefault(parts[0].toUpperCase(), args -> System.err.println("无效命令,输入HELP以获取帮助.")).accept(parts);
			if (parts[0].equalsIgnoreCase("SAVE")) break;
		} catch (Exception error) {
			System.err.println("无效命令,输入HELP以获取帮助.");
		}
	}
}
