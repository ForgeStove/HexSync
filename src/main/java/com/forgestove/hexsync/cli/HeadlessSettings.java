package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
public class HeadlessSettings {
	public static final Map<String, Consumer<String[]>> COMMAND_MAP = createCommandMap();
	// 无头模式设置
	public static void headlessSettings() {
		System.out.println("进入设置模式,输入命令或输入HELP以获取帮助.");
		var scanner = new Scanner(System.in);
		while (true) try {
			System.out.print(HexSync.NAME + "Settings>");
			var parts = scanner.nextLine().trim().split("\\s+");
			if (parts.length == 0 || parts[0].isEmpty()) continue;
			var cmd = parts[0].toUpperCase();
			if ("EXIT".equals(cmd)) break;
			var action = COMMAND_MAP.get(cmd);
			if (action != null) {
				try {
					// 参数长度校验
					if (!validateArgs(cmd, parts)) continue;
					action.accept(parts);
					if ("SAVE".equals(cmd)) break;
				} catch (Exception error) {
					System.err.println("命令执行出错: " + error.getMessage());
				}
			} else {
				System.err.println("无效命令,输入HELP以获取帮助.");
			}
		} catch (Exception error) {
			System.err.println("无效命令,输入HELP以获取帮助.");
		}
	}
	public static boolean validateArgs(String cmd, String[] args) {
		// 参数数量校验
		return switch (cmd) {
			case "SP", "CP", "SA", "SD", "CD", "CO", "SS", "CS" -> checkArgs(args, 2);
			case "SL" -> checkArgs(args, 3);
			default -> true;
		};
	}
	public static boolean checkArgs(String[] args, int required) {
		if (args.length < required) {
			System.err.println("参数不足,输入HELP以获取帮助.");
			return false;
		}
		return true;
	}
	public static @NotNull Map<String, Consumer<String[]>> createCommandMap() {
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
			"HELP", args -> System.out.println("""
				可用命令:
				SP <端口>\t设置服务端端口
				SL <上传限速> <下载限速>\t设置限速
				SD <目录>\t设置服务端同步目录
				SS <true/false>\t设置服务端自动启动
				CP <端口>\t设置客户端端口
				SA <地址>\t设置服务器地址
				CD <目录>\t设置客户端同步目录
				CO <目录>\t设置仅客户端模组目录
				CS <true/false>\t设置客户端自动启动
				SAVE\t保存设置并退出
				EXIT\t退出设置模式
				HELP\t显示帮助信息""")
		);
		return map;
	}
}
