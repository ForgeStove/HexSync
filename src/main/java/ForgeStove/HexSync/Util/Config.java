package ForgeStove.HexSync.Util;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import static ForgeStove.HexSync.Client.Client.*;
import static ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static ForgeStove.HexSync.Server.Server.*;
import static ForgeStove.HexSync.Util.Log.*;
import static java.io.File.separator;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.*;
import static java.lang.System.lineSeparator;
public class Config {
	public static final String CONFIG_PATH = HEX_SYNC_NAME + separator + "config.properties"; // 配置文件路径
	public static final String SERVER_PORT = "serverPort"; // 服务端端口配置项
	public static final String SERVER_SYNC_DIRECTORY = "serverSyncDirectory"; // 服务端同步文件夹路径配置项
	public static final String SERVER_UPLOAD_RATE_LIMIT = "serverUploadRateLimit"; // 上传速率限制配置项
	public static final String SERVER_AUTO_START = "serverAutoStart"; // 服务端自动启动配置项
	public static final String CLIENT_PORT = "clientPort"; // 客户端端口配置项
	public static final String SERVER_ADDRESS = "serverAddress"; // 服务器地址配置项
	public static final String CLIENT_SYNC_DIRECTORY = "clientSyncDirectory"; // 客户端同步文件夹路径配置项
	public static final String CLIENT_ONLY_DIRECTORY = "clientOnlyDirectory"; // 仅客户端文件夹路径配置项
	public static final String CLIENT_AUTO_START = "clientAutoStart"; // 客户端自动启动配置项
	public static String serverSyncDirectory = "mods"; // 服务端同步文件夹路径，默认值mods
	public static String clientSyncDirectory = "mods"; // 客户端同步文件夹路径，默认值mods
	public static String clientOnlyDirectory = "clientOnlyMods"; // 仅客户端文件夹路径，默认值clientOnlyMods
	public static String serverUploadRateLimitUnit = "MB"; // 上传速率限制单位，默认MB
	public static String serverAddress = "localhost"; // 服务器地址，默认值localhost
	// 加载配置
	public static void loadConfig() {
		File configFile = new File(CONFIG_PATH);
		if (!configFile.isFile()) {
			saveConfig();
			return;
		}
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile))) {
			Map<String, Consumer<String>> configMap = new HashMap<>();
			configMap.put(SERVER_PORT, input -> serverPort = parseInt(input));
			configMap.put(SERVER_UPLOAD_RATE_LIMIT, Settings::setRate);
			configMap.put(SERVER_SYNC_DIRECTORY, input -> serverSyncDirectory = input);
			configMap.put(SERVER_AUTO_START, input -> serverAutoStart = parseBoolean(input));
			configMap.put(CLIENT_PORT, input -> clientPort = parseInt(input));
			configMap.put(SERVER_ADDRESS, input -> serverAddress = input);
			configMap.put(CLIENT_SYNC_DIRECTORY, input -> clientSyncDirectory = input);
			configMap.put(CLIENT_ONLY_DIRECTORY, input -> clientOnlyDirectory = input);
			configMap.put(CLIENT_AUTO_START, input -> clientAutoStart = parseBoolean(input));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				line = line.replace(" ", ""); // 去除空格
				if (!line.matches("^[a-zA-Z].*")) continue; // 仅当首字符不是字母时跳过
				String[] parts = line.split("=");
				if (parts.length != 2) {
					log(WARNING, "配置格式错误: " + line);
					continue;
				}
				Consumer<String> action = configMap.get(parts[0]);
				if (action != null) action.accept(parts[1]);
				else log(WARNING, "配置项错误: " + line);
			}
		} catch (IOException error) {
			log(SEVERE, "配置读取失败: " + error.getMessage());
		}
	}
	// 保存配置
	public static void saveConfig() {
		String[][] configEntries = {
				{"# 服务端配置"},
				{SERVER_PORT, valueOf(serverPort)},
				{SERVER_UPLOAD_RATE_LIMIT, serverUploadRateLimit + " " + serverUploadRateLimitUnit},
				{SERVER_SYNC_DIRECTORY, serverSyncDirectory},
				{SERVER_AUTO_START, valueOf(serverAutoStart)},
				{"# 客户端配置"},
				{CLIENT_PORT, valueOf(clientPort)},
				{SERVER_ADDRESS, serverAddress},
				{CLIENT_SYNC_DIRECTORY, clientSyncDirectory},
				{CLIENT_ONLY_DIRECTORY, clientOnlyDirectory},
				{CLIENT_AUTO_START, valueOf(clientAutoStart)}
		};
		StringBuilder configContent = new StringBuilder();
		for (String[] entry : configEntries) {
			if (entry[0].startsWith("#")) configContent.append(entry[0]).append(lineSeparator());
			else configContent.append(format("%s=%s%n", entry[0], entry.length > 1 ? entry[1] : ""));
		}
		configContent.deleteCharAt(configContent.length() - 1); // 去除末尾的换行符
		File configFile = new File(CONFIG_PATH);
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configFile))) {
			bufferedWriter.write(configContent.toString()); // 写入配置文件
			log(INFO, "配置已保存: " + lineSeparator() + configContent);
		} catch (IOException error) {
			log(SEVERE, "配置保存失败: " + error.getMessage());
		}
	}
}
