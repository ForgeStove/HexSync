package ForgeStove;
import java.util.*;
import java.util.function.Consumer;

import static ForgeStove.Client.clientAutoStart;
import static ForgeStove.Config.*;
import static ForgeStove.HexSync.*;
import static ForgeStove.Log.*;
import static ForgeStove.Server.serverAutoStart;
import static ForgeStove.Utils.*;
import static java.io.File.separator;
import static java.lang.System.*;
public class HeadlessUI {
	public static void headlessUI() {
		out.println("欢迎使用" + HEX_SYNC_NAME + "!");
		out.println("输入HELP以获取帮助.");
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", Server::startServer);
		map.put("RC", Client::startClient);
		map.put("SS", Server::stopServer);
		map.put("SC", Client::stopClient);
		map.put("SET", HeadlessUI::headlessSettings);
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
	// 无头模式设置
	public static void headlessSettings() {
		Map<String, Consumer<String[]>> map = new HashMap<>();
		map.put("SP", args -> getPort(args[1], true));
		map.put("SL", args -> setRate(args[1] + " " + args[2]));
		map.put("SD", args -> setDirectory(args[1], "服务端同步", value -> serverSyncDirectory = value));
		map.put("SS", args -> setAutoStart(args[1], true, value -> serverAutoStart = value));
		map.put("CP", args -> getPort(args[1], false));
		map.put(
				"SA", args -> {
					serverAddress = args[1];
					out.println("服务器地址已设置为: " + serverAddress);
				}
		);
		map.put("CD", args -> setDirectory(args[1], "客户端同步", value -> clientSyncDirectory = value));
		map.put("CO", args -> setDirectory(args[1], "仅客户端模组", value -> clientOnlyDirectory = value));
		map.put("CS", args -> setAutoStart(args[1], false, value -> clientAutoStart = value));
		map.put("SAVE", args -> saveConfig());
		map.put(
				"HELP", args -> printlnStrings(new String[]{
						"SP [端口号]            |设置服务端端口",
						"SL [速率] [B/KB/MB/GB] |设置服务端最大上传速率",
						"SD [目录]              |设置服务端同步目录",
						"SS [Y/N]               |设置服务端自动启动",
						"CP [端口号]            |设置客户端端口",
						"SA [地址]              |设置服务器地址",
						"CD [目录]              |设置客户端同步目录",
						"CO [目录]              |设置客户端仅客户端目录",
						"CS [Y/N]               |设置客户端自动启动",
						"SAVE                   |保存并退出设置",
						"EXIT                   |退出设置而不保存",
						"HELP                   |帮助"
				})
		);
		out.println("进入设置模式,输入命令或输入HELP以获取帮助.");
		Scanner scanner = new Scanner(in);
		while (true) try {
			out.print(HEX_SYNC_NAME + "Settings>");
			String[] parts = scanner.nextLine().split("\\s+");
			if (parts.length == 0) continue;
			if (parts[0].equalsIgnoreCase("EXIT")) break;
			map.getOrDefault(parts[0].toUpperCase(), args -> err.println("无效命令,输入HELP以获取帮助.")).accept(parts);
			if (parts[0].equalsIgnoreCase("SAVE")) break;
		} catch (Exception error) {
			err.println("无效命令,输入HELP以获取帮助.");
		}
	}
	// 设置文件夹路径
	public static void setDirectory(String directory, String log, Consumer<String> consumer) {
		if (!directory.isEmpty() && !directory.contains(separator)) {
			consumer.accept(directory);
			out.println(log + "文件夹路径已设置为: " + directory);
		} else err.println("路径格式错误,请输入绝对路径或相对路径.");
	}
	// 设置自动启动
	public static void setAutoStart(String input, Boolean isServer, Consumer<Boolean> consumer) {
		if (input.matches("[yYnN]")) {
			boolean value = input.matches("[yY]");
			consumer.accept(value);
			out.println(isServer ? "服务端" : "客户端" + "自动启动已设置为: " + value);
		} else err.println("无效输入,请输入Y/N.");
	}
	// 输出帮助信息的通用方法
	public static void printlnStrings(String[] messages) {
		for (String message : messages) out.println(message);
	}
}
