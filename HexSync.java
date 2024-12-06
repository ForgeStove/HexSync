import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.*;
public class HexSync extends JFrame {
	private static final String HEX_SYNC_NAME = HexSync.class.getName(); // 程序名称
	private static final Logger LOGGER = Logger.getLogger(HEX_SYNC_NAME); // 日志记录器
	private static final Map<String, String> SERVER_MAP = new HashMap<>(); // 存储服务端文件名和对应的SHA512数据
	private static final String SEPARATOR = File.separator; // 文件分隔符
	private static final String LINE_SEPARATOR = System.lineSeparator(); // 换行符
	private static final String HEX_SYNC_DIRECTORY = HEX_SYNC_NAME; // 文件夹目录
	private static final String LOG_FILE = HEX_SYNC_DIRECTORY + SEPARATOR + HEX_SYNC_NAME + ".log"; // 日志文件路径
	private static final String CONFIG_FILE_PATH = HEX_SYNC_DIRECTORY + SEPARATOR + HEX_SYNC_NAME + "Config.properties"; // 配置文件路径
	private static final String SERVER_AUTO_START_CONFIG = "ServerAutoStart"; // 服务端自动启动配置项
	private static final String SERVER_HTTP_PORT_CONFIG = "ServerHTTPPort"; // 服务端端口配置项
	private static final String SERVER_SYNC_DIRECTORY_CONFIG = "ServerSyncDirectoryPath"; // 服务端同步文件夹路径配置项
	private static final String SERVER_UPLOAD_RATE_LIMIT_CONFIG = "ServerUploadRateLimit"; // 上传速率限制配置项
	private static final String CLIENT_HTTP_PORT_CONFIG = "ClientHTTPPort"; // 客户端端口配置项
	private static final String SERVER_ADDRESS_CONFIG = "ServerAddress"; // 服务器地址配置项
	private static final String CLIENT_SYNC_DIRECTORY_CONFIG = "ClientSyncDirectoryPath"; // 客户端同步文件夹路径配置项
	private static final String CLIENT_AUTO_START_CONFIG = "clientAutoStart"; // 客户端自动启动配置项
	private static String serverSyncDirectory = "mods"; // 服务端同步文件夹目录，默认值"mods"
	private static String clientSyncDirectory = "mods"; // 客户端同步文件夹目录，默认值"mods"
	private static String serverUploadRateLimitUnit = "MB/s"; // 上传速率限制单位，默认MB/s
	private static String serverAddress = "localhost"; // 服务器地址闭
	private static boolean headless; // 是否无头模式
	private static boolean isErrorDownload; // 客户端下载文件时是否发生错误，影响客户端是否自动关闭
	private static boolean serverAutoStart; // 服务端自动启动，默认不自动启动
	private static boolean clientAutoStart; // 客户端自动启动，默认不自动启动
	private static int serverHTTPPort = 65535;// HTTP 端口，默认值65535
	private static int clientHTTPPort = 65535; // 客户端 HTTP 端口，默认值65535
	private static long serverUploadRateLimit = 1; // 上传速率限制值，默认限速1MB/s
	private static HttpServer HTTPServer; // 用于存储服务器实例
	private static HttpURLConnection HTTPURLConnection; // 用于存储HTTP连接实例
	private static Thread serverHTTPThread; // 服务器线程
	private static Thread clientHTTPThread; // 客户端线程
	private static JButton toggleServerButton; // 服务端开关按钮
	private static JButton toggleClientButton; // 客户端开关按钮
	private static SystemTray systemTray; // 系统托盘
	private static TrayIcon trayIcon; // 托盘图标
	public static void main(String[] args) {
		initializeLogger();
		loadConfig();
		initializeUI(args);
	}
	// 初始化日志记录器
	private static void initializeLogger() {
		new Thread(() -> {
			createDirectory(HEX_SYNC_DIRECTORY);
			File logFile = new File(LOG_FILE);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, false))) {
				if (logFile.length() > 0) writer.write(""); // 清空内容
				FileHandler fileHandler = new FileHandler(LOG_FILE, true);
				fileHandler.setFormatter(new SingleLineFormatter()); // 设置日志格式化器
				LOGGER.addHandler(fileHandler); // 将FileHandler添加到日志记录器
			} catch (IOException | SecurityException error) {
				LOGGER.log(Level.SEVERE, "初始化日志时出错: " + error.getMessage(), error);
			}
		}).start();
	}
	// 初始化UI
	private static void initializeUI(String[] args) {
		HexSync HexSync = new HexSync();
		// 无头模式
		headless = GraphicsEnvironment.isHeadless() || Arrays.asList(args).contains("-headless");
		if (headless) HexSync.headlessUI();
		else {
			// 外观初始化
			String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
			try {
				UIManager.setLookAndFeel(lookAndFeel);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
					 UnsupportedLookAndFeelException error) {
				LOGGER.log(Level.SEVERE, "设置外观失败: " + error.getMessage());
			}
			// 创建窗口
			SwingUtilities.invokeLater(() -> {
				HexSync.createUI();
				HexSync.setVisible(!serverAutoStart);
			});
		}
	}
	// 初始化文件
	private static void initializeFiles(boolean isServer) {
		createDirectory(isServer ? serverSyncDirectory : clientSyncDirectory); // 创建同步文件夹
		createDirectory(HEX_SYNC_DIRECTORY); // 在当前目录下创建HexSync文件夹
		loadConfig(); // 加载配置文件
		if (!isServer) return;
		SERVER_MAP.clear(); // 清空同步文件列表
		File syncDirectory = new File(serverSyncDirectory); // 获取同步文件夹
		File[] files = syncDirectory.listFiles(); // 获取同步文件夹下的所有文件
		if (files == null) return; // 同步文件夹为空
		for (File file : files) {
			if (!file.isFile()) continue; // 跳过非文件项
			String SHA512Value = calculateSHA512(file);
			SERVER_MAP.put(file.getName(), SHA512Value);
		}
		LOGGER.log(Level.INFO, "初始化文件完成");
	}
	// 检测并创建文件夹
	private static void createDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.exists()) return;
		boolean isCreated = directory.mkdirs(); // 创建文件夹并保存结果
		LOGGER.log(isCreated ? Level.INFO : Level.SEVERE, (isCreated ? "文件夹已创建: " : "无法创建文件夹: ") + directoryPath);
	}
	// 加载配置文件
	private static void loadConfig() {
		new Thread(() -> {
			File configFile = new File(CONFIG_FILE_PATH);
			if (!configFile.exists()) return;
			try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
				String line;
				while ((line = reader.readLine()) != null) processConfigLine(line.trim()); // 处理每一行配置
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "配置读取失败: " + error.getMessage());
			}
		}).start();
	}
	// 处理单行配置
	private static void processConfigLine(String line) {
		if (line.isEmpty() || line.startsWith("#")) return;
		String[] parts = line.split("=");
		if (parts.length != 2) {
			LOGGER.log(Level.WARNING, "配置文件中行格式不正确,跳过: " + line);
			return;
		}
		String head = parts[0].trim();
		String tail = parts[1].trim();
		switch (head) {
			case SERVER_HTTP_PORT_CONFIG:
				serverHTTPPort = Integer.parseInt(tail);
				break;
			case SERVER_UPLOAD_RATE_LIMIT_CONFIG:
				parseRateLimit(tail, line);
				break;
			case SERVER_SYNC_DIRECTORY_CONFIG:
				serverSyncDirectory = tail;
				break;
			case SERVER_AUTO_START_CONFIG:
				serverAutoStart = Boolean.parseBoolean(tail);
				break;
			case CLIENT_HTTP_PORT_CONFIG:
				clientHTTPPort = Integer.parseInt(tail);
				break;
			case SERVER_ADDRESS_CONFIG:
				serverAddress = tail;
				break;
			case CLIENT_SYNC_DIRECTORY_CONFIG:
				clientSyncDirectory = tail;
				break;
			case CLIENT_AUTO_START_CONFIG:
				clientAutoStart = Boolean.parseBoolean(tail);
				break;
			default:
				LOGGER.log(Level.WARNING, "未知的配置项,跳过: " + head);
				break;
		}
	}
	// 解析上传速率限制
	private static void parseRateLimit(String tail, String originalLine) {
		String[] limitParts = tail.split(" ");
		if (limitParts.length != 2) LOGGER.log(Level.WARNING, "上传速率限制格式不正确,跳过: " + originalLine);
		else {
			serverUploadRateLimit = Long.parseLong(limitParts[0]);
			serverUploadRateLimitUnit = limitParts[1];
		}
	}
	// 地址格式化,转换为HTTP协议
	private static String HTTPFormat(String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("https://") ? address.replace("https://", "http://") : "http://" + address;
	}
	// 计算SHA512校验码
	private static String calculateSHA512(File file) {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] byteBuffer = new byte[8192]; // 缓冲区大小
			int bytesRead; // 读取字节数
			MessageDigest SHA512 = MessageDigest.getInstance("SHA-512"); // 获取SHA-512算法
			while ((bytesRead = fileInputStream.read(byteBuffer)) != -1) SHA512.update(byteBuffer, 0, bytesRead);
			byte[] digest = SHA512.digest(); // 获取SHA-512校验码
			StringBuilder stringBuilder = new StringBuilder();
			for (byte singleByte : digest) stringBuilder.append(String.format("%02x", singleByte)); // 转换为十六进制字符串
			return stringBuilder.toString(); // 返回SHA-512校验码
		} catch (Exception error) {
			LOGGER.log(Level.SEVERE, "计算校验码时出错: " + error.getMessage());
			return null;
		}
	}
	// 从服务器请求文件名和SHA512值列表
	private static Map<String, String> requestHTTPList() {
		String URL = HTTPFormat(serverAddress) + ":" + clientHTTPPort + "/list"; // 服务器地址
		LOGGER.log(Level.INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, String> requestMap = new HashMap<>(); // 复制请求列表
		try {
			URL requestURL = new URL(URL); // 创建URL
			int responseCode = getResponseCode(requestURL);
			if (responseCode != HttpURLConnection.HTTP_OK) {
				if (clientHTTPThread != null)
					LOGGER.log(Level.SEVERE, "请求文件列表失败,HTTP错误代码: " + responseCode); // 记录错误日志
				isErrorDownload = true;
				return requestMap;
			}
		} catch (IOException error) {
			if (clientHTTPThread != null) LOGGER.log(Level.SEVERE, "连接服务器时出错: " + error.getMessage());
			isErrorDownload = true;
			return requestMap;
		}
		try (BufferedReader in = new BufferedReader(new InputStreamReader(HTTPURLConnection.getInputStream()))) {
			String fileName; // 临时变量,用于存储文件名
			while ((fileName = in.readLine()) != null) { // 读取文件名
				String SHA512Value = in.readLine(); // 读取对应的SHA512值
				if (SHA512Value == null) continue;
				requestMap.put(fileName.trim(), SHA512Value.trim()); // 将文件名与SHA512值放入Map
			}
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "读取响应时出错: " + error.getMessage());
			isErrorDownload = true;
		}
		// 记录整体文件列表
		StringBuilder fileListBuilder = new StringBuilder("接收到文件列表:\n");
		logMap(requestMap, fileListBuilder);
		return requestMap;
	}
	private static void logMap(Map<String, String> map, StringBuilder fileListBuilder) {
		for (Map.Entry<String, String> entry : map.entrySet()) fileListBuilder.append(entry.getKey()).append("\n");
		LOGGER.log(Level.INFO, fileListBuilder.toString());
	}
	// 从服务器下载文件
	private static boolean downloadHTTPFile(String fileName, String filePath, Map<String, String> filesToDownloadMap) {
		File clientFile = new File(filePath); // 目标本地文件
		try {
			String serverSHA512 = filesToDownloadMap.get(fileName);
			URL requestURL = new URL(HTTPFormat(serverAddress) + ":" + clientHTTPPort + "/download=" + serverSHA512); // 创建URL
			int responseCode = getResponseCode(requestURL);
			if (responseCode != HttpURLConnection.HTTP_OK) {
				LOGGER.log(Level.SEVERE, "下载失败,HTTP错误代码: " + responseCode);
				return false;
			}
			// 下载成功,读取输入流并写入本地文件
			try (InputStream inputStream = HTTPURLConnection.getInputStream();
				 FileOutputStream outputStream = new FileOutputStream(filePath)) {
				byte[] buffer = new byte[8192]; // 8KB缓冲区
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
			}
			// 进行SHA512校验
			if (serverSHA512 == null) {
				LOGGER.log(Level.SEVERE, "无法获取服务器的SHA512值: " + fileName);
				return false;
			}
			String clientSHA512 = calculateSHA512(clientFile);
			if (serverSHA512.equals(clientSHA512)) return true; // 下载成功且SHA512校验通过
			LOGGER.log(Level.SEVERE, "校验失败,文件可能已损坏: " + fileName);
			if (!clientFile.delete()) LOGGER.log(Level.SEVERE, "无法删除损坏的文件: " + clientFile.getPath());
			return false;
		} catch (Exception error) {
			LOGGER.log(Level.SEVERE, "下载文件时出错: " + error.getMessage());
			return false;
		}
	}
	private static int getResponseCode(URL requestURL) throws IOException {
		HTTPURLConnection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
		HTTPURLConnection.setRequestMethod("GET"); // 设置请求方式为GET
		return HTTPURLConnection.getResponseCode(); // 返回响应码
	}
	// 保存配置的方法
	private static void saveConfig() {
		File configFile = new File(CONFIG_FILE_PATH);
		// 使用常量数组保存配置参数及其值
		String[][] configEntries = {
				{"# 服务端配置"},
				{SERVER_HTTP_PORT_CONFIG, String.valueOf(serverHTTPPort)},
				{SERVER_UPLOAD_RATE_LIMIT_CONFIG, serverUploadRateLimit + " " + serverUploadRateLimitUnit},
				{SERVER_SYNC_DIRECTORY_CONFIG, serverSyncDirectory},
				{SERVER_AUTO_START_CONFIG, String.valueOf(serverAutoStart)},
				{"# 客户端配置"},
				{CLIENT_HTTP_PORT_CONFIG, String.valueOf(clientHTTPPort)},
				{SERVER_ADDRESS_CONFIG, serverAddress},
				{CLIENT_SYNC_DIRECTORY_CONFIG, clientSyncDirectory},
				{CLIENT_AUTO_START_CONFIG, String.valueOf(clientAutoStart)},
		};
		// 构建配置内容
		StringBuilder configContent = new StringBuilder();
		for (String[] entry : configEntries) {
			if (entry[0].startsWith("#")) configContent.append(entry[0]).append(LINE_SEPARATOR);
			else
				configContent.append(entry[0]).append("=").append(entry.length > 1 ? entry[1] : "").append(LINE_SEPARATOR);
		}
		// 写入配置文件
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
			writer.write(configContent.toString());
			LOGGER.log(Level.INFO, "配置已保存: " + LINE_SEPARATOR + configContent);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "配置保存失败: " + error.getMessage(), error);
		}
	}
	// 发送响应
	private static void sendHTTPResponse(HttpExchange exchange, byte[] responseBytes, int HTTPCode) {
		try (OutputStream outputStream = exchange.getResponseBody()) {
			exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 设置Content-Type
			exchange.sendResponseHeaders(HTTPCode, responseBytes.length); // 设置响应头
			long startTime = System.currentTimeMillis(); // 记录开始时间
			long uploadRateInBytes = convertToBytes(serverUploadRateLimit, serverUploadRateLimitUnit); // 转换为字节
			int responseLength = responseBytes.length;
			int totalBytesSent = 0; // 记录已发送字节数
			if (serverUploadRateLimit == 0) {
				outputStream.write(responseBytes);
				return; // 直接返回，不再执行后续代码
			}
			while (totalBytesSent < responseLength) {
				int bytesToSend = Math.min(16384, responseLength - totalBytesSent); // 每次最多发送16KB
				outputStream.write(responseBytes, totalBytesSent, bytesToSend); // 写入数据
				totalBytesSent += bytesToSend; // 更新已发送总字节数
				long expectedBytesSent = (System.currentTimeMillis() - startTime * uploadRateInBytes) / 1000; // 计算期望已发送字节数
				if (totalBytesSent > expectedBytesSent) {
					long sleepTime = (totalBytesSent - expectedBytesSent) * 1000 / uploadRateInBytes; // 计算需要等待的时间
					Thread.sleep(sleepTime); // 暂停
				}
			}
		} catch (IOException | InterruptedException error) {
			LOGGER.log(Level.SEVERE, "发送响应时出错: " + error.getMessage());
		}
	}
	// 单位转换方法
	private static long convertToBytes(long value, String unit) {
		switch (unit) {
			case "B/s":
				return value;
			case "KB/s":
				return value * 1024;
			case "MB/s":
				return value * 1024 * 1024;
			case "GB/s":
				return value * 1024 * 1024 * 1024;
			default:
				LOGGER.log(Level.WARNING, "未知的上传速率单位: " + unit);
				return 0;
		}
	}
	private static String readServerFiles(String SHA512) {
		return SERVER_MAP.entrySet().stream()
				.filter(entry -> SHA512.equals(entry.getValue())) // 过滤出SHA512值匹配的条目
				.map(entry -> serverSyncDirectory + SEPARATOR + entry.getKey()) // 提取文件路径
				.findFirst() // 只返回第一个匹配的文件路径
				.orElse(null); // 如果没有匹配，返回null
	}
	// 检查文件是否已存在并进行SHA512校验
	private static boolean checkFile(File localFile, String fileName, Map<String, String> requestMap) {
		if (!localFile.exists()) return false; // 文件不存在,直接返回
		String serverSHA512 = requestMap.get(fileName); // 从服务端请求的文件列表中获取SHA512值
		if (serverSHA512 == null) return false;
		return serverSHA512.equals(calculateSHA512(localFile));
	}
	private static void openLog() {
		try {
			String os = System.getProperty("os.name").toLowerCase();// 检查操作系统类型
			String command;
			if (os.contains("win")) {// Windows平台
				command = "Get-Content -Path '" + LOG_FILE + "' -Encoding utf8 -Wait";
				Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "start", "powershell.exe", "-Command", command});
			} else if (os.contains("mac")) {// macOS平台
				command = "tail -f " + LOG_FILE;
				Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
			} else if (os.contains("nix") || os.contains("nux")) {// Linux平台
				command = "tail -f " + LOG_FILE;
				Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
			} else LOGGER.log(Level.SEVERE, "不支持的操作系统: " + os);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "打开命令行读取日志文件时出错: " + error.getMessage());
		}
	}
	private void headlessUI() {
		System.out.println("欢迎使用" + HEX_SYNC_NAME + "!");
		System.out.println("输入 'help'/'h' 以获取帮助.");
		if (serverAutoStart)
			toggleService(toggleServerButton, this::startHTTPServer, this::stopHTTPServer, "启动服务端", "停止服务端");
		if (clientAutoStart)
			toggleService(toggleClientButton, this::startHTTPClient, this::stopHTTPClient, "启动客户端", "停止客户端");
		Scanner scanner = new Scanner(System.in);
		String command;
		while (true) {
			System.out.print(HEX_SYNC_NAME + "> ");
			command = scanner.nextLine();
			switch (command) {
				case "toggleHTTPServer":
				case "s":
					if (serverHTTPThread == null) startHTTPServer();
					else stopHTTPServer();
					break;
				case "toggleHTTPClient":
				case "c":
					if (clientHTTPThread == null) startHTTPClient();
					else stopHTTPClient();
					break;
				case "settings":
				case "set":
					headlessSettings();
					break;
				case "help":
				case "h":
					System.out.println("'toggleHTTPServer'/'s': 切换服务端.");
					System.out.println("'toggleHTTPClient'/'c': 切换客户端.");
					System.out.println("'settings'/'set': 设置.");
					System.out.println("'exit'/'e': 退出.");
					break;
				case "exit":
				case "e":
					System.exit(0);
					break;
				default:
					System.out.println("无效命令,输入 'help'/'h' 以获取帮助.");
			}
		}
	}
	private void headlessSettings() {
		System.out.println("输入 'help'/'h' 以获取帮助.");
		System.out.println("输入 'exit'/'e' 以保存并退出.");
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print(HEX_SYNC_NAME + "Settings> ");
			String input;
			switch (scanner.nextLine()) {
				case "serverHTTPPort":
				case "sp":
					System.out.print("请输入服务端HTTP端口: ");
					input = scanner.nextLine();
					if (input.matches("\\d+")) {
						serverHTTPPort = Integer.parseInt(input);
						LOGGER.log(Level.INFO, "服务端HTTP端口已设置为: " + serverHTTPPort);
					} else {
						System.out.println("无效输入,请输入数字.");
					}
					break;
				case "serverUploadRateLimit":
				case "sl":
					System.out.print("请输入服务端上传速率('B/s'/'KB/s'/'MB/s'/GB/s): ");
					input = scanner.nextLine();
					if (input.matches("\\d+(\\s+B/s|\\s+KB/s|\\s+MB/s|\\s+GB/s)")) {
						String[] parts = input.split("\\s+");
						serverUploadRateLimit = Long.parseLong(parts[0]);
						serverUploadRateLimitUnit = parts[1];
						LOGGER.log(Level.INFO, "服务端上传速率已设置为: " + serverUploadRateLimit + " " + serverUploadRateLimitUnit);
					} else {
						System.out.println("无效输入,请输入数字及单位.");
					}
					break;
				case "serverSyncDirectory":
				case "sd":
					System.out.print("请输入服务端同步目录: ");
					input = scanner.nextLine();
					serverSyncDirectory = input;
					LOGGER.log(Level.INFO, "服务端同步目录已设置为: " + serverSyncDirectory);
					break;
				case "serverAutoStart":
				case "ss":
					System.out.print("是否自动启动服务端? (y/n): ");
					input = scanner.nextLine();
					if (input.matches("[yYnN]")) {
						serverAutoStart = input.matches("[yY]");
						LOGGER.log(Level.INFO, "服务端自动启动已设置为: " + serverAutoStart);
					} else {
						System.out.println("无效输入,请输入'y'/'Y'或'n'/'N'.");
					}
					break;
				case "clientHTTPPort":
				case "cp":
					System.out.print("请输入客户端HTTP端口: ");
					input = scanner.nextLine();
					if (input.matches("\\d+") && Integer.parseInt(input) > 0 && Integer.parseInt(input) < 65536) {
						clientHTTPPort = Integer.parseInt(input);
						LOGGER.log(Level.INFO, "客户端HTTP端口已设置为: " + clientHTTPPort);
					} else {
						System.out.println("无效输入,请输入数字.");
					}
					break;
				case "serverAddress":
				case "sa":
					System.out.print("请输入服务端地址: ");
					input = scanner.nextLine();
					if (input.matches("\\d+\\.\\d+")) {
						serverAddress = input;
						LOGGER.log(Level.INFO, "服务端地址已设置为: " + serverAddress);
					} else {
						System.out.println("无效输入,请输入IP地址.");
					}
					break;
				case "clientSyncDirectory":
				case "cd":
					System.out.print("请输入客户端同步目录: ");
					input = scanner.nextLine();
					clientSyncDirectory = input;
					LOGGER.log(Level.INFO, "客户端同步目录已设置为: " + clientSyncDirectory);
					break;
				case "clientAutoStart":
				case "cs":
					System.out.print("是否自动启动客户端? (y/n): ");
					input = scanner.nextLine();
					if (input.matches("[yYnN]")) {
						clientAutoStart = input.matches("[yY]");
						LOGGER.log(Level.INFO, "客户端自动启动已设置为: " + clientAutoStart);
					} else {
						System.out.println("无效输入,请输入'y'/'Y'或'n'/'N'.");
					}
					break;
				case "exit":
				case "e":
					saveConfig();
					return;
				case "help":
				case "h":
					System.out.println("'serverHTTPPort'/'sp': 设置服务端HTTP端口.");
					System.out.println("'serverUploadRateLimit'/'sl': 设置服务端上传速率.");
					System.out.println("'serverSyncDirectory'/'sd': 设置服务端同步目录.");
					System.out.println("'serverAutoStart'/'ss': 设置服务端自动启动.");
					System.out.println("'clientHTTPPort'/'cp': 设置客户端HTTP端口.");
					System.out.println("'serverAddress'/'sa': 设置服务端地址.");
					System.out.println("'clientSyncDirectory'/'cd': 设置客户端同步目录.");
					System.out.println("'clientAutoStart'/'cs': 设置客户端自动启动.");
					System.out.println("'exit'/'e': 保存并退出.");
					break;
				default:
					System.out.println("无效命令,输入 'help'/'h' 以获取帮助.");
			}
		}
	}
	private void stopHTTPServer() {
		LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server正在关闭...");
		HTTPServer.stop(0); // 停止服务
		serverHTTPThread.stop(); // 停止线程
		serverHTTPThread = null; // 清除线程引用
		LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server已关闭");
		toggleIcon();
	}
	private void startHTTPServer() {
		serverHTTPThread = new Thread(() -> {
			LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server正在启动...");
			initializeFiles(true);
			if (SERVER_MAP.isEmpty()) {
				LOGGER.log(Level.WARNING, serverSyncDirectory + "无文件,无法启动服务器");
				stopHTTPServer();
				return;
			}
			try {
				HTTPServer = HttpServer.create(new InetSocketAddress(serverHTTPPort), 0);
				HTTPServer.createContext("/", new HTTPRequestHandler());
				HTTPServer.setExecutor(null);
				HTTPServer.start();
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "服务器异常: " + error.getMessage());
			}
			LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server正在运行...端口号为: " + serverHTTPPort);
		});
		serverHTTPThread.start();
	}
	private void stopHTTPClient() {
		HTTPURLConnection.disconnect();
		clientHTTPThread = null; // 清除线程引用
		LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Client已关闭");
		if (clientAutoStart && !isErrorDownload) System.exit(0); // 自动退出
		if (headless) return;
		toggleClientButton.setText("启动客户端");
		toggleIcon();
	}
	private void startHTTPClient() {
		clientHTTPThread = new Thread(() -> {
			LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Client正在启动...");
			initializeFiles(false);
			Map<String, String> requestMap = requestHTTPList();
			if (!requestMap.isEmpty()) {
				LOGGER.log(Level.INFO, "获取到 " + requestMap.size() + " 个文件");
				Map<String, String> filesToDownloadMap = new HashMap<>(); // 用于存储需要下载的文件列表
				populateFilesToDownloadMap(requestMap, filesToDownloadMap);
				downloadHTTPFiles(filesToDownloadMap);
			} else isErrorDownload = true;
			stopHTTPClient();
		});
		clientHTTPThread.start();
	}
	// 构建需要下载的文件列表
	private void populateFilesToDownloadMap(Map<String, String> requestMap, Map<String, String> filesToDownloadMap) {
		Map<String, Boolean> clientMap = new HashMap<>();
		for (File file : Objects.requireNonNull(new File(clientSyncDirectory).listFiles()))
			clientMap.put(calculateSHA512(file), true);
		for (Map.Entry<String, String> entry : requestMap.entrySet()) {
			String fileName = entry.getKey();
			String SHA512Value = entry.getValue();
			if (fileName.isEmpty() || SHA512Value.isEmpty()) return;
			boolean contentExists = clientMap.containsKey(SHA512Value);
			if (!contentExists && !checkFile(new File(clientSyncDirectory + SEPARATOR + fileName), fileName, requestMap))
				filesToDownloadMap.put(fileName, SHA512Value);
		}
	}
	// 下载所有文件
	private void downloadHTTPFiles(Map<String, String> filesToDownloadMap) {
		if (filesToDownloadMap.isEmpty()) {
			LOGGER.log(Level.INFO, "没有需要下载的文件");
			return;
		}
		StringBuilder fileListBuilder = new StringBuilder("需要下载:\n");
		logMap(filesToDownloadMap, fileListBuilder);
		int downloadedCount = 0;
		int filesToDownloadMapSize = filesToDownloadMap.size();
		for (Map.Entry<String, String> entry : filesToDownloadMap.entrySet()) {
			String fileName = entry.getKey(); // 文件名
			String filePath = clientSyncDirectory + SEPARATOR + fileName; // 设置下载路径
			if (downloadHTTPFile(fileName, filePath, filesToDownloadMap)) {
				downloadedCount++; // 成功下载时增加计数
				LOGGER.log(Level.INFO, "已下载: [" + downloadedCount + "/" + filesToDownloadMapSize + "] " + filePath);
			} else {
				LOGGER.log(Level.SEVERE, "下载失败: " + filePath);
				isErrorDownload = true; // 记录下载失败
			}
		}
		LOGGER.log(Level.INFO, "下载完成: [" + downloadedCount + "/" + filesToDownloadMapSize + "]");
	}
	// 创建用户界面
	private void createUI() {
		Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
		toggleIcon();
		Font systemFont = UIManager.getFont("Label.font"); // 获取系统字体
		// 设置全局字体
		UIManager.put("Button.font", systemFont);
		UIManager.put("Label.font", systemFont);
		UIManager.put("TextArea.font", systemFont);
		UIManager.put("TextField.font", systemFont);
		// 设置窗口基本属性
		setTitle(HEX_SYNC_NAME + "控制面板");
		setSize(SCREEN_SIZE.width / 10, SCREEN_SIZE.height / 4);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null); // 居中显示
		// 创建面板
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		buttonPanel(panel);// 创建按钮面板
		add(panel);  // 添加主面板到窗口
		createSystemTray(); // 创建系统托盘
		openLog(); // 打开日志文件
		if (serverAutoStart)
			toggleService(toggleServerButton, this::startHTTPServer, this::stopHTTPServer, "启动服务端", "停止服务端"); // 启动服务端
		if (clientAutoStart)
			toggleService(toggleClientButton, this::startHTTPClient, this::stopHTTPClient, "启动客户端", "停止客户端"); // 启动客户端
	}
	private void buttonPanel(JPanel panel) {
		Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
		JPanel buttonPanel = new JPanel();
		JButton openDirectoryButton = new JButton("目录");
		JButton openLogButton = new JButton("日志");
		JButton settingsButton = new JButton("设置");
		toggleServerButton = new JButton("启动服务端");
		toggleClientButton = new JButton("启动客户端");
		JButton shutdownButton = new JButton("退出");
		// 设置按钮的首选大小
		Dimension buttonSize = new Dimension(SCREEN_SIZE.width / 15, SCREEN_SIZE.height / 32);
		openDirectoryButton.setPreferredSize(buttonSize);
		openLogButton.setPreferredSize(buttonSize);
		settingsButton.setPreferredSize(buttonSize);
		toggleServerButton.setPreferredSize(buttonSize);
		toggleClientButton.setPreferredSize(buttonSize);
		shutdownButton.setPreferredSize(buttonSize);
		// 添加按钮监听事件
		actionListener(openDirectoryButton, openLogButton, settingsButton, shutdownButton);
		// 添加按钮到按钮面板
		buttonPanel.add(openDirectoryButton);
		buttonPanel.add(openLogButton);
		buttonPanel.add(settingsButton);
		buttonPanel.add(toggleServerButton);
		buttonPanel.add(toggleClientButton);
		buttonPanel.add(shutdownButton);
		panel.add(buttonPanel, BorderLayout.CENTER); // 添加按钮面板到主面板
	}
	// 按钮监听事件
	private void actionListener(JButton openDirectoryButton, JButton openLogButton, JButton settingsButton, JButton shutdownButton) {
		openDirectoryButton.addActionListener(event -> {
			try {
				Desktop.getDesktop().open(new File(".")); // 打开目录
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "打开目录时出错: " + error.getMessage());
			}
		});
		openLogButton.addActionListener(event -> openLog());
		toggleServerButton.addActionListener(event -> toggleService(toggleServerButton, this::startHTTPServer, this::stopHTTPServer, "启动服务端", "停止服务端"));
		toggleClientButton.addActionListener(event -> toggleService(toggleClientButton, this::startHTTPClient, this::stopHTTPClient, "启动客户端", "停止客户端"));
		settingsButton.addActionListener(event -> openSettingsDialog()); // 打开设置对话框
		shutdownButton.addActionListener(event -> System.exit(0)); // 关闭程序
		// 添加窗口监听器
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				if (SystemTray.isSupported()) setVisible(false); // 隐藏窗口而不是退出程序
				else System.exit(0);
			}
		});
	}
	private void toggleService(JButton button, Runnable startService, Runnable stopService, String startText, String stopText) {
		button.setEnabled(false);
		if (button == toggleServerButton && serverHTTPThread == null || button == toggleClientButton && clientHTTPThread == null)
			startService.run();
		else stopService.run();
		button.setText(button == toggleServerButton && serverHTTPThread == null || button == toggleClientButton && clientHTTPThread == null ? startText : stopText);
		toggleIcon();
		button.setEnabled(true);
	}
	private void createSystemTray() {
		if (SystemTray.isSupported() && systemTray == null) {
			systemTray = SystemTray.getSystemTray();
			trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource(serverHTTPThread != null || clientHTTPThread != null ? "IconO.png" : "IconI.png")), HEX_SYNC_NAME);
			trayIcon.setImageAutoSize(true); // 自动调整图标大小
			trayIcon.setToolTip(HEX_SYNC_NAME + "控制面板");
			PopupMenu popup = getPopupMenu(); // 创建右键菜单
			trayIcon.setPopupMenu(popup);
			trayIcon.addActionListener(event -> setVisible(true));
			try {
				systemTray.add(trayIcon); // 添加托盘图标
			} catch (AWTException error) {
				LOGGER.log(Level.SEVERE, "添加托盘图标失败: " + error.getMessage());
			}
		}
	}
	// 切换图标
	private void toggleIcon() {
		Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(serverHTTPThread != null || clientHTTPThread != null ? "IconO.png" : "IconI.png"));
		setIconImage(icon);
		if (trayIcon != null) trayIcon.setImage(icon);
	}
	private PopupMenu getPopupMenu() {
		PopupMenu popup = new PopupMenu();
		MenuItem openItem = new MenuItem("Open");
		openItem.addActionListener(event -> setVisible(true));
		MenuItem hideItem = new MenuItem("Hide");
		hideItem.addActionListener(event -> setVisible(false));
		MenuItem openDirectoryItem = new MenuItem("Open Directory");
		openDirectoryItem.addActionListener(event -> {
			try {
				Desktop.getDesktop().open(new File(HEX_SYNC_DIRECTORY)); // 打开目录
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "打开目录时出错: " + error.getMessage());
			}
		});
		MenuItem settingsItem = new MenuItem("Settings");
		settingsItem.addActionListener(event -> openSettingsDialog()); // 打开设置对话框
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(event -> System.exit(0)); // 关闭程序
		// 将菜单项添加到弹出菜单
		popup.add(openItem);
		popup.addSeparator();
		popup.add(hideItem);
		popup.addSeparator();
		popup.add(openDirectoryItem);
		popup.addSeparator();
		popup.add(settingsItem);
		popup.addSeparator();
		popup.add(exitItem);
		return popup;
	}
	// 打开设置对话框
	private void openSettingsDialog() {
		loadConfig();
		Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
		int inputHeight = (int) (SCREEN_SIZE.height * 0.1); // 输入框高度为屏幕高度的10%
		int inputWidth = (int) (SCREEN_SIZE.width * 0.3); // 输入框宽度为屏幕宽度的20%
		int borderPadding = (int) (inputHeight * 0.1); // 边距为输入框高度的5%
		JDialog settingsDialog = new JDialog(this, "设置", true);
		settingsDialog.setLayout(new BorderLayout(borderPadding, borderPadding));
		// 创建选项卡面板
		JTabbedPane tabbedPane = new JTabbedPane();
		// 服务端设置选项卡
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		serverPanel.setBorder(BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding));
		JTextField serverPortField = new JTextField(String.valueOf(serverHTTPPort));
		JTextField serverUploadRateLimitField = new JTextField(String.valueOf(serverUploadRateLimit));
		JTextField serverSyncDirectoryPathField = new JTextField(serverSyncDirectory);
		// 创建选择单位的下拉框
		String[] serverUploadRateUnits = new String[]{"B/s", "KB/s", "MB/s", "GB/s"};
		JComboBox<String> serverUploadRateLimitUnitBox = new JComboBox<>(serverUploadRateUnits);
		serverUploadRateLimitUnitBox.setSelectedItem(serverUploadRateLimitUnit);
		// 添加标签和文本框到设置面板
		serverPanel.add(new JLabel("端口号: "));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel("上传速率限制(0为无限制): "));
		// 添加上传速率限制输入框与单位选择框
		JPanel serverUploadRateLimitPanel = new JPanel(new GridLayout(1, 2, 5, 0));
		serverUploadRateLimitPanel.add(serverUploadRateLimitField);
		serverUploadRateLimitPanel.add(serverUploadRateLimitUnitBox); // 将单位选择框添加到输入框旁边
		serverPanel.add(serverUploadRateLimitPanel);
		serverPanel.add(new JLabel("服务端同步文件夹路径: "));
		serverPanel.add(serverSyncDirectoryPathField);
		serverPortField.setSize(new Dimension(inputWidth, inputHeight));
		serverSyncDirectoryPathField.setSize(new Dimension(inputWidth, inputHeight));
		serverUploadRateLimitField.setSize(new Dimension(inputWidth * 3 / 4, inputHeight));
		serverUploadRateLimitUnitBox.setSize(new Dimension(inputWidth / 4, inputHeight));
		// 客户端设置选项卡
		JPanel clientPanel = new JPanel(new GridLayout(5, 2));
		clientPanel.setBorder(BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding));
		// 创建文本框
		JTextField clientPortField = new JTextField(String.valueOf(clientHTTPPort));
		JTextField clientAddressField = new JTextField(serverAddress);
		JTextField clientSyncDirectoryPathField = new JTextField(clientSyncDirectory);
		// 添加标签和文本框
		clientPanel.add(new JLabel("端口号: "));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel("服务器地址: "));
		clientPanel.add(clientAddressField);
		clientPanel.add(new JLabel("客户端同步文件夹路径: "));
		clientPanel.add(clientSyncDirectoryPathField);
		// 添加选项卡到选项卡面板
		tabbedPane.addTab("服务端设置", serverPanel);
		tabbedPane.addTab("客户端设置", clientPanel);
		settingsDialog.add(tabbedPane, BorderLayout.CENTER);
		JButton aboutButton = new JButton("关于");
		aboutButton.addActionListener(event -> {
			try {
				Desktop.getDesktop().browse(URI.create(("https://github.com/ForgeStove/HexSync")));
			} catch (Exception error) {
				LOGGER.log(Level.SEVERE, "无法打开浏览器: " + error.getMessage());
			}
		});
		JButton configSaveButton = new JButton("保存");
		JCheckBox serverAutoStartBox = new JCheckBox("自动启动服务端");
		JCheckBox clientAutoStartBox = new JCheckBox("自动启动客户端");
		serverAutoStartBox.setSelected(serverAutoStart); // 根据当前配置设置复选框状态
		clientAutoStartBox.setSelected(clientAutoStart);
		serverPanel.add(serverAutoStartBox); // 将复选框添加到服务端设置面板
		clientPanel.add(clientAutoStartBox); // 将复选框添加到客户端设置面板
		configSaveButton.addActionListener(event -> {
			// 定义输入框数组及其对应的提示信息和选项卡索引
			Object[][] inputs = {
					{serverPortField, "服务端端口", 0}, // 服务器端设置的索引
					{serverUploadRateLimitField, "上传速率限制", 0},
					{serverSyncDirectoryPathField, "服务端同步文件夹路径", 0},
					{clientPortField, "客户端端口", 1}, // 客户端设置的索引
					{clientAddressField, "服务器地址", 1},
					{clientSyncDirectoryPathField, "客户端同步文件夹路径", 1}
			};
			// 检查输入框是否为空
			for (Object[] input : inputs) {
				JTextField textField = (JTextField) input[0];
				String fieldName = (String) input[1];
				int tabIndex = (int) input[2]; // 获取对应的索引
				if (isEmptyTextField(textField, fieldName)) {
					tabbedPane.setSelectedIndex(tabIndex); // 跳转到对应的选项卡
					textField.selectAll(); // 选中输入框
					textField.requestFocus(); // 聚焦输入框
					return; // 输入框为空，返回
				}
			}
			// 检测端口号是否是数字且在合法范围内
			if (isInvalidPort(serverPortField) || isInvalidPort(clientPortField)) return;
			// 检测上传速率上限
			String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
			if (isNotNumber(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
				LOGGER.log(Level.WARNING, "上传速率上限不正确: " + uploadRateLimitText);
				tabbedPane.setSelectedIndex(0);
				serverUploadRateLimitField.selectAll(); // 选中输入框
				serverUploadRateLimitField.requestFocus(); // 聚焦输入框
				return; // 上传速率上限不合法
			}
			serverAutoStart = serverAutoStartBox.isSelected();
			clientAutoStart = clientAutoStartBox.isSelected();
			serverHTTPPort = Integer.parseInt(serverPortField.getText().trim());
			serverUploadRateLimit = Integer.parseInt(uploadRateLimitText);
			serverUploadRateLimitUnit = (String) serverUploadRateLimitUnitBox.getSelectedItem();
			serverSyncDirectory = serverSyncDirectoryPathField.getText().trim();
			clientHTTPPort = Integer.parseInt(clientPortField.getText().trim());
			serverAddress = clientAddressField.getText().trim();
			clientSyncDirectory = clientSyncDirectoryPathField.getText().trim();
			saveConfig(); // 保存配置
			settingsDialog.dispose(); // 关闭对话框
		});
		// 关闭按钮
		JButton cancelButton = new JButton("取消");
		cancelButton.addActionListener(event -> settingsDialog.dispose()); // 关闭对话框而不保存
		// 按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(configSaveButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(aboutButton);
		// 添加按钮面板到对话框的南部
		settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
		// 设置对话框的基本属性
		settingsDialog.setSize(SCREEN_SIZE.width / 4, SCREEN_SIZE.height / 5);
		settingsDialog.setLocationRelativeTo(this); // 居中
		settingsDialog.setVisible(true); // 显示对话框
	}
	// 检测端口号有效性的方法
	private boolean isInvalidPort(JTextField portField) {
		String portText = portField.getText().trim();
		boolean invalid = isNotNumber(portText) || Integer.parseInt(portText) < 1 || Integer.parseInt(portText) > 65535;
		if (!invalid) return false; // 端口号合法
		LOGGER.log(Level.WARNING, "端口号不在有效范围内: " + portText);
		portField.selectAll(); // 选中输入框
		portField.requestFocus(); // 聚焦输入框
		return true; // 端口号不合法
	}
	private boolean isNotNumber(String text) {
		if (text == null || text.trim().isEmpty()) return true; // 空字符串返回false
		try {
			Long.parseLong(text.trim()); // 尝试将文本转换为长整型
			return false; // 转换成功，返回true
		} catch (NumberFormatException error) {
			LOGGER.log(Level.WARNING, "不正确的数字格式: " + text);
			return true; // 转换失败，返回false
		}
	}
	private boolean isEmptyTextField(JTextField textField, String fieldName) {
		String text = textField.getText().trim();
		if (!text.isEmpty()) return false; // 返回false表示输入框不为空
		LOGGER.log(Level.WARNING, fieldName + "不能为空");
		return true; // 返回true表示输入框为空
	}
	// 日志格式化
	private static class SingleLineFormatter extends SimpleFormatter {
		@Override
		public String format(LogRecord record) {
			String logLevel = record.getLevel().getName();
			switch (logLevel) {
				case "INFO":
					logLevel = "信息";
					break;
				case "WARNING":
					logLevel = "警告";
					break;
				case "SEVERE":
					logLevel = "严重";
					break;
				default:
					logLevel = "未知";
			}
			return String.format("[%s][%tF][%tT]%s%n", logLevel, record.getMillis(), record.getMillis(), record.getMessage());
		}
	}
	// HTTP请求处理器
	private static class HTTPRequestHandler implements HttpHandler {
		// 处理请求
		private static void processHTTPRequest(String requestURI, HttpExchange exchange) {
			// 解析requestURI,查看是文件请求还是文件列表请求
			if ("/list".equals(requestURI)) sendHTTPList(exchange);
			else if (requestURI.startsWith("/download=")) {
				String clientSHA512 = requestURI.substring("/download=".length()); // 去掉"/download="得到SHA512值
				String filePath = readServerFiles(clientSHA512); // 读取文件路径
				File file = new File(filePath); // 构造文件对象
				if (!SERVER_MAP.containsValue(clientSHA512) || !file.exists() || !file.isFile()) {
					sendHTTPResponse(exchange, "未找到对应的文件".getBytes(), HttpURLConnection.HTTP_NOT_FOUND);
					return;
				}
				try (InputStream fileInputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
					exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, file.length());  // 设置响应头
					try (OutputStream outputStream = exchange.getResponseBody()) {
						byte[] buffer = new byte[8192];
						int bytesRead;
						while ((bytesRead = fileInputStream.read(buffer)) != -1)
							outputStream.write(buffer, 0, bytesRead);
					} catch (IOException error) {
						LOGGER.log(Level.SEVERE, "写入输出流时出错: " + error.getMessage());
					}
				} catch (IOException error) {
					LOGGER.log(Level.SEVERE, "发送文件时出错: " + error.getMessage());
				}
				LOGGER.log(Level.INFO, "已发送文件: " + filePath);
			}
		}
		// 发送文件名和校验码列表
		private static void sendHTTPList(HttpExchange exchange) {
			StringBuilder responseBuilder = new StringBuilder(); // 用于构建响应内容
			for (Map.Entry<String, String> entry : SERVER_MAP.entrySet()) { // 遍历同步文件列表
				String fileName = entry.getKey(); // 获取文件名
				String SHA512Value = entry.getValue(); // 获取校验码
				if (SHA512Value == null) LOGGER.log(Level.WARNING, "未找到文件: " + fileName);
				else responseBuilder.append(fileName).append(LINE_SEPARATOR).append(SHA512Value).append(LINE_SEPARATOR);
			}
			sendHTTPResponse(exchange, responseBuilder.toString().getBytes(), HttpURLConnection.HTTP_OK);
			LOGGER.log(Level.INFO, "已发送列表");
		}
		@Override
		public void handle(HttpExchange exchange) {
			try {
				String requestMethod = exchange.getRequestMethod(); // 获取请求方法
				if ("GET".equalsIgnoreCase(requestMethod)) {
					String requestURI = exchange.getRequestURI().getPath(); // 获取请求URI
					LOGGER.log(Level.INFO, "客户端请求: " + requestURI); // 记录客户端请求信息
					processHTTPRequest(requestURI, exchange); // 处理请求
				} else exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "处理请求时出错: " + error.getMessage(), error);
			}
		}
	}
}