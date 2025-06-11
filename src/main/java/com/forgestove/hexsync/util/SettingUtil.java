package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Config;
import com.forgestove.hexsync.util.Rate.Unit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Consumer;
public class SettingUtil {
	// 字符串转端口
	public static boolean canSetPort(String portInput, boolean isServer) {
		var side = isServer ? "服务端" : "客户端";
		try {
			var port = Integer.parseInt(portInput);
			if (port > 0 && port < 65536) {
				if (isServer) Config.serverPort = port;
				else Config.clientPort = port;
				if (HexSync.HEADLESS) System.out.printf("%s端口已设置为: %d%n", side, port);
				return true;
			} else {
				if (HexSync.HEADLESS) System.err.println(side + "端口号必须在0~65535之间.");
				return false;
			}
		} catch (NumberFormatException error) {
			if (HexSync.HEADLESS) System.out.println(side + "端口号必须为数字.");
			return false;
		}
	}
	// 设置最大上传速率
	public static void setRate(@NotNull String input) {
		var parts = input.split("\\s+", 2);
		if (parts.length != 2) {
			if (HexSync.HEADLESS) System.err.println("速率格式错误，应为: <数字> <单位>");
			return;
		}
		var rateUnit = Unit.fromString(parts[1]);
		if (!parts[0].matches("\\d+") || isInvalidLong(parts[0]) || rateUnit == null) {
			if (HexSync.HEADLESS) System.err.println("速率格式错误，应为: <数字> <单位>");
			return;
		}
		Config.serverUploadRate = new Rate(Long.parseLong(parts[0]), rateUnit);
	}
	// 检测数字输入是否不在Long范围内
	public static boolean isInvalidLong(@NotNull String input) {
		try {
			Long.parseLong(input.trim());
			return false;
		} catch (NumberFormatException error) {
			Log.warn("错误的数字格式或超出范围: " + input);
			return true;
		}
	}
	// 设置文件夹路径
	public static void setDirectory(@NotNull String directory, String log, Consumer<String> consumer) {
		if (!directory.isEmpty() && !directory.contains(File.separator)) {
			consumer.accept(directory);
			System.out.printf("%s文件夹路径已设置为: %s%n", log, directory);
		} else System.err.println("路径格式错误,请输入绝对路径或相对路径.");
	}
	// 设置自动启动
	public static void setAutoStart(@NotNull String input, Boolean isServer, Consumer<Boolean> consumer) {
		if (input.matches("[yYnN]")) {
			var value = input.matches("[yY]");
			consumer.accept(value);
			System.out.println(isServer ? "服务端" : "客户端" + "自动启动已设置为: " + value);
		} else System.err.println("无效输入,请输入Y|N.");
	}
}
