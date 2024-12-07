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
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
public class HexSync implements HttpHandler {
	private static final String HEX_SYNC_NAME = HexSync.class.getName(); // 程序名称
	private static final Logger LOGGER = Logger.getLogger(HEX_SYNC_NAME); // 日志记录器
	private static final Map<String, String> SERVER_MAP = new HashMap<>(); // 存储服务端文件名和对应的SHA512数据
	private static final String FILE_SEPARATOR = File.separator; // 文件分隔符
	private static final String LINE_SEPARATOR = System.lineSeparator(); // 换行符
	private static final String HEX_SYNC_DIRECTORY = HEX_SYNC_NAME; // 文件夹目录
	private static final String LOG_FILE = HEX_SYNC_DIRECTORY + FILE_SEPARATOR + HEX_SYNC_NAME + ".log"; // 日志文件路径
	private static final String CONFIG_FILE_PATH = HEX_SYNC_DIRECTORY + FILE_SEPARATOR + HEX_SYNC_NAME + "Config.properties"; // 配置文件路径
	private static final String SERVER_AUTO_START_CONFIG = "ServerAutoStart"; // 服务端自动启动配置项
	private static final String SERVER_HTTP_PORT_CONFIG = "ServerHTTPPort"; // 服务端端口配置项
	private static final String SERVER_SYNC_DIRECTORY_CONFIG = "ServerSyncDirectoryPath"; // 服务端同步文件夹路径配置项
	private static final String SERVER_UPLOAD_RATE_LIMIT_CONFIG = "ServerUploadRateLimit"; // 上传速率限制配置项
	private static final String CLIENT_HTTP_PORT_CONFIG = "ClientHTTPPort"; // 客户端端口配置项
	private static final String SERVER_ADDRESS_CONFIG = "ServerAddress"; // 服务器地址配置项
	private static final String CLIENT_SYNC_DIRECTORY_CONFIG = "ClientSyncDirectoryPath"; // 客户端同步文件夹路径配置项
	private static final String CLIENT_AUTO_START_CONFIG = "clientAutoStart"; // 客户端自动启动配置项
	private static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync";
	private static String serverSyncDirectory = "mods"; // 服务端同步文件夹目录，默认值"mods"
	private static String clientSyncDirectory = "mods"; // 客户端同步文件夹目录，默认值"mods"
	private static String serverUploadRateLimitUnit = "MB/s"; // 上传速率限制单位，默认MB/s
	private static String serverAddress = "localhost"; // 服务器地址闭
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
				fileHandler.setFormatter(new SimpleFormatter()); // 设置日志格式化器
				LOGGER.addHandler(fileHandler); // 将FileHandler添加到日志记录器
			} catch (IOException | SecurityException error) {
				LOGGER.log(Level.SEVERE, "初始化日志时出错: " + error.getMessage(), error);
			}
		}).start();
	}
	// 初始化UI
	private static void initializeUI(String[] args) {
		if (Arrays.asList(args).contains("-headless") || GraphicsEnvironment.isHeadless()) headlessUI();
		String lookAndFeel = UIManager.getSystemLookAndFeelClassName();// 外观初始化
		try {
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
				 UnsupportedLookAndFeelException error) {
			LOGGER.log(Level.SEVERE, "设置外观失败: " + error.getMessage());
		}
		SwingUtilities.invokeLater(HexSync::createUI);// 创建窗口
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
		Arrays.stream(files).filter(File::isFile).forEach(file -> {
			String SHA512Value = calculateSHA512(file);
			SERVER_MAP.put(file.getName(), SHA512Value);
		});
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
		StringBuilder fileListBuilder = new StringBuilder("接收到文件列表:" + LINE_SEPARATOR);
		logMap(requestMap, fileListBuilder);
		return requestMap;
	}
	private static void logMap(Map<String, String> map, StringBuilder fileListBuilder) {
		map.forEach((key, value) -> fileListBuilder.append(key).append(LINE_SEPARATOR));
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
			try (InputStream inputStream = HTTPURLConnection.getInputStream(); FileOutputStream outputStream = new FileOutputStream(filePath)) {
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
		String[][] configEntries = {{"# 服务端配置"}, {SERVER_HTTP_PORT_CONFIG, String.valueOf(serverHTTPPort)}, {SERVER_UPLOAD_RATE_LIMIT_CONFIG, serverUploadRateLimit + " " + serverUploadRateLimitUnit}, {SERVER_SYNC_DIRECTORY_CONFIG, serverSyncDirectory}, {SERVER_AUTO_START_CONFIG, String.valueOf(serverAutoStart)}, {"# 客户端配置"}, {CLIENT_HTTP_PORT_CONFIG, String.valueOf(clientHTTPPort)}, {SERVER_ADDRESS_CONFIG, serverAddress}, {CLIENT_SYNC_DIRECTORY_CONFIG, clientSyncDirectory}, {CLIENT_AUTO_START_CONFIG, String.valueOf(clientAutoStart)},};
		// 构建配置内容
		StringBuilder configContent = new StringBuilder();
		Arrays.stream(configEntries).forEach(entry -> {
			if (entry[0].startsWith("#")) configContent.append(entry[0]).append(LINE_SEPARATOR);
			else
				configContent.append(entry[0]).append("=").append(entry.length > 1 ? entry[1] : "").append(LINE_SEPARATOR);
		});
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
		return SERVER_MAP.entrySet().stream().filter(entry -> SHA512.equals(entry.getValue())) // 过滤出SHA512值匹配的条目
				.map(entry -> serverSyncDirectory + FILE_SEPARATOR + entry.getKey()) // 提取文件路径
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
			} else LOGGER.log(Level.WARNING, "不支持的操作系统: " + os);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "打开命令行读取日志文件时出错: " + error.getMessage());
		}
	}
	private static void headlessSettingsHelp() {
		System.out.println("设置服务端HTTP端口: 'sp <端口号>'");
		System.out.println("设置服务端上传速率: 'sl <速率> <单位>'");
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
					while ((bytesRead = fileInputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
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
		SERVER_MAP.forEach((fileName, SHA512Value) -> { // 遍历同步文件列表
			if (SHA512Value == null) LOGGER.log(Level.WARNING, "未找到文件: " + fileName);
			else responseBuilder.append(fileName).append(LINE_SEPARATOR).append(SHA512Value).append(LINE_SEPARATOR);
		});
		sendHTTPResponse(exchange, responseBuilder.toString().getBytes(), HttpURLConnection.HTTP_OK);
		LOGGER.log(Level.INFO, "已发送列表");
	}
	// 检测端口号有效性的方法
	private static boolean isPort(String portInput) {
		if (isNumber(portInput) && Integer.parseInt(portInput) > 0 && Integer.parseInt(portInput) < 65536) return true;
		LOGGER.log(Level.WARNING, "端口号不在0~65535的范围内: " + portInput);
		return false; // 端口号不合法
	}
	private static boolean isNumber(String numberInput) {
		if (numberInput == null || numberInput.trim().isEmpty()) return false; // 空字符串返回false
		try {
			Long.parseLong(numberInput.trim()); // 尝试将文本转换为长整型
			return true; // 转换成功，返回true
		} catch (NumberFormatException error) {
			LOGGER.log(Level.WARNING, "不正确的数字格式: " + numberInput);
			return false; // 转换失败，返回false
		}
	}
	private static void configSaveButtonAction(JTextField serverPortField, JTextField serverUploadRateLimitField, JTextField serverSyncDirectoryPathField, JTextField clientPortField, JTextField clientAddressField, JTextField clientSyncDirectoryPathField, JTabbedPane tabbedPane, JCheckBox serverAutoStartBox, JCheckBox clientAutoStartBox, JComboBox<String> serverUploadRateLimitUnitBox, JDialog settingsDialog) {
		// 定义输入框数组及其对应的提示信息和选项卡索引
		Object[][] inputs = {{serverPortField, "服务端端口", 0}, {serverUploadRateLimitField, "上传速率限制", 0}, {serverSyncDirectoryPathField, "服务端同步文件夹路径", 0}, {clientPortField, "客户端端口", 1}, {clientAddressField, "服务器地址", 1}, {clientSyncDirectoryPathField, "客户端同步文件夹路径", 1}};
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
		if (!isNumber(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
			LOGGER.log(Level.WARNING, "上传速率上限不正确: " + uploadRateLimitText);
			tabbedPane.setSelectedIndex(0);
			serverUploadRateLimitField.selectAll(); // 选中输入框
			serverUploadRateLimitField.requestFocus(); // 聚焦输入框
			return;
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
	}
	private static void aboutButtonAction(JFrame frame, Dimension SCREEN_SIZE) {
		JScrollPane scrollPane = getJScrollPane("<html><body style=\"font-family: sans-serif;\">" + HEX_SYNC_NAME + "<br>By: ForgeStove<br>GitHub: <a href=\"" + GITHUB_URL + "\">" + GITHUB_URL + "</a>" + "</body></html>");
		scrollPane.setPreferredSize(new Dimension(SCREEN_SIZE.width / 5, SCREEN_SIZE.height / 15));
		JOptionPane.showMessageDialog(frame, scrollPane, "关于", JOptionPane.PLAIN_MESSAGE);
	}
	private static JScrollPane getJScrollPane(String htmlContent) {
		JEditorPane aboutEditorPane = new JEditorPane("text/html", htmlContent);
		aboutEditorPane.setEditable(false);
		// 处理链接点击事件
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
						serverUploadRateLimit = Long.parseLong(rateParts[0]);
						serverUploadRateLimitUnit = rateParts[1];
						LOGGER.log(Level.INFO, "服务端上传速率已设置为: " + serverUploadRateLimit + " " + serverUploadRateLimitUnit);
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
			if (SERVER_MAP.isEmpty()) {
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
		clientHTTPThread = null; // 清除线程引用
		LOGGER.log(Level.INFO, HEX_SYNC_NAME + "Client已关闭");
		if (clientAutoStart && !isErrorDownload) System.exit(0); // 自动退出
	}
	private static void startHTTPClient() {
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
	private static void populateFilesToDownloadMap(Map<String, String> requestMap, Map<String, String> filesToDownloadMap) {
		Map<String, Boolean> clientMap = new HashMap<>();
		for (File file : Objects.requireNonNull(new File(clientSyncDirectory).listFiles()))
			clientMap.put(calculateSHA512(file), true);
		for (Map.Entry<String, String> entry : requestMap.entrySet()) {
			String fileName = entry.getKey();
			String SHA512Value = entry.getValue();
			if (fileName.isEmpty() || SHA512Value.isEmpty()) return;
			boolean contentExists = clientMap.containsKey(SHA512Value);
			if (!contentExists && !checkFile(new File(clientSyncDirectory + FILE_SEPARATOR + fileName), fileName, requestMap))
				filesToDownloadMap.put(fileName, SHA512Value);
		}
	}
	// 下载所有文件
	private static void downloadHTTPFiles(Map<String, String> filesToDownloadMap) {
		if (filesToDownloadMap.isEmpty()) {
			LOGGER.log(Level.INFO, "没有需要下载的文件");
			return;
		}
		StringBuilder fileListBuilder = new StringBuilder("需要下载:" + LINE_SEPARATOR);
		logMap(filesToDownloadMap, fileListBuilder);
		int downloadedCount = 0;
		int filesToDownloadMapSize = filesToDownloadMap.size();
		for (Map.Entry<String, String> entry : filesToDownloadMap.entrySet()) {
			String fileName = entry.getKey(); // 文件名
			String filePath = clientSyncDirectory + FILE_SEPARATOR + fileName; // 设置下载路径
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
	private static void createUI() {
		Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
		// 设置窗口基本属性
		JFrame frame = new JFrame(HEX_SYNC_NAME + " 控制面板");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setVisible(!serverAutoStart);
		frame.setTitle(HEX_SYNC_NAME + "控制面板");
		frame.setSize(SCREEN_SIZE.width / 5, SCREEN_SIZE.height / 10);
		frame.setLocationRelativeTo(null); // 居中显示
		buttonPanel(frame);// 创建按钮面板
		icon(frame);
		if (serverAutoStart) startHTTPServer(); // 启动服务端
		if (clientAutoStart) startHTTPClient(); // 启动客户端
	}
	private static void buttonPanel(JFrame frame) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
		JPanel buttonPanel = new JPanel();
		JButton openLogButton = new JButton("日志");
		JButton settingsButton = new JButton("设置");
		JButton shutdownButton = new JButton("退出");
		JButton toggleServerButton = new JButton("启动服务端");
		JButton toggleClientButton = new JButton("启动客户端");
		// 设置按钮的首选大小
		Dimension buttonSize = new Dimension(SCREEN_SIZE.width / 17, SCREEN_SIZE.height / 40);
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
		actionListener(frame, openLogButton, settingsButton, shutdownButton, toggleServerButton, toggleClientButton);
		// 添加按钮到按钮面板
		buttonPanel.add(openLogButton);
		buttonPanel.add(settingsButton);
		buttonPanel.add(shutdownButton);
		buttonPanel.add(toggleServerButton);
		buttonPanel.add(toggleClientButton);
		panel.add(buttonPanel, BorderLayout.CENTER); // 添加按钮面板到主面板
		frame.add(panel);  // 添加主面板到窗口
	}
	// 按钮监听事件
	private static void actionListener(JFrame frame, JButton openLogButton, JButton settingsButton, JButton shutdownButton, JButton toggleServerButton, JButton toggleClientButton) {
		openLogButton.addActionListener(event -> openLog());
		toggleButtonAction(toggleServerButton, HexSync::startHTTPServer, HexSync::stopHTTPServer, "启动服务端", "停止服务端", frame);
		toggleButtonAction(toggleClientButton, HexSync::startHTTPClient, HexSync::stopHTTPClient, "启动客户端", "停止客户端", frame);
		settingsButton.addActionListener(event -> openSettingsDialog(frame)); // 打开设置对话框
		shutdownButton.addActionListener(event -> System.exit(0)); // 关闭程序
	}
	// 处理按钮事件的通用方法
	private static void toggleButtonAction(JButton button, Runnable startAction, Runnable stopAction, String startText, String stopText, JFrame frame) {
		button.addActionListener(event -> {
			button.setEnabled(false);
			if (button.getText().equals(startText)) startAction.run();
			else stopAction.run();
			button.setText(button.getText().equals(startText) ? stopText : startText);
			icon(frame);
			button.setEnabled(true);
		});
	}
	private static void systemTray(JFrame frame) {
		if (!SystemTray.isSupported()) return;
		TrayIcon trayIcon; // 托盘图标
		boolean running = serverHTTPThread != null || clientHTTPThread != null; // 状态标记
		trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource(running ? "IconO.png" : "IconI.png")), HEX_SYNC_NAME);
		trayIcon.setImageAutoSize(true); // 自动调整图标大小
		trayIcon.setToolTip(HEX_SYNC_NAME + "控制面板");
		trayIcon.setPopupMenu(getPopupMenu(frame));
		trayIcon.addActionListener(event -> frame.setVisible(true));
		try {
			SystemTray systemTray = SystemTray.getSystemTray();
			if (systemTray.getTrayIcons().length == 0) systemTray.add(trayIcon);
			else {
				TrayIcon existingIcon = systemTray.getTrayIcons()[0];
				existingIcon.setImage(Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource(running ? "IconO.png" : "IconI.png")));
			}
		} catch (AWTException error) {
			LOGGER.log(Level.SEVERE, "添加托盘图标失败: " + error.getMessage());
		}
	}
	// 切换图标
	private static void icon(JFrame frame) {
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource(serverHTTPThread != null || clientHTTPThread != null ? "IconO.png" : "IconI.png")));
		systemTray(frame);
	}
	private static PopupMenu getPopupMenu(JFrame frame) {
		PopupMenu popup = new PopupMenu();
		MenuItem openItem = new MenuItem("Open");
		MenuItem hideItem = new MenuItem("Hide");
		MenuItem settingsItem = new MenuItem("Settings");
		MenuItem exitItem = new MenuItem("Exit");
		openItem.addActionListener(event -> frame.setVisible(true));
		hideItem.addActionListener(event -> frame.setVisible(false));
		settingsItem.addActionListener(event -> openSettingsDialog(frame));
		exitItem.addActionListener(event -> System.exit(0));
		// 将菜单项添加到弹出菜单
		popup.add(openItem);
		popup.addSeparator();
		popup.add(hideItem);
		popup.addSeparator();
		popup.add(settingsItem);
		popup.addSeparator();
		popup.add(exitItem);
		return popup;
	}
	// 打开设置对话框
	private static void openSettingsDialog(JFrame frame) {
		loadConfig();
		Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
		int inputHeight = (int) (SCREEN_SIZE.height * 0.05);
		int inputWidth = (int) (SCREEN_SIZE.width * 0.2);
		JDialog settingsDialog = new JDialog(frame, "设置", true);
		// 创建选项卡面板
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		JPanel clientPanel = new JPanel(new GridLayout(5, 2));
		serverPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		clientPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
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
		JCheckBox serverAutoStartBox = new JCheckBox("自动启动服务端");
		JCheckBox clientAutoStartBox = new JCheckBox("自动启动客户端");
		serverAutoStartBox.setFocusPainted(false);
		clientAutoStartBox.setFocusPainted(false);
		// 设置复选框初始状态
		serverAutoStartBox.setSelected(serverAutoStart);
		clientAutoStartBox.setSelected(clientAutoStart);
		// 添加复选框到设置面板
		serverPanel.add(serverAutoStartBox);
		clientPanel.add(clientAutoStartBox);
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
		configSaveButton.addActionListener(event -> configSaveButtonAction(serverPortField, serverUploadRateLimitField, serverSyncDirectoryPathField, clientPortField, clientAddressField, clientSyncDirectoryPathField, tabbedPane, serverAutoStartBox, clientAutoStartBox, serverUploadRateLimitUnitBox, settingsDialog));
		cancelButton.addActionListener(event -> settingsDialog.dispose()); // 关闭对话框而不保存
		aboutButton.addActionListener(event -> aboutButtonAction(frame, SCREEN_SIZE));
		// 按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(configSaveButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(aboutButton);
		// 添加按钮面板到对话框的南部
		settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
		// 设置对话框的基本属性
		settingsDialog.setSize(SCREEN_SIZE.width / 4, SCREEN_SIZE.height / 5);
		settingsDialog.setLocationRelativeTo(frame); // 居中
		settingsDialog.setVisible(true); // 显示对话框
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