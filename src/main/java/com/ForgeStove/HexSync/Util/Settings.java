package com.ForgeStove.HexSync.Util;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static com.ForgeStove.HexSync.Client.Client.clientPort;
import static com.ForgeStove.HexSync.Server.Server.*;
import static com.ForgeStove.HexSync.Util.Config.*;
import static com.ForgeStove.HexSync.Util.Log.log;
import static com.ForgeStove.HexSync.Util.Log.*;
import static com.ForgeStove.HexSync.Util.RateUnit.*;
import static java.io.File.separator;
import static java.lang.Math.*;
import static java.lang.System.*;
public class Settings {
	// 字符串转端口
	public static boolean canSetPort(String portInput, boolean isServer) {
		var side = isServer ? "服务端" : "客户端";
		try {
			var port = Integer.parseInt(portInput);
			if (port > 0 && port < 65536) {
				if (isServer) serverPort = port;
				else clientPort = port;
				if (HEADLESS) out.println(side + "端口已设置为: " + port);
				return true;
			} else {
				if (HEADLESS) err.println(side + "端口号必须在0~65535之间.");
				return false;
			}
		} catch (NumberFormatException error) {
			if (HEADLESS) out.println(side + "端口号必须为数字.");
			return false;
		}
	}
	// 设置最大上传速率
	public static void setRate(@NotNull String input) {
		var parts = input.split("\\s+", 2);
		if (input.matches("\\d+(\\s+(" + BPS.unit + "|" + KBPS.unit + "|" + MBPS.unit + "|" + GBPS.unit + "))")
				&& !isInvalidLong(parts[0])) {
			serverUploadRateLimit = Long.parseLong(parts[0]);
			serverUploadRateLimitUnit = fromUnit(parts[1]);
			if (serverThread != null) maxUploadRateInBytes = multiplyExact(
					serverUploadRateLimit,
					(long) pow(1024, serverUploadRateLimitUnit.exponent)
			);
		} else if (HEADLESS) err.println("速率格式错误");
	}
	// 检测数字输入是否不在Long范围内
	public static boolean isInvalidLong(@NotNull String input) {
		try {
			Long.parseLong(input.trim());
			return false;
		} catch (NumberFormatException error) {
			log(WARNING, "错误的数字格式或超出范围: " + input);
			return true;
		}
	}
	// 设置文件夹路径
	public static void setDirectory(@NotNull String directory, String log, Consumer<String> consumer) {
		if (!directory.isEmpty() && !directory.contains(separator)) {
			consumer.accept(directory);
			out.println(log + "文件夹路径已设置为: " + directory);
		} else err.println("路径格式错误,请输入绝对路径或相对路径.");
	}
	// 设置自动启动
	public static void setAutoStart(@NotNull String input, Boolean isServer, Consumer<Boolean> consumer) {
		if (input.matches("[yYnN]")) {
			var value = input.matches("[yY]");
			consumer.accept(value);
			out.println(isServer ? "服务端" : "客户端" + "自动启动已设置为: " + value);
		} else err.println("无效输入,请输入Y|N.");
	}
	// 地址格式化,转换为HTTP协议
	public static @NotNull String formatHTTP(@NotNull String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("http://") ? address : "http://" + address; // 添加HTTP协议头
	}
}
