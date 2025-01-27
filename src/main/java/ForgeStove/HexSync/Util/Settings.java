package ForgeStove.HexSync.Util;
import java.util.function.Consumer;

import static ForgeStove.HexSync.Client.Client.clientPort;
import static ForgeStove.HexSync.Server.Server.*;
import static ForgeStove.HexSync.Util.Config.*;
import static ForgeStove.HexSync.Util.Config.RateUnit.*;
import static ForgeStove.HexSync.Util.Log.*;
import static java.io.File.separator;
import static java.lang.System.*;
public class Settings {
	// 字符串转端口
	public static boolean canSetPort(String portInput, boolean isServer) {
		String side = isServer ? "服务端" : "客户端";
		try {
			int port = Integer.parseInt(portInput);
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
	public static void setRate(String input) {
		String[] parts = input.split("\\s+", 2);
		if (input.matches("\\d+(\\s+" + BPS + "|\\s+" + KBPS + "|\\s+" + MBPS + "|\\s+" + GBPS + ")") && !isInvalidLong(
				parts[0])) {
			serverUploadRateLimit = Long.parseLong(parts[0]);
			serverUploadRateLimitUnit = RateUnit.valueOf(parts[1]);
			if (HEADLESS) out.println("上传速率已设置为: " + serverUploadRateLimit + " " + serverUploadRateLimitUnit);
		} else if (HEADLESS) err.println("速率格式错误");
	}
	// 检测数字输入是否不在Long范围内
	public static boolean isInvalidLong(String input) {
		try {
			Long.parseLong(input.trim());
			return false;
		} catch (NumberFormatException error) {
			log(WARNING, "错误的数字格式或超出范围: " + input);
			return true;
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
		} else err.println("无效输入,请输入Y|N.");
	}
	// 地址格式化,转换为HTTP协议
	public static String formatHTTP(String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("http://") ? address : "http://" + address; // 添加HTTP协议头
	}
}
