package ForgeStove;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

import static ForgeStove.Client.*;
import static ForgeStove.Config.*;
import static ForgeStove.HexSync.HEX_SYNC_NAME;
import static ForgeStove.Log.*;
import static ForgeStove.NormalUI.HEADLESS;
import static ForgeStove.Server.*;
import static java.lang.Math.multiplyExact;
import static java.lang.System.*;
public class Utils {
	// 初始化文件
	public static void initFiles(boolean isServer) {
		makeDirectory(isServer ? serverSyncDirectory : clientSyncDirectory);
		makeDirectory(HEX_SYNC_NAME);
		loadConfig();
		if (isServer) Server.serverMap = initMap(serverSyncDirectory);
		else {
			makeDirectory(clientOnlyDirectory);
			errorDownload = false;
		}
	}
	// 初始化文件名校验码键值对表
	public static Map<String, Long> initMap(String directory) {
		Map<String, Long> map = new HashMap<>();
		File[] fileList = new File(directory).listFiles(); // 获取文件夹下的所有文件
		if (fileList != null) for (File file : fileList)
			if (file.isFile()) map.put(file.getName(), calculateCRC(file));
		return map;
	}
	// 计算文件校验码
	public static long calculateCRC(File file) {
		CRC32 crc = new CRC32();
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) crc.update(buffer, 0, bytesRead);
		} catch (IOException error) {
			log(SEVERE, "计算CRC时出错: " + error.getMessage());
		}
		return crc.getValue();
	}
	// 创建文件夹
	public static void makeDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.isDirectory()) return;
		if (directory.mkdirs()) log(INFO, "文件夹已创建: " + directoryPath);
		else log(SEVERE, "无法创建文件夹: " + directoryPath);
	}
	// 地址格式化,转换为HTTP协议
	public static String formatHTTP(String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("http://") ? address : "http://" + address; // 添加HTTP协议头
	}
	// 字符串转端口
	public static boolean getPort(String portInput, boolean isServer) {
		String side = isServer ? "服务端" : "客户端";
		try {
			int port = Integer.parseInt(portInput);
			if (port > 0 && port < 65536) {
				// 设置端口并记录日志
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
	// 单位转换方法
	public static long convertToBytes(long value, String unit) {
		try {
			switch (unit) {
				case "B":
					return value;
				case "KB":
					return multiplyExact(value, 1024);
				case "MB":
					return multiplyExact(value, 1048576);
				case "GB":
					return multiplyExact(value, 1073741824);
				default:
					log(WARNING, "未知的最大上传速率单位: " + unit);
					return 0;
			}
		} catch (ArithmeticException error) {
			log(WARNING, "最大上传速率溢出，自动转化为无限制: " + error.getMessage());
			return 0; // 溢出
		}
	}
	// 设置最大上传速率
	public static void setRate(String input) {
		String[] parts = input.split("\\s+");
		if (input.matches("\\d+(\\s+B|\\s+KB|\\s+MB|\\s+GB)") && !invalidLong(parts[0])) {
			serverUploadRateLimit = Long.parseLong(parts[0]);
			serverUploadRateLimitUnit = parts[1];
		} else if (HEADLESS) err.println("速率格式错误");
	}
	// 检测数字输入是否不在Long范围内
	public static boolean invalidLong(String numberInput) {
		String trimmedInput = numberInput.trim();
		if (trimmedInput.isEmpty()) return true;
		try {
			Long.parseLong(trimmedInput);
			return false;
		} catch (NumberFormatException error) {
			log(WARNING, "错误的数字格式或超出范围: " + numberInput);
			return true;
		}
	}
}