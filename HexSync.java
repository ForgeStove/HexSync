import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
public class HexSync implements HttpHandler {
	private static final String HEX_SYNC_NAME = HexSync.class.getName(); // 程序名称
	private static final Logger LOGGER = Logger.getLogger(HEX_SYNC_NAME); // 日志记录器
	private static final AtomicLong availableTokens = new AtomicLong(0); // 当前可用令牌数量
	private static final String FILE_SEPARATOR = File.separator; // 文件分隔符
	private static final String LINE_SEPARATOR = System.lineSeparator(); // 换行符
	private static final String HEX_SYNC_DIRECTORY = HEX_SYNC_NAME; // 文件夹目录
	private static final String HEX_SYNC_NAME_PATH = HEX_SYNC_DIRECTORY + FILE_SEPARATOR + HEX_SYNC_NAME; // 程序名称路径
	private static final String LOG_FILE = HEX_SYNC_NAME_PATH + ".log"; // 日志文件路径
	private static final String CONFIG_FILE_PATH = HEX_SYNC_NAME_PATH + "Config.properties"; // 配置文件路径
	private static final String SERVER_HTTP_PORT_CONFIG = "serverHTTPPort"; // 服务端端口配置项
	private static final String SERVER_SYNC_DIRECTORY_CONFIG = "serverSyncDirectoryPath"; // 服务端同步文件夹路径配置项
	private static final String SERVER_UPLOAD_RATE_LIMIT_CONFIG = "serverUploadRateLimit"; // 上传速率限制配置项
	private static final String SERVER_AUTO_START_CONFIG = "serverAutoStart"; // 服务端自动启动配置项
	private static final String CLIENT_HTTP_PORT_CONFIG = "clientHTTPPort"; // 客户端端口配置项
	private static final String SERVER_ADDRESS_CONFIG = "serverAddress"; // 服务器地址配置项
	private static final String CLIENT_SYNC_DIRECTORY_CONFIG = "clientSyncDirectoryPath"; // 客户端同步文件夹路径配置项
	private static final String CLIENT_ONLY_DIRECTORY_CONFIG = "clientOnlyDirectoryPath"; // 仅客户端文件夹路径配置项
	private static final String CLIENT_AUTO_START_CONFIG = "clientAutoStart"; // 客户端自动启动配置项
	private static final String MINE_CRAFT_UPDATE_MODE_CONFIG = "MineCraftUpdateMode"; // MC更新模式配置项
	private static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync"; // 项目GitHub地址
	private static Map<String, String> serverMap = new HashMap<>(); // 存储服务端文件名和对应的SHA数据
	private static String serverSyncDirectory = "mods"; // 服务端同步文件夹目录，默认值"mods"
	private static String clientSyncDirectory = "mods"; // 客户端同步文件夹目录，默认值"mods"
	private static String clientOnlyDirectory = "clientOnlyMods"; // 仅客户端文件夹目录，默认值"clientOnlyMods"
	private static String serverUploadRateLimitUnit = "MB/s"; // 上传速率限制单位，默认MB/s
	private static String serverAddress = "localhost"; // 服务器地址，默认值localhost
	private static HttpServer HTTPServer; // 用于存储服务器实例
	private static HttpURLConnection HTTPURLConnection; // 用于存储HTTP连接实例
	private static Thread serverHTTPThread; // 服务器线程
	private static Thread clientHTTPThread; // 客户端线程
	private static boolean isErrorDownload; // 客户端下载文件时是否发生错误，影响客户端是否自动关闭
	private static boolean serverAutoStart; // 服务端自动启动，默认不自动启动
	private static boolean clientAutoStart; // 客户端自动启动，默认不自动启动
	private static boolean updateMineCraftMode; // 客户端是否为MC更新模式，默认不启用
	private static int serverHTTPPort = 65535;// HTTP 端口，默认值65535
	private static int clientHTTPPort = 65535; // 客户端 HTTP 端口，默认值65535
	private static long serverUploadRateLimit = 1; // 上传速率限制值，默认限速1MB/s
	public static void main(String[] args) {
		initializeLogger();
		loadConfig();
		initializeUI(args);
	}
	// 初始化日志记录器
	private static void initializeLogger() {
		new Thread(() -> {
			try {
				createDirectory(HEX_SYNC_DIRECTORY);
				FileHandler fileHandler = new FileHandler(LOG_FILE, false);
				fileHandler.setFormatter(new SimpleFormatter()); // 设置日志格式化器
				LOGGER.addHandler(fileHandler); // 将FileHandler添加到日志记录器
			} catch (Exception error) {
				LOGGER.log(Level.SEVERE, "初始化日志时出错: " + error.getMessage(), error);
			}
		}).start();
	}
	// 初始化UI
	private static void initializeUI(String[] args) {
		if (GraphicsEnvironment.isHeadless() || Arrays.asList(args).contains("-headless")) headlessUI(); // 无头模式
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception error) {
			LOGGER.log(Level.SEVERE, "设置外观失败: " + error.getMessage());
		}
		SwingUtilities.invokeLater(HexSync::createUI);// 创建窗口
	}
	// 初始化文件
	private static void initializeFiles(boolean isServer) {
		createDirectory(isServer ? serverSyncDirectory : clientSyncDirectory); // 创建同步文件夹
		createDirectory(HEX_SYNC_DIRECTORY); // 在当前目录下创建HexSync文件夹
		loadConfig(); // 加载配置文件
		if (isServer) serverMap = initializeMap(serverSyncDirectory, serverMap); // 初始化服务端文件列表
		else createDirectory(clientOnlyDirectory); // 创建仅客户端文件夹
		LOGGER.log(Level.INFO, isServer ? "服务端初始化完成" : "客户端初始化完成");
	}
	private static Map<String, String> initializeMap(String directory, Map<String, String> map) {
		map.clear(); // 清空同步文件列表
		File[] files = new File(directory).listFiles(); // 获取同步文件夹下的所有文件
		if (files == null) return null;
		for (File file : files)
			if (file.isFile()) {
				String SHAValue = calculateSHA(file);
				map.put(file.getName(), SHAValue);
			}
		return map;
	}
	// 检测并创建文件夹
	private static void createDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (!directory.exists())
			if (directory.mkdirs()) LOGGER.log(Level.INFO, "文件夹已创建: " + directoryPath);
			else LOGGER.log(Level.SEVERE, "无法创建文件夹: " + directoryPath);
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
			LOGGER.log(Level.WARNING, "配置格式不正确: " + line);
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
			case CLIENT_ONLY_DIRECTORY_CONFIG:
				clientOnlyDirectory = tail;
				break;
			case CLIENT_AUTO_START_CONFIG:
				clientAutoStart = Boolean.parseBoolean(tail);
				break;
			case MINE_CRAFT_UPDATE_MODE_CONFIG:
				updateMineCraftMode = Boolean.parseBoolean(tail);
				break;
			default:
				LOGGER.log(Level.WARNING, "不正确的配置项: " + head);
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
		return address.startsWith("https://") ? address.replace(
				"https://", "http://"
		) : "http://" + address;
	}
	// 计算SHA校验码
	private static String calculateSHA(File file) {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] byteBuffer = new byte[8192]; // 缓冲区大小
			int bytesRead; // 读取字节数
			MessageDigest SHA = MessageDigest.getInstance("SHA-256"); // 获取SHA算法
			while ((bytesRead = fileInputStream.read(byteBuffer)) != -1) SHA.update(byteBuffer, 0, bytesRead);
			byte[] digest = SHA.digest(); // 获取校验码
			StringBuilder stringBuilder = new StringBuilder();
			for (byte singleByte : digest) stringBuilder.append(String.format("%02x", singleByte));
			return stringBuilder.toString(); // 返回校验码
		} catch (Exception error) {
			LOGGER.log(Level.SEVERE, "计算校验码时出错: " + error.getMessage());
			return null;
		}
	}
	// 从服务器请求文件名和SHA值列表
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
				String SHAValue = in.readLine(); // 读取对应的SHA值
				if (SHAValue == null) continue;
				requestMap.put(fileName.trim(), SHAValue.trim()); // 将文件名与SHA值放入Map
			}
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "读取响应时出错: " + error.getMessage());
			isErrorDownload = true;
		}
		LOGGER.log(Level.INFO, "请求文件列表成功,共收到" + requestMap.size() + "个文件"); // 记录请求成功日志
		return requestMap;
	}
	// 从服务器下载文件
	private static boolean downloadFile(String filePath, Map<String, String> toDownloadMap) {
		if (clientHTTPThread == null) return false; // 客户端线程已关闭
		File clientFile = new File(filePath); // 目标本地文件
		String fileName = filePath.substring(clientSyncDirectory.length() + 1); // 去除同步文件夹路径
		String requestSHA = toDownloadMap.get(fileName);
		try {
			int responseCode = getResponseCode(new URL(
					HTTPFormat(serverAddress) + ":" + clientHTTPPort + "/download/" + requestSHA
			));
			if (responseCode != HttpURLConnection.HTTP_OK) {
				LOGGER.log(Level.SEVERE, "下载失败,HTTP错误代码: " + responseCode);
				return false;
			}
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "连接服务器时出错: " + error.getMessage());
			return false;
		}
		// 下载成功,读取输入流并写入本地文件
		try (
				InputStream inputStream = HTTPURLConnection.getInputStream();
				FileOutputStream outputStream = new FileOutputStream(filePath)
		) {
			byte[] buffer = new byte[8192]; // 8KB缓冲区
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "读取响应时出错: " + error.getMessage());
		}
		// 进行SHA校验
		if (requestSHA == null) {
			LOGGER.log(Level.SEVERE, "无法获取服务器的SHA值: " + fileName);
			return false;
		}
		String clientSHA = calculateSHA(clientFile);
		if (requestSHA.equals(clientSHA)) return true; // 下载成功且SHA校验通过
		LOGGER.log(Level.SEVERE, "校验失败,文件可能已损坏: " + fileName);
		if (!clientFile.delete()) LOGGER.log(Level.SEVERE, "无法删除损坏的文件: " + clientFile.getPath());
		return false;
	}
	private static int getResponseCode(URL requestURL) throws IOException {
		HTTPURLConnection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
		HTTPURLConnection.setRequestMethod("GET"); // 设置请求方式为GET
		return HTTPURLConnection.getResponseCode(); // 返回响应码
	}
	// 保存配置
	private static void saveConfig() {
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
				{CLIENT_ONLY_DIRECTORY_CONFIG, clientOnlyDirectory},
				{CLIENT_AUTO_START_CONFIG, String.valueOf(clientAutoStart)},
				{MINE_CRAFT_UPDATE_MODE_CONFIG, String.valueOf(updateMineCraftMode)},
		};
		StringBuilder configContent = new StringBuilder();
		for (String[] entry : configEntries) {
			if (entry[0].startsWith("#")) configContent.append(entry[0]).append(LINE_SEPARATOR);
			else configContent
					.append(entry[0])
					.append("=")
					.append(entry.length > 1 ? entry[1] : "")
					.append(LINE_SEPARATOR);
		}
		File configFile = new File(CONFIG_FILE_PATH);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
			writer.write(configContent.toString());// 写入配置文件
			LOGGER.log(Level.INFO, "配置已保存: " + LINE_SEPARATOR + configContent);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "配置保存失败: " + error.getMessage(), error);
		}
	}
	// 发送数据
	private static void sendHTTPResponse(HttpExchange exchange, byte[] responseBytes, int HTTPCode) {
		new Thread(() -> {
			if (responseBytes == null) return;
			int responseBytesLength = responseBytes.length;
			long maxUploadRateInBytes = convertToBytes(serverUploadRateLimit, serverUploadRateLimitUnit);
			long lastFillTime = System.currentTimeMillis(); // 最近一次填充时间
			try (OutputStream outputStream = exchange.getResponseBody()) {
				exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 设置Content-Type
				exchange.sendResponseHeaders(HTTPCode, responseBytesLength); // 设置响应头
				int totalBytesSent = 0; // 记录已发送字节数
				if (serverUploadRateLimit == 0) {
					outputStream.write(responseBytes);
					return; // 直接返回，不再执行后续代码
				}
				while (totalBytesSent < responseBytesLength) {
					long currentTime = System.currentTimeMillis();
					availableTokens.addAndGet((currentTime - lastFillTime) * maxUploadRateInBytes / 1000);
					lastFillTime = currentTime; // 更新时间
					// 尝试发送数据
					int bytesToSend = Math.min(16384, responseBytesLength - totalBytesSent); // 每次最多发送16KB
					if (availableTokens.get() >= bytesToSend) {
						outputStream.write(responseBytes, totalBytesSent, bytesToSend); // 写入数据
						totalBytesSent += bytesToSend; // 更新已发送字节数
						availableTokens.addAndGet(-bytesToSend); // 减少可用令牌
					} else {
						// 如果没有足够的令牌，计算需要等待的时间
						long requiredTokens = bytesToSend - availableTokens.get();
						long sleepTime = (requiredTokens * 1000) / maxUploadRateInBytes;
						Thread.sleep(sleepTime); // 暂停
					}
				}
			} catch (Exception error) {
				LOGGER.log(Level.SEVERE, "发送响应时出错: " + error.getMessage());
			}
		}).start(); // 启动新线程
	}
	// 单位转换方法
	private static long convertToBytes(long value, String unit) {
		try {
			switch (unit) {
				case "B/s":
					return value;
				case "KB/s":
					return Math.multiplyExact(value, 1024);
				case "MB/s":
					return Math.multiplyExact(value, 1024 * 1024);
				case "GB/s":
					return Math.multiplyExact(value, 1024 * 1024 * 1024);
				default:
					LOGGER.log(Level.WARNING, "未知的上传速率单位: " + unit);
					return 0;
			}
		} catch (ArithmeticException error) {
			return Long.MAX_VALUE; // 溢出，返回最大值
		}
	}
	// 检查文件是否已存在并进行SHA校验
	private static boolean checkNoFile(File localFile, String fileName, Map<String, String> requestMap) {
		if (!localFile.exists()) return true; // 文件不存在,直接返回
		String serverSHA = requestMap.get(fileName); // 从服务端请求的文件列表中获取SHA值
		if (serverSHA == null) return true;
		return !serverSHA.equals(calculateSHA(localFile));
	}
	private static void openLog() {
		try {
			String os = System.getProperty("os.name").toLowerCase();// 检查操作系统类型
			String command;
			if (os.contains("win")) {// Windows平台
				command = "Get-Content -Path '" + LOG_FILE + "' -Encoding utf8 -Wait";
				Runtime.getRuntime().exec(new String[]{
						"cmd.exe", "/c", "start", "powershell.exe", "-Command", command
				});
			} else if (os.contains("mac")) {// macOS平台
				command = "tail -f " + LOG_FILE;
				Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
			} else if (os.contains("nix") || os.contains("nux")) {// Linux平台
				command = "tail -f " + LOG_FILE;
				Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", command});
			} else LOGGER.log(Level.WARNING, "不支持的操作系统: " + os);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "打开命令行读取日志文件时出错: " + error.getMessage());
		}
	}
	private static void headlessSettingsHelp() {
		System.out.println("设置服务端HTTP端口: 'sp <端口号>'");
		System.out.println("设置服务端上传速率: 'sl <速率> <'B/s'/'KB/s'/'MB/s'/'GB/s'>'");
		System.out.println("设置服务端同步目录: 'sd <目录>'");
		System.out.println("设置服务端自动启动: 'ss <y/n>'");
		System.out.println("设置客户端HTTP端口: 'cp <端口号>'");
		System.out.println("设置服务端地址: 'sa <地址>'");
		System.out.println("设置客户端同步目录: 'cd <目录>'");
		System.out.println("设置客户端自动启动: 'cs <y/n>'");
		System.out.println("帮助: 'help'");
		System.out.println("GitHub地址: 'github'");
		System.out.println("保存并退出: 'save'");
	}
	// 处理请求
	private static void processHTTPRequest(String requestURI, HttpExchange exchange) throws IOException {
		LOGGER.log(Level.INFO, "收到请求: " + requestURI);
		byte[] responseBytes = "".getBytes();
		int HTTPCode = HttpURLConnection.HTTP_NOT_FOUND;
		switch (requestURI) {
			case "/list":
				StringBuilder responseBuilder = new StringBuilder(); // 用于构建响应内容
				for (Map.Entry<String, String> entry : serverMap.entrySet()) {
					String fileName = entry.getKey();
					String SHAValue = entry.getValue();
					responseBuilder.append(fileName).append(LINE_SEPARATOR).append(SHAValue).append(LINE_SEPARATOR);
				}
				responseBytes = responseBuilder.toString().getBytes(); // 转换为字节数组
				HTTPCode = HttpURLConnection.HTTP_OK; // 设置响应码为200
				break;
			case "/favicon.ico":
				responseBytes = Files.readAllBytes(Paths.get("IconI.png")); // 读取为字节数组
				HTTPCode = HttpURLConnection.HTTP_OK;
				break;
			default:
				if (requestURI.startsWith("/download/")) {
					String requestSHA = requestURI.substring(requestURI.lastIndexOf("/") + 1);
					String filePath = null;
					for (Map.Entry<String, String> entry : serverMap.entrySet()) {
						if (!requestSHA.equals(entry.getValue())) continue;
						filePath = serverSyncDirectory + FILE_SEPARATOR + entry.getKey();
						break;
					}
					// 读取文件路径
					if (filePath == null) break;
					File file = new File(filePath); // 构造文件对象
					if (serverMap.containsValue(requestSHA) && file.exists() && file.isFile()) {
						try (InputStream inputStream = Files.newInputStream(file.toPath())) {
							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							byte[] buffer = new byte[4096]; // 设定每次读取的字节数
							int bytesRead;
							while ((bytesRead = inputStream.read(buffer)) != -1)
								outputStream.write(buffer, 0, bytesRead); // 将读取到的字节写入输出流
							responseBytes = outputStream.toByteArray(); // 转换为字节数组
						} catch (IOException e) {
							LOGGER.log(Level.SEVERE, "读取文件时发生错误: " + e.getMessage());
						}
						HTTPCode = HttpURLConnection.HTTP_OK; // 发送成功,返回200
						LOGGER.log(Level.INFO, "已发送文件: " + filePath);
					}
				} else LOGGER.log(Level.WARNING, "未知的请求: " + requestURI);
				break;
		}
		sendHTTPResponse(exchange, responseBytes, HTTPCode);
	}
	// 检测端口号有效性的方法
	private static boolean isPort(String portInput) {
		if (
				isNumberInLong(portInput)
						&& portInput.trim().length() < 6
						&& Integer.parseInt(portInput) > 0
						&& Integer.parseInt(portInput) < 65536
		) return true;
		LOGGER.log(Level.WARNING, "端口号不在0~65535的范围内: " + portInput);
		return false; // 端口号不合法
	}
	private static boolean isNumberInLong(String numberInput) {
		if (numberInput == null || numberInput.trim().isEmpty()) return false; // 空字符串返回false
		try {
			Long.parseLong(numberInput.trim()); // 尝试将文本转换为长整型
			return true; // 转换成功，返回true
		} catch (NumberFormatException error) {
			LOGGER.log(Level.WARNING, "不正确的数字格式或超出范围: " + numberInput);
			return false; // 转换失败，返回false
		}
	}
	private static void configSaveButtonAction(
			JTextField serverPortField,
			JTextField serverUploadRateLimitField,
			JTextField serverSyncDirectoryPathField,
			JTextField clientPortField,
			JTextField clientAddressField,
			JTextField clientSyncDirectoryPathField,
			JTextField clientOnlyDirectoryField,
			JTabbedPane tabbedPane,
			JCheckBox serverAutoStartBox,
			JCheckBox clientAutoStartBox,
			JCheckBox updateMineCraftModeBox,
			JComboBox<String> serverUploadRateLimitUnitBox,
			JDialog settingsDialog
	) {
		// 定义输入框数组及其对应的提示信息和选项卡索引
		Object[][] inputs = {
				{serverPortField, "服务端端口", 0},
				{serverUploadRateLimitField, "上传速率限制", 0},
				{serverSyncDirectoryPathField, "服务端同步文件夹路径", 0},
				{clientPortField, "客户端端口", 1},
				{clientAddressField, "服务器地址", 1},
				{clientSyncDirectoryPathField, "客户端同步文件夹路径", 1},
				{clientOnlyDirectoryField, "客户端仅同步文件夹路径", 1},
		};
		// 检查输入框是否为空
		for (Object[] input : inputs) {
			JTextField textField = (JTextField) input[0];
			String fieldName = (String) input[1];
			int tabIndex = (int) input[2]; // 获取对应的索引
			if (textField.getText().trim().isEmpty()) {
				tabbedPane.setSelectedIndex(tabIndex); // 跳转到对应的选项卡
				textField.selectAll(); // 选中输入框
				textField.requestFocus(); // 聚焦输入框
				LOGGER.log(Level.WARNING, fieldName + "不能为空");
				return;
			}
		}
		// 检测端口号是否是数字且在合法范围内
		if (!isPort(serverPortField.getText().trim())) {
			serverPortField.selectAll(); // 选中输入框
			serverPortField.requestFocus(); // 聚焦输入框
			return;
		}
		if (!isPort(clientPortField.getText().trim())) {
			clientPortField.selectAll(); // 选中输入框
			clientPortField.requestFocus(); // 聚焦输入框
			return;
		}
		// 检测上传速率上限
		String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
		if (!isNumberInLong(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
			LOGGER.log(Level.WARNING, "上传速率上限不正确: " + uploadRateLimitText);
			tabbedPane.setSelectedIndex(0);
			serverUploadRateLimitField.selectAll(); // 选中输入框
			serverUploadRateLimitField.requestFocus(); // 聚焦输入框
			return;
		}
		serverAutoStart = serverAutoStartBox.isSelected();
		clientAutoStart = clientAutoStartBox.isSelected();
		serverHTTPPort = Integer.parseInt(serverPortField.getText().trim());
		serverUploadRateLimit = Long.parseLong(uploadRateLimitText);
		serverUploadRateLimitUnit = (String) serverUploadRateLimitUnitBox.getSelectedItem();
		serverSyncDirectory = serverSyncDirectoryPathField.getText().trim();
		clientHTTPPort = Integer.parseInt(clientPortField.getText().trim());
		serverAddress = clientAddressField.getText().trim();
		clientSyncDirectory = clientSyncDirectoryPathField.getText().trim();
		clientOnlyDirectory = clientOnlyDirectoryField.getText().trim();
		updateMineCraftMode = updateMineCraftModeBox.isSelected();
		saveConfig(); // 保存配置
		settingsDialog.dispose(); // 关闭对话框
	}
	private static void aboutButtonAction(Dialog dialog) {
		JScrollPane scrollPane = getJScrollPane(
				"<html><body style=\"font-family: sans-serif;\">"
						+ HEX_SYNC_NAME + "<br>By: ForgeStove<br>GitHub: <a href=\""
						+ GITHUB_URL + "\">"
						+ GITHUB_URL + "</a>" + "</body></html>"
		);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		scrollPane.setPreferredSize(new Dimension(screenSize.width / 5, screenSize.height / 15));
		// 创建自定义对话框
		JDialog aboutDialog = new JDialog(dialog, "关于", Dialog.ModalityType.MODELESS);
		aboutDialog.getContentPane().add(scrollPane); // 添加内容
		aboutDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // 关闭方式
		aboutDialog.pack(); // 调整大小
		aboutDialog.setLocationRelativeTo(dialog); // 居中显示
		aboutDialog.setVisible(true);// 显示对话框
	}
	private static JScrollPane getJScrollPane(String htmlContent) {
		JEditorPane aboutEditorPane = new JEditorPane("text/html", htmlContent);
		aboutEditorPane.setEditable(false);
		aboutEditorPane.addHyperlinkListener(hyperlinkEvent -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(hyperlinkEvent.getEventType())) {
				try {
					Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
				} catch (Exception error) {
					LOGGER.log(Level.SEVERE, "打开链接时出错: " + error.getMessage());
				}
			}
		});
		return new JScrollPane(aboutEditorPane);
	}
	private static void headlessUI() {
		System.out.println("欢迎使用" + HEX_SYNC_NAME + "!");
		System.out.println("输入 'help' 以获取帮助.");
		if (serverAutoStart) startHTTPServer();
		if (clientAutoStart) startHTTPClient();
		Scanner scanner = new Scanner(System.in);
		String command;
		while (true) {
			System.out.print(HEX_SYNC_NAME + "> ");
			command = scanner.nextLine();
			switch (command) {
				case "rs":
					startHTTPServer();
					break;
				case "rc":
					startHTTPClient();
					break;
				case "ss":
					stopHTTPServer();
					break;
				case "sc":
					stopHTTPClient();
					break;
				case "set":
					headlessSettings();
					break;
				case "help":
					System.out.println(" 启动服务端: 'rs'");
					System.out.println(" 启动客户端: 'rc'");
					System.out.println(" 停止服务端: 'ss'");
					System.out.println(" 停止客户端: 'sc'");
					System.out.println(" 设置: 'set'");
					System.out.println(" 退出: 'exit'");
					break;
				case "exit":
					System.exit(0);
					break;
				default:
					System.out.println("无效命令,输入 'help' 以获取帮助.");
			}
		}
	}
	private static void headlessSettings() {
		headlessSettingsHelp();
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print(HEX_SYNC_NAME + "Settings> ");
			String input = scanner.nextLine();
			String[] parts = input.split("\\s+");
			String command = parts[0];
			// 检查是否有参数
			if (parts.length < 2 && !command.equals("save") && !command.equals("help") && !command.equals("github")) {
				System.out.println("无效命令或缺少参数, 输入 'help' 以获取帮助.");
				continue;
			}
			switch (command) {
				case "sp":
					String portInput = parts[1];
					if (isPort(portInput)) {
						serverHTTPPort = Integer.parseInt(portInput);
						LOGGER.log(Level.INFO, "服务端HTTP端口已设置为: " + serverHTTPPort);
					}
					continue;
				case "sl":
					String rateInput = input.substring(command.length()).trim();
					if (rateInput.matches("\\d+(\\s+B/s|\\s+KB/s|\\s+MB/s|\\s+GB/s)")) {
						String[] rateParts = rateInput.split("\\s+");
						if (!isNumberInLong(rateParts[0])) {
							System.out.println("无效输入,请输入数字.");
							continue;
						}
						serverUploadRateLimit = Long.parseLong(rateParts[0]);
						serverUploadRateLimitUnit = rateParts[1];
						LOGGER.log(Level.INFO,
								"服务端上传速率已设置为: " + serverUploadRateLimit + " " + serverUploadRateLimitUnit
						);
					} else System.out.println("无效输入,请输入数字及单位.");
					continue;
				case "sd":
					String syncDirInput = parts[1];
					if (!syncDirInput.isEmpty() && !syncDirInput.contains(FILE_SEPARATOR)) {
						serverSyncDirectory = syncDirInput;
						LOGGER.log(Level.INFO, "服务端同步目录已设置为: " + serverSyncDirectory);
					} else System.out.println("同步目录格式错误,请输入绝对路径或相对路径.");
					continue;
				case "ss":
					String autoStartInput = parts[1];
					if (autoStartInput.matches("[yYnN]")) {
						serverAutoStart = autoStartInput.matches("[yY]");
						LOGGER.log(Level.INFO, "服务端自动启动已设置为: " + serverAutoStart);
					} else System.out.println("无效输入,请输入'y'/'Y'或'n'/'N'.");
					continue;
				case "cp":
					String clientPortInput = parts[1];
					if (isPort(clientPortInput)) {
						clientHTTPPort = Integer.parseInt(clientPortInput);
						LOGGER.log(Level.INFO, "客户端HTTP端口已设置为: " + clientHTTPPort);
					}
					continue;
				case "sa":
					String addressInput = parts[1];
					if (addressInput.matches("\\d+\\.\\d+")) {
						serverAddress = addressInput;
						LOGGER.log(Level.INFO, "服务端地址已设置为: " + serverAddress);
					} else System.out.println("无效输入,请输入IP地址.");
					continue;
				case "cd":
					String clientSyncDirInput = parts[1];
					if (!clientSyncDirInput.isEmpty() && !clientSyncDirInput.contains(FILE_SEPARATOR)) {
						clientSyncDirectory = clientSyncDirInput;
						LOGGER.log(Level.INFO, "客户端同步目录已设置为: " + clientSyncDirectory);
					} else System.out.println("同步目录格式错误,请输入绝对路径或相对路径.");
					continue;
				case "cs":
					String clientAutoStartInput = parts[1];
					if (clientAutoStartInput.matches("[yYnN]")) {
						clientAutoStart = clientAutoStartInput.matches("[yY]");
						LOGGER.log(Level.INFO, "客户端自动启动已设置为: " + clientAutoStart);
					} else System.out.println("无效输入,请输入'y'/'Y'或'n'/'N'.");
					continue;
				case "save":
					saveConfig();
					return;
				case "help":
					headlessSettingsHelp();
					continue;
				case "github":
					System.out.println(GITHUB_URL);
					continue;
				default:
					System.out.println("无效命令,输入 'help' 以获取帮助.");
			}
		}
	}
	private static void stopHTTPServer() {
		LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server正在关闭...");
		if (HTTPServer != null) {
			HTTPServer.stop(0); // 停止服务
			serverHTTPThread = null; // 清除线程引用
		}
		LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server已关闭");
	}
	private static void startHTTPServer() {
		serverHTTPThread = new Thread(() -> {
			LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server正在启动...");
			initializeFiles(true);
			if (serverMap.isEmpty()) {
				LOGGER.log(Level.WARNING, serverSyncDirectory + "无文件,无法启动服务器");
				stopHTTPServer();
				return;
			}
			try {
				HTTPServer = HttpServer.create(new InetSocketAddress(serverHTTPPort), 0);
				HTTPServer.createContext("/", new HexSync());
				HTTPServer.setExecutor(null);
				HTTPServer.start();
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "服务器异常: " + error.getMessage());
			}
			LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Server正在运行...端口号为: " + serverHTTPPort);
		});
		serverHTTPThread.start();
	}
	private static void stopHTTPClient() {
		if (HTTPURLConnection != null) HTTPURLConnection.disconnect();
		if (clientHTTPThread != null) {
			clientHTTPThread = null; // 清除线程引用
			LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Client已关闭");
		}
		if (clientAutoStart && !isErrorDownload) System.exit(0); // 自动退出
	}
	private static void startHTTPClient() {
		clientHTTPThread = new Thread(() -> {
			LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Client正在启动...");
			initializeFiles(false);
			Map<String, String> requestMap = requestHTTPList();
			if (requestMap.isEmpty()) isErrorDownload = true;
			else {
				LOGGER.log(Level.INFO, "获取到 " + requestMap.size() + " 个文件");
				Map<String, String> toDownloadMap = new HashMap<>(); // 用于存储需要下载的文件列表
				Map<String, String> clientOnlyMap = new HashMap<>(); // 用于存储客户端仅同步的文件列表
				clientOnlyMap = initializeMap(clientOnlyDirectory, clientOnlyMap); // 初始化客户端仅同步的文件列表
				if (updateMineCraftMode) deleteFilesNotInMap(requestMap, clientOnlyMap);
				Map<String, String> clientMap = new HashMap<>(); // 用于存储客户端文件列表
				clientMap = initializeMap(clientSyncDirectory, clientMap); // 初始化客户端文件列表
				createToDownloadMap(requestMap, toDownloadMap, clientMap); // 构建需要下载的文件列表
				downloadFilesInMap(toDownloadMap); // 下载文件
				if (updateMineCraftMode) copyAllFiles(clientOnlyDirectory, clientSyncDirectory);
			}
			stopHTTPClient();
		});
		clientHTTPThread.start();
	}
	private static void deleteFilesNotInMap(Map<String, String> requestMap, Map<String, String> clientOnlyMap) {
		File[] files = new File(clientSyncDirectory).listFiles();
		if (files != null) for (File file : files) {
			String fileName = file.getName();
			if (file.isFile()
					&& !requestMap.containsKey(fileName)
					&& !clientOnlyMap.containsKey(fileName)
					&& checkNoFile(file, fileName, requestMap)
					&& checkNoFile(file, fileName, clientOnlyMap)
			) if (file.delete()) LOGGER.log(Level.INFO, "已删除不存在于服务端的文件: " + fileName);
			else LOGGER.log(Level.SEVERE, "删除文件失败: " + fileName);
		}
	}
	private static void copyAllFiles(String source, String target) {
		File targetDirectory = new File(target);
		createDirectory(String.valueOf(targetDirectory));
		// 获取源目录下的所有文件和目录
		File[] files = new File(source).listFiles();
		if (files != null) for (File file : files) {
			File targetFile = new File(targetDirectory, file.getName());
			// 递归复制子目录
			if (file.isDirectory()) copyAllFiles(file.getAbsolutePath(), targetFile.getAbsolutePath());
			else {
				if (targetFile.exists()) continue;// 检查目标文件是否存在
				// 复制文件
				try (InputStream in = Files.newInputStream(file.toPath());
					 OutputStream out = Files.newOutputStream(targetFile.toPath())) {
					byte[] buffer = new byte[8192];
					int length;
					while ((length = in.read(buffer)) > 0) {
						out.write(buffer, 0, length);
					}
					LOGGER.log(Level.INFO, "已复制文件: " + file.getName() + " 到 " + targetFile.getAbsolutePath());
				} catch (IOException error) {
					LOGGER.log(Level.SEVERE, "复制文件失败: " + file.getName(), error);
				}
			}
		}
	}
	// 构建需要下载的文件列表
	private static void createToDownloadMap(
			Map<String, String> requestMap, Map<String, String> toDownloadMap, Map<String, String> clientMap
	) {
		for (Map.Entry<String, String> entry : requestMap.entrySet()) {
			String fileName = entry.getKey();
			String SHAValue = entry.getValue();
			if (fileName.isEmpty() || SHAValue.isEmpty()) return;
			if (!clientMap.containsKey(fileName) && !clientMap.containsValue(SHAValue) && checkNoFile(
					new File(clientSyncDirectory + FILE_SEPARATOR + fileName), fileName, requestMap
			)) toDownloadMap.put(fileName, SHAValue);
		}
	}
	// 下载需要的文件
	private static void downloadFilesInMap(Map<String, String> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			LOGGER.log(Level.INFO, "同步目录已是最新,无需下载.");
			return;
		}
		LOGGER.log(Level.INFO, "开始下载 " + toDownloadMap.size() + " 个文件");
		int downloadedCount = 0;
		int toDownloadMapSize = toDownloadMap.size();
		for (Map.Entry<String, String> entry : toDownloadMap.entrySet()) {
			String filePath = clientSyncDirectory + FILE_SEPARATOR + entry.getKey(); // 设置下载路径
			if (downloadFile(filePath, toDownloadMap)) {
				downloadedCount++; // 成功下载时增加计数
				LOGGER.log(
						Level.INFO, "已下载: [" + downloadedCount + "/" + toDownloadMapSize + "] " + filePath
				);
			} else {
				LOGGER.log(Level.SEVERE, "下载失败: " + filePath);
				isErrorDownload = true; // 记录下载失败
			}
		}
		LOGGER.log(Level.INFO, "下载完成: [" + downloadedCount + "/" + toDownloadMapSize + "]");
	}
	private static void createUI() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		JDialog dialog = new JDialog();
		dialog.setTitle(HEX_SYNC_NAME + " 控制面板");
		dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		dialog.setSize(screenSize.width / 5, screenSize.height / 10);
		dialog.setLocationRelativeTo(null); // 居中显示
		// 创建按钮面板
		buttonPanel(dialog, screenSize);
		icon(dialog);
		if (serverAutoStart) startHTTPServer(); // 启动服务端
		if (clientAutoStart) startHTTPClient(); // 启动客户端
		dialog.setVisible(true); // 显示对话框
	}
	private static void buttonPanel(Dialog dialog, Dimension screenSize) {
		JButton openLogButton = new JButton("日志");
		JButton settingsButton = new JButton("设置");
		JButton shutdownButton = new JButton("退出");
		JButton toggleServerButton = new JButton("启动服务端");
		JButton toggleClientButton = new JButton("启动客户端");
		// 设置按钮的首选大小
		Dimension buttonSize = new Dimension(screenSize.width / 17, screenSize.height / 40);
		openLogButton.setPreferredSize(buttonSize);
		settingsButton.setPreferredSize(buttonSize);
		shutdownButton.setPreferredSize(buttonSize);
		toggleServerButton.setPreferredSize(buttonSize);
		toggleClientButton.setPreferredSize(buttonSize);
		// 去除按钮的默认边框
		openLogButton.setFocusPainted(false);
		settingsButton.setFocusPainted(false);
		shutdownButton.setFocusPainted(false);
		toggleServerButton.setFocusPainted(false);
		toggleClientButton.setFocusPainted(false);
		// 添加按钮监听事件
		actionListener(dialog, openLogButton, settingsButton, shutdownButton, toggleServerButton, toggleClientButton);
		// 添加按钮到按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(openLogButton);
		buttonPanel.add(settingsButton);
		buttonPanel.add(shutdownButton);
		buttonPanel.add(toggleServerButton);
		buttonPanel.add(toggleClientButton);
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(buttonPanel, BorderLayout.CENTER); // 添加按钮面板到主面板
		dialog.add(panel);  // 添加主面板到窗口
	}
	// 按钮监听事件
	private static void actionListener(
			Dialog dialog, JButton openLogButton, JButton settingsButton,
			JButton shutdownButton, JButton toggleServerButton, JButton toggleClientButton
	) {
		openLogButton.addActionListener(event -> openLog());
		toggleButtonAction(
				toggleServerButton, HexSync::startHTTPServer, HexSync::stopHTTPServer,
				"启动服务端", "停止服务端", dialog
		);
		toggleButtonAction(
				toggleClientButton, HexSync::startHTTPClient, HexSync::stopHTTPClient,
				"启动客户端", "停止客户端", dialog
		);
		settingsButton.addActionListener(event -> openSettingsDialog(dialog)); // 打开设置对话框
		shutdownButton.addActionListener(event -> System.exit(0)); // 关闭程序
	}
	// 处理按钮事件的通用方法
	private static void toggleButtonAction(
			JButton button, Runnable startAction, Runnable stopAction,
			String startText, String stopText, Dialog dialog
	) {
		button.addActionListener(event -> {
			button.setEnabled(false);
			if (button.getText().equals(startText)) startAction.run();
			else stopAction.run();
			button.setText(button.getText().equals(startText) ? stopText : startText);
			icon(dialog);
			button.setEnabled(true);
		});
	}
	// 切换图标
	private static void icon(Dialog dialog) {
		dialog.setIconImage(Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource(
				serverHTTPThread != null || clientHTTPThread != null ? "IconO.png" : "IconI.png"
		)));
		if (!SystemTray.isSupported()) return;
		TrayIcon trayIcon; // 托盘图标
		boolean running = serverHTTPThread != null || clientHTTPThread != null; // 状态标记
		trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource(
				running ? "IconO.png" : "IconI.png")), HEX_SYNC_NAME
		);
		trayIcon.setImageAutoSize(true); // 自动调整图标大小
		trayIcon.setToolTip(HEX_SYNC_NAME + "控制面板");
		trayIcon.setPopupMenu(popupMenu(dialog));
		trayIcon.addActionListener(event -> dialog.setVisible(true));
		try {
			SystemTray systemTray = SystemTray.getSystemTray();
			if (systemTray.getTrayIcons().length == 0) systemTray.add(trayIcon);
			else {
				TrayIcon existingIcon = systemTray.getTrayIcons()[0];
				existingIcon.setImage(Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource(
						running ? "IconO.png" : "IconI.png"
				)));
			}
		} catch (AWTException error) {
			LOGGER.log(Level.SEVERE, "添加托盘图标失败: " + error.getMessage());
		}
	}
	private static PopupMenu popupMenu(Dialog dialog) {
		PopupMenu popupMenu = new PopupMenu();
		MenuItem openItem = new MenuItem("Open");
		MenuItem hideItem = new MenuItem("Hide");
		MenuItem settingsItem = new MenuItem("Settings");
		MenuItem aboutItem = new MenuItem("About");
		MenuItem exitItem = new MenuItem("Exit");
		openItem.addActionListener(event -> dialog.setVisible(true));
		hideItem.addActionListener(event -> dialog.setVisible(false));
		settingsItem.addActionListener(event -> openSettingsDialog(dialog));
		aboutItem.addActionListener(event -> aboutButtonAction(dialog));
		exitItem.addActionListener(event -> System.exit(0));
		// 将菜单项添加到弹出菜单
		popupMenu.add(openItem);
		popupMenu.addSeparator();
		popupMenu.add(hideItem);
		popupMenu.addSeparator();
		popupMenu.add(settingsItem);
		popupMenu.addSeparator();
		popupMenu.add(aboutItem);
		popupMenu.addSeparator();
		popupMenu.add(exitItem);
		return popupMenu;
	}
	// 打开设置对话框
	private static void openSettingsDialog(Dialog dialog) {
		loadConfig();
		JDialog settingsDialog = new JDialog(dialog, "设置", Dialog.ModalityType.MODELESS);
		// 创建选项卡面板
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		JPanel clientPanel = new JPanel(new GridLayout(5, 2));
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
		// 创建文本框
		JTextField clientPortField = new JTextField(String.valueOf(clientHTTPPort));
		JTextField clientAddressField = new JTextField(serverAddress);
		JTextField clientSyncDirectoryPathField = new JTextField(clientSyncDirectory);
		JTextField clientOnlyDirectoryPathField = new JTextField(clientOnlyDirectory);
		// 添加标签和文本框
		clientPanel.add(new JLabel("端口号: "));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel("服务器地址: "));
		clientPanel.add(clientAddressField);
		clientPanel.add(new JLabel("客户端同步文件夹路径: "));
		clientPanel.add(clientSyncDirectoryPathField);
		clientPanel.add(new JLabel("仅客户端模组文件夹路径: "));
		clientPanel.add(clientOnlyDirectoryPathField);
		// 添加选项卡到选项卡面板
		JCheckBox serverAutoStartBox = new JCheckBox("自动启动服务端");
		JCheckBox clientAutoStartBox = new JCheckBox("自动启动客户端");
		JCheckBox updateMineCraftModeBox = new JCheckBox("MineCraft更新模式");
		serverAutoStartBox.setFocusPainted(false);
		clientAutoStartBox.setFocusPainted(false);
		updateMineCraftModeBox.setFocusPainted(false);
		// 设置复选框初始状态
		serverAutoStartBox.setSelected(serverAutoStart);
		clientAutoStartBox.setSelected(clientAutoStart);
		updateMineCraftModeBox.setSelected(updateMineCraftMode);
		// 添加复选框到设置面板
		serverPanel.add(serverAutoStartBox);
		clientPanel.add(clientAutoStartBox);
		clientPanel.add(updateMineCraftModeBox);
		// 添加选项卡面板到设置对话框
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("服务端设置", serverPanel);
		tabbedPane.addTab("客户端设置", clientPanel);
		tabbedPane.setFocusable(false);
		settingsDialog.add(tabbedPane, BorderLayout.CENTER);
		JButton configSaveButton = new JButton("保存");
		JButton cancelButton = new JButton("取消");
		JButton aboutButton = new JButton("关于");
		configSaveButton.setFocusPainted(false);
		cancelButton.setFocusPainted(false);
		aboutButton.setFocusPainted(false);
		// 按钮监听事件
		configSaveButton.addActionListener(event -> configSaveButtonAction(
				serverPortField,
				serverUploadRateLimitField,
				serverSyncDirectoryPathField,
				clientPortField,
				clientAddressField,
				clientSyncDirectoryPathField,
				clientOnlyDirectoryPathField,
				tabbedPane,
				serverAutoStartBox,
				clientAutoStartBox,
				updateMineCraftModeBox,
				serverUploadRateLimitUnitBox,
				settingsDialog
		));
		cancelButton.addActionListener(event -> settingsDialog.dispose()); // 关闭对话框而不保存
		aboutButton.addActionListener(event -> aboutButtonAction(dialog));
		// 按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(configSaveButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(aboutButton);
		// 添加按钮面板到对话框的南部
		settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
		settingsDialog.pack(); // 自动调整大小
		settingsDialog.setLocationRelativeTo(dialog); // 居中
		settingsDialog.setVisible(true); // 显示对话框
	}
	@Override
	public void handle(HttpExchange exchange) {
		try {
			String requestMethod = exchange.getRequestMethod(); // 获取请求方法
			if (!requestMethod.equalsIgnoreCase("GET")) return;
			processHTTPRequest(exchange.getRequestURI().getPath(), exchange); // 处理请求
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "处理请求时出错: " + error.getMessage(), error);
		}
	}
}