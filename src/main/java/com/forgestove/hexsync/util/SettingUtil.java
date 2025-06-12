package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Consumer;
public class SettingUtil {
	// 字符串转端口
	public static void setPort(Port port, boolean isServer) {
		var side = isServer ? "服务端" : "客户端";
		if (isServer) Data.serverPort.set(port);
		else Data.clientPort.set(port);
		if (HexSync.HEADLESS) System.out.printf("%s端口已设置为: %d%n", side, port.getValue());
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
}
