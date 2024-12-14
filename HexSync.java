import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
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

import static java.util.logging.Level.*;
public class HexSync implements HttpHandler {
	private static final String HEX_SYNC_NAME = HexSync.class.getName(); // 程序名称
	private static final Logger LOGGER = Logger.getLogger(HEX_SYNC_NAME); // 日志记录器
	private static final AtomicLong AVAILABLE_TOKENS = new AtomicLong(0); // 当前可用令牌数量
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
	private static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync"; // 项目GitHub地址
	private static final String ICON_I = "iconI.png";
	private static final String ICON_O = "iconO.png";
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
	private static boolean headless = GraphicsEnvironment.isHeadless();
	private static final Dimension SCREEN_SIZE = headless ? null : Toolkit.getDefaultToolkit().getScreenSize();
	private static int serverHTTPPort = 65535;// HTTP 端口，默认值65535
	private static int clientHTTPPort = 65535; // 客户端 HTTP 端口，默认值65535
	private static long serverUploadRateLimit = 1; // 上传速率限制值，默认限速1MB/s
	public static void main(String[] args) {
		initializeLogger();
		loadConfig();
		createDirectory(serverSyncDirectory);
		createDirectory(clientSyncDirectory);
		createDirectory(clientOnlyDirectory);
		initializeUI(args);
	}
	private static void log(Level level, String message) {
		LOGGER.log(level, message);
	}
	// 初始化日志记录器
	private static void initializeLogger() {
		new Thread(() -> {
			try {
				createDirectory(HEX_SYNC_DIRECTORY);
				FileHandler fileHandler = new FileHandler(LOG_FILE, false);
				fileHandler.setFormatter(new SimpleFormatter());
				LOGGER.addHandler(fileHandler);
			} catch (Exception error) {
				log(SEVERE, "初始化日志时出错: " + error.getMessage());
			}
		}).start();
	}
	// 初始化UI
	private static void initializeUI(String[] args) {
		if (serverAutoStart) startHTTPServer();
		if (clientAutoStart) startHTTPClient();
		headless = headless || Arrays.asList(args).contains("-headless");
		if (headless) headlessUI(); // 无头模式
		else try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(HexSync::createUI);// 有头模式
		} catch (Exception error) {
			log(SEVERE, "设置外观失败: " + error.getMessage());
		}
	}
	// 初始化文件
	private static void initializeFiles(boolean isServer) {
		createDirectory(isServer ? serverSyncDirectory : clientSyncDirectory); // 创建同步文件夹
		createDirectory(HEX_SYNC_DIRECTORY); // 在当前目录下创建HexSync文件夹
		loadConfig(); // 加载配置文件
		if (isServer) serverMap = initializeMap(serverSyncDirectory, serverMap); // 初始化服务端文件列表
		else createDirectory(clientOnlyDirectory); // 创建仅客户端文件夹
		log(INFO, isServer ? "服务端初始化完成" : "客户端初始化完成");
	}
	private static Map<String, String> initializeMap(String directory, Map<String, String> map) {
		map.clear(); // 清空同步文件列表
		File[] fileList = new File(directory).listFiles(); // 获取同步文件夹下的所有文件
		if (fileList == null) return null;
		for (File file : fileList)
			if (file.isFile()) {
				String SHAValue = calculateSHA(file);
				map.put(file.getName(), SHAValue);
			}
		return map;
	}
	// 检测并创建文件夹
	private static void createDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.isDirectory()) return;
		if (directory.mkdirs()) log(INFO, "文件夹已创建: " + directoryPath);
		else log(SEVERE, "无法创建文件夹: " + directoryPath);
	}
	// 加载配置文件
	private static void loadConfig() {
		File configFile = new File(CONFIG_FILE_PATH);
		if (!configFile.isFile()) return;
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) processConfigLine(line.trim()); // 处理每一行配置
		} catch (IOException error) {
			log(SEVERE, "配置读取失败: " + error.getMessage());
		}
	}
	// 处理单行配置
	private static void processConfigLine(String line) {
		if (line.isEmpty() || line.startsWith("#")) return;
		String[] parts = line.split("=");
		if (parts.length != 2) {
			log(WARNING, "配置格式不正确: " + line);
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
			default:
				log(WARNING, "不正确的配置项: " + head);
				break;
		}
	}
	// 解析上传速率限制
	private static void parseRateLimit(String tail, String originalLine) {
		String[] limitParts = tail.split(" ");
		if (limitParts.length != 2) log(WARNING, "上传速率限制格式不正确,跳过: " + originalLine);
		else {
			serverUploadRateLimit = Long.parseLong(limitParts[0]);
			serverUploadRateLimitUnit = limitParts[1];
		}
	}
	// 地址格式化,转换为HTTP协议
	private static String formatHTTP(String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("https://") ? address.replace(
				"https://", "http://"
		) : "http://" + address;
	}
	// 计算文件校验码
	private static String calculateSHA(File file) {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] byteBuffer = new byte[16384];
			int bytesRead;
			MessageDigest SHA = MessageDigest.getInstance("SHA-256");
			while ((bytesRead = fileInputStream.read(byteBuffer)) != -1) SHA.update(byteBuffer, 0, bytesRead);
			StringBuilder stringBuilder = new StringBuilder();
			for (byte singleByte : SHA.digest()) stringBuilder.append(String.format("%02x", singleByte));
			return stringBuilder.toString();
		} catch (Exception error) {
			log(SEVERE, "计算校验码时出错: " + error.getMessage());
			return null;
		}
	}
	// 从服务器请求文件名和SHA值列表
	private static Map<String, String> requestHTTPList() {
		String URL = formatHTTP(serverAddress) + ":" + clientHTTPPort + "/list"; // 服务器地址
		log(INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, String> requestMap = new HashMap<>(); // 复制请求列表
		try {
			URL requestURL = new URL(URL); // 创建URL
			int responseCode = getResponseCode(requestURL);
			if (responseCode != HttpURLConnection.HTTP_OK) {
				if (clientHTTPThread != null)
					log(SEVERE, "请求文件列表失败,HTTP错误代码: " + responseCode); // 记录错误日志
				isErrorDownload = true;
				return requestMap;
			}
		} catch (IOException error) {
			if (clientHTTPThread != null) log(SEVERE, "连接服务器时出错: " + error.getMessage());
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
			log(SEVERE, "读取响应时出错: " + error.getMessage());
			isErrorDownload = true;
		}
		log(INFO, "请求文件列表成功,共收到 [" + requestMap.size() + "] 个文件"); // 记录请求成功日志
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
					formatHTTP(serverAddress) + ":" + clientHTTPPort + "/download/" + requestSHA
			));
			if (responseCode != HttpURLConnection.HTTP_OK) {
				log(SEVERE, "下载失败,HTTP错误代码: " + responseCode);
				return false;
			}
		} catch (IOException error) {
			log(SEVERE, "连接服务器时出错: " + error.getMessage());
			return false;
		}
		// 读取输入流并写入本地文件
		try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
			byte[] buffer = new byte[16384]; // 缓冲区
			int bytesRead;
			while ((bytesRead = HTTPURLConnection.getInputStream().read(buffer)) != -1)
				outputStream.write(buffer, 0, bytesRead);
		} catch (IOException error) {
			log(SEVERE, "读取响应时出错: " + error.getMessage());
		}
		// 进行SHA校验
		if (requestSHA == null) {
			log(SEVERE, "无法获取请求的校验码: " + fileName);
			return false;
		}
		String clientSHA = calculateSHA(clientFile);
		if (requestSHA.equals(clientSHA)) return true; // 下载成功且校验通过
		log(SEVERE, "校验失败,文件可能已损坏: " + fileName);
		if (!clientFile.delete()) log(SEVERE, "无法删除损坏的文件: " + clientFile.getPath());
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
			log(INFO, "配置已保存: " + LINE_SEPARATOR + configContent);
		} catch (IOException error) {
			log(SEVERE, "配置保存失败: " + error.getMessage());
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
					AVAILABLE_TOKENS.addAndGet((currentTime - lastFillTime) * maxUploadRateInBytes / 1000);
					lastFillTime = currentTime; // 更新时间
					// 尝试发送数据
					int bytesToSend = Math.min(16384, responseBytesLength - totalBytesSent);
					if (AVAILABLE_TOKENS.get() >= bytesToSend) {
						outputStream.write(responseBytes, totalBytesSent, bytesToSend); // 写入数据
						totalBytesSent += bytesToSend; // 更新已发送字节数
						AVAILABLE_TOKENS.addAndGet(-bytesToSend); // 减少可用令牌
					} else {
						long requiredTokens = bytesToSend - AVAILABLE_TOKENS.get();
						long sleepTime = (requiredTokens * 1000) / maxUploadRateInBytes;
						Thread.sleep(sleepTime);
					}
				}
			} catch (Exception error) {
				log(SEVERE, "发送响应时出错: " + error.getMessage());
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
					log(WARNING, "未知的上传速率单位: " + unit);
					return 0;
			}
		} catch (ArithmeticException error) {
			return Long.MAX_VALUE; // 溢出，返回最大值
		}
	}
	// 检查文件是否已存在并进行SHA校验
	private static boolean checkNoFile(File file, String fileName, Map<String, String> map) {
		if (!file.exists()) return true;
		return !map.get(fileName).equals(calculateSHA(file));
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
			} else log(WARNING, "不支持的操作系统: " + os);
		} catch (IOException error) {
			log(SEVERE, "打开命令行读取日志文件时出错: " + error.getMessage());
		}
	}
	private static void headlessSettingsHelp() {
		println("设置服务端HTTP端口: 'sp <端口号>'\n"
				+ "设置服务端上传速率: 'sl <速率> <'B/s'/'KB/s'/'MB/s'/'GB/s'>'\n"
				+ "设置服务端同步目录: 'sd <目录>'\n"
				+ "设置服务端自动启动: 'ss <y/n>'\n"
				+ "设置客户端HTTP端口: 'cp <端口号>'\n"
				+ "设置服务器地址: 'sa <地址>'\n"
				+ "设置客户端同步目录: 'cd <目录>'\n"
				+ "设置客户端自动启动: 'cs <y/n>'\n"
				+ "仓库地址: 'github'\n"
				+ "保存并退出: 'save'\n"
				+ "帮助: 'help'");
	}
	private static void println(String message) {
		System.out.println(message);
	}
	// 处理请求
	private static void processHTTPRequest(String requestURI, HttpExchange exchange) {
		log(INFO, "收到请求: " + requestURI);
		byte[] responseBytes = "".getBytes();
		int HTTPCode = HttpURLConnection.HTTP_NOT_FOUND;
		if ("/list".equals(requestURI)) {
			StringBuilder responseBuilder = new StringBuilder(); // 用于构建响应内容
			serverMap.forEach((fileName, SHA) ->
					responseBuilder.append(fileName).append(LINE_SEPARATOR).append(SHA).append(LINE_SEPARATOR));
			responseBytes = responseBuilder.toString().getBytes();
			HTTPCode = HttpURLConnection.HTTP_OK;
		} else if (requestURI.startsWith("/download/")) {
			String requestSHA = requestURI.substring(requestURI.lastIndexOf("/") + 1);
			String filePath = serverMap.entrySet().stream()
					.filter(entry -> requestSHA.equals(entry.getValue()))
					.map(entry -> serverSyncDirectory + FILE_SEPARATOR + entry.getKey())
					.findFirst()
					.orElse(null);
			if (filePath == null) {
				log(SEVERE, "无法找到对应的文件: " + requestSHA);
				sendHTTPResponse(exchange, responseBytes, HTTPCode);
				return;
			}
			File file = new File(filePath); // 构造文件对象
			if (!serverMap.containsValue(requestSHA) || !file.exists() || !file.isFile()) {
				log(SEVERE, "文件不存在或请求的校验码不正确: " + filePath);
				sendHTTPResponse(exchange, responseBytes, HttpURLConnection.HTTP_NOT_FOUND);
				return;
			}
			try (InputStream inputStream = Files.newInputStream(file.toPath())) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[16384];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) byteArrayOutputStream.write(buffer, 0, bytesRead);
				responseBytes = byteArrayOutputStream.toByteArray();
			} catch (IOException error) {
				log(SEVERE, "读取文件时发生错误: " + error.getMessage());
			}
			try {
				String encodedName = URLEncoder.encode(file.getName(), "UTF-8").replace("+", "%20");
				exchange.getResponseHeaders().set(
						"Content-Disposition", "attachment; filename=\""
								+ encodedName
								+ "\"; filename*=UTF-8''"
								+ encodedName
				);
			} catch (UnsupportedEncodingException error) {
				log(SEVERE, "编码文件名时出错: " + error.getMessage());
			}
			HTTPCode = HttpURLConnection.HTTP_OK; // 发送成功,返回200
			log(INFO, "发送文件: " + filePath);
		} else log(WARNING, "未知的请求: " + requestURI);
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
		log(WARNING, "端口号不在0~65535的范围内: " + portInput);
		return false; // 端口号不合法
	}
	private static boolean isNumberInLong(String numberInput) {
		if (numberInput == null || numberInput.trim().isEmpty()) return false; // 空字符串返回false
		try {
			Long.parseLong(numberInput.trim()); // 尝试将文本转换为长整型
			return true; // 转换成功，返回true
		} catch (NumberFormatException error) {
			log(WARNING, "不正确的数字格式或超出范围: " + numberInput);
			return false; // 转换失败，返回false
		}
	}
	private static void aboutButtonAction() {
		JScrollPane scrollPane = getJScrollPane(
				"<html><body>"
						+ HEX_SYNC_NAME
						+ "<br>By: ForgeStove"
						+ "<br>GitHub仓库地址: " + "<a href=\"" + GITHUB_URL + "\">" + GITHUB_URL + "</a>"
						+ "</body></html>"
		);
		scrollPane.setPreferredSize(new Dimension(SCREEN_SIZE.width / 5, SCREEN_SIZE.height / 15));
		JDialog aboutDialog = createDialog("关于");
		aboutDialog.getContentPane().add(scrollPane);
		aboutDialog.pack();
	}
	private static JScrollPane getJScrollPane(String htmlContent) {
		JEditorPane aboutEditorPane = new JEditorPane("text/html", htmlContent);
		aboutEditorPane.setEditable(false);
		aboutEditorPane.addHyperlinkListener(hyperlinkEvent -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(hyperlinkEvent.getEventType())) {
				try {
					Desktop.getDesktop().browse(hyperlinkEvent.getURL().toURI());
				} catch (Exception error) {
					log(SEVERE, "打开链接时出错: " + error.getMessage());
				}
			}
		});
		return new JScrollPane(aboutEditorPane);
	}
	private static void headlessUI() {
		println("欢迎使用" + HEX_SYNC_NAME + "!");
		println("输入 'help' 以获取帮助.");
		Scanner scanner = new Scanner(System.in);
		String command;
		while (true) {
			System.out.print(HEX_SYNC_NAME + ">");
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
					println("启动服务端: 'rs'");
					println("启动客户端: 'rc'");
					println("停止服务端: 'ss'");
					println("停止客户端: 'sc'");
					println("设置: 'set'");
					println("退出: 'exit'");
					println("帮助: 'help'");
					break;
				case "exit":
					System.exit(0);
					break;
				default:
					println("无效命令,输入 'help' 以获取帮助.");
			}
		}
	}
	private static void headlessSettings() {
		headlessSettingsHelp();
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print(HEX_SYNC_NAME + "Settings>");
			String input = scanner.nextLine();
			String[] parts = input.split("\\s+");
			String command = parts[0];
			// 检查是否有参数
			if (parts.length < 2 && !command.equals("save") && !command.equals("help") && !command.equals("github")) {
				println("无效命令或缺少参数, 输入 'help' 以获取帮助.");
				continue;
			}
			switch (command) {
				case "sp":
					String portInput = parts[1];
					if (isPort(portInput)) {
						serverHTTPPort = Integer.parseInt(portInput);
						log(INFO, "服务端HTTP端口已设置为: " + serverHTTPPort);
					}
					continue;
				case "sl":
					String rateInput = input.substring(command.length()).trim();
					if (rateInput.matches("\\d+(\\s+B/s|\\s+KB/s|\\s+MB/s|\\s+GB/s)")) {
						String[] rateParts = rateInput.split("\\s+");
						if (!isNumberInLong(rateParts[0])) {
							println("无效输入,请输入数字.");
							continue;
						}
						serverUploadRateLimit = Long.parseLong(rateParts[0]);
						serverUploadRateLimitUnit = rateParts[1];
						log(INFO,
								"服务端上传速率已设置为: " + serverUploadRateLimit + " " + serverUploadRateLimitUnit
						);
					} else println("无效输入,请输入数字及单位.");
					continue;
				case "sd":
					String syncDirInput = parts[1];
					if (!syncDirInput.isEmpty() && !syncDirInput.contains(FILE_SEPARATOR)) {
						serverSyncDirectory = syncDirInput;
						log(INFO, "服务端同步目录已设置为: " + serverSyncDirectory);
					} else println("同步目录格式错误,请输入绝对路径或相对路径.");
					continue;
				case "ss":
					String autoStartInput = parts[1];
					if (autoStartInput.matches("[yYnN]")) {
						serverAutoStart = autoStartInput.matches("[yY]");
						log(INFO, "服务端自动启动已设置为: " + serverAutoStart);
					} else println("无效输入,请输入'y'/'Y'或'n'/'N'.");
					continue;
				case "cp":
					String clientPortInput = parts[1];
					if (isPort(clientPortInput)) {
						clientHTTPPort = Integer.parseInt(clientPortInput);
						log(INFO, "客户端HTTP端口已设置为: " + clientHTTPPort);
					}
					continue;
				case "sa":
					String addressInput = parts[1];
					if (addressInput.matches("\\d+\\.\\d+")) {
						serverAddress = addressInput;
						log(INFO, "服务端地址已设置为: " + serverAddress);
					} else println("无效输入,请输入IP地址.");
					continue;
				case "cd":
					String clientSyncDirInput = parts[1];
					if (!clientSyncDirInput.isEmpty() && !clientSyncDirInput.contains(FILE_SEPARATOR)) {
						clientSyncDirectory = clientSyncDirInput;
						log(INFO, "客户端同步目录已设置为: " + clientSyncDirectory);
					} else println("同步目录格式错误,请输入绝对路径或相对路径.");
					continue;
				case "cs":
					String clientAutoStartInput = parts[1];
					if (clientAutoStartInput.matches("[yYnN]")) {
						clientAutoStart = clientAutoStartInput.matches("[yY]");
						log(INFO, "客户端自动启动已设置为: " + clientAutoStart);
					} else println("无效输入,请输入'y'/'Y'或'n'/'N'.");
					continue;
				case "save":
					saveConfig();
					return;
				case "help":
					headlessSettingsHelp();
					continue;
				case "github":
					println(GITHUB_URL);
					continue;
				default:
					println("无效命令,输入 'help' 以获取帮助.");
			}
		}
	}
	private static void stopHTTPServer() {
		if (HTTPServer == null) return;
		log(INFO, HEX_SYNC_NAME + "Server正在关闭...");
		HTTPServer.stop(0); // 停止服务
		serverHTTPThread = null; // 清除线程引用
		log(INFO, HEX_SYNC_NAME + "Server已关闭");
	}
	private static void startHTTPServer() {
		serverHTTPThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Server正在启动...");
			initializeFiles(true);
			if (serverMap.isEmpty()) {
				log(WARNING, serverSyncDirectory + "无文件,无法启动服务器");
				stopHTTPServer();
				return;
			}
			try {
				HTTPServer = HttpServer.create(new InetSocketAddress(serverHTTPPort), 0);
				HTTPServer.createContext("/", new HexSync());
				HTTPServer.setExecutor(null);
				HTTPServer.start();
			} catch (IOException error) {
				log(SEVERE, "服务器异常: " + error.getMessage());
			}
			log(INFO, HEX_SYNC_NAME + "Server正在运行...端口号为: " + serverHTTPPort);
		});
		serverHTTPThread.start();
	}
	private static void stopHTTPClient() {
		if (HTTPURLConnection != null) HTTPURLConnection.disconnect();
		if (clientHTTPThread != null) {
			clientHTTPThread = null; // 清除线程引用
			log(INFO, HEX_SYNC_NAME + "Client已关闭");
		}
		if (clientAutoStart && !isErrorDownload) System.exit(0); // 自动退出
	}
	private static void startHTTPClient() {
		clientHTTPThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Client正在启动...");
			initializeFiles(false);
			Map<String, String> requestMap = requestHTTPList();
			isErrorDownload = requestMap.isEmpty();
			if (isErrorDownload) return;
			else {
				log(INFO, "获取到 " + requestMap.size() + " 个文件");
				Map<String, String> toDownloadMap = new HashMap<>(); // 用于存储需要下载的文件列表
				Map<String, String> clientOnlyMap = new HashMap<>(); // 用于存储客户端仅同步的文件列表
				clientOnlyMap = initializeMap(clientOnlyDirectory, clientOnlyMap); // 初始化客户端仅同步的文件列表
				deleteFilesNotInMap(requestMap, clientOnlyMap);
				Map<String, String> clientMap = new HashMap<>(); // 用于存储客户端文件列表
				clientMap = initializeMap(clientSyncDirectory, clientMap); // 初始化客户端文件列表
				createToDownloadMap(requestMap, toDownloadMap, clientMap); // 构建需要下载的文件列表
				downloadFilesInMap(toDownloadMap); // 下载文件
				copyAllFiles(clientOnlyDirectory, clientSyncDirectory);
			}
			stopHTTPClient();
		});
		clientHTTPThread.start();
	}
	private static void deleteFilesNotInMap(Map<String, String> requestMap, Map<String, String> clientOnlyMap) {
		File[] fileList = new File(clientSyncDirectory).listFiles();
		if (fileList != null) for (File file : fileList) {
			String fileName = file.getName();
			if (!file.isFile() || requestMap.containsKey(fileName) || clientOnlyMap.containsKey(fileName)) continue;
			if (file.delete()) log(INFO, "已删除文件: " + fileName);
			else log(SEVERE, "删除文件失败: " + fileName);
		}
	}
	private static void copyAllFiles(String source, String target) {
		File targetDirectory = new File(target);
		createDirectory(String.valueOf(targetDirectory));
		File[] fileList = new File(source).listFiles();
		if (fileList != null) for (File file : fileList) {
			File targetFile = new File(targetDirectory, file.getName());
			// 递归复制子目录
			if (file.isDirectory()) copyAllFiles(file.getAbsolutePath(), targetFile.getAbsolutePath());
			else {
				if (targetFile.exists()) continue;
				try (InputStream inputStream = Files.newInputStream(file.toPath());
					 OutputStream outputStream = Files.newOutputStream(targetFile.toPath())) {
					byte[] buffer = new byte[16384];
					int length;
					while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
					log(INFO, "已复制文件: " + file.getName() + " 到 " + targetFile.getAbsolutePath());
				} catch (IOException error) {
					log(SEVERE, "复制文件失败: " + file.getName());
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
			log(INFO, "已是最新,无需下载.");
			if (headless || isErrorDownload) return;
			JDialog dialog = createDialog("已是最新,无需下载.");
			dialog.setSize(SCREEN_SIZE.width / 5, 0);
			return;
		}
		log(INFO, "开始下载 " + toDownloadMap.size() + " 个文件");
		int downloadedCount = 0;
		int toDownloadMapSize = toDownloadMap.size();
		JDialog progressDialog = null;
		if (!headless) progressDialog = createProgressDialog(toDownloadMapSize);
		for (Map.Entry<String, String> entry : toDownloadMap.entrySet()) {
			String filePath = clientSyncDirectory + FILE_SEPARATOR + entry.getKey(); // 设置下载路径
			if (downloadFile(filePath, toDownloadMap)) {
				downloadedCount++; // 成功下载时增加计数
				log(INFO, "已下载: [" + downloadedCount + "/" + toDownloadMapSize + "] " + filePath);
				if (!headless && progressDialog != null) updateProgressDialog(progressDialog, downloadedCount);
			} else {
				log(SEVERE, "下载失败: " + filePath);
				isErrorDownload = true; // 记录下载失败
			}
		}
		if (progressDialog != null) progressDialog.dispose();
		if (!headless) {
			JDialog dialog = createDialog(isErrorDownload ? "下载失败,请检查网络连接."
					: "下载完成: [" + downloadedCount + "/" + toDownloadMapSize + "]");
			dialog.setSize(SCREEN_SIZE.width / 5, 0);
			if (clientAutoStart) System.exit(0); // 自动退出
		}
		log(INFO, "下载完成: [" + downloadedCount + "/" + toDownloadMapSize + "]");
	}
	private static JDialog createDialog(String title) {
		JDialog dialog = new JDialog();
		toggleIcon(dialog);
		dialog.setTitle(title);
		dialog.setVisible(true);
		dialog.setResizable(false);
		dialog.setAlwaysOnTop(true);
		dialog.setLocationRelativeTo(null);
		dialog.setSize(SCREEN_SIZE.width / 5, SCREEN_SIZE.height / 15);
		return dialog;
	}
	// 创建进度条对话框
	private static JDialog createProgressDialog(int totalFiles) {
		JDialog dialog = createDialog("下载进度");
		SwingUtilities.invokeLater(() -> {
			JProgressBar progressBar = new JProgressBar(0, totalFiles);
			progressBar.setStringPainted(true);
			progressBar.setForeground(Color.getColor("#008080"));
			progressBar.setBackground(Color.getColor("#D3D3D3"));
			progressBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			progressBar.setFont(UIManager.getFont("Label.font").deriveFont(18f));
			dialog.add(progressBar, BorderLayout.CENTER);
		});
		return dialog;
	}
	// 更新进度对话框
	private static void updateProgressDialog(JDialog dialog, int completedFiles) {
		SwingUtilities.invokeLater(() -> {
			JProgressBar progressBar = (JProgressBar) dialog.getContentPane().getComponent(0);
			progressBar.setValue(completedFiles);
		});
	}
	private static void createUI() {
		JDialog dialog = createDialog(HEX_SYNC_NAME + " 控制面板");
		dialog.setSize(SCREEN_SIZE.width / 5, SCREEN_SIZE.height / 10);
		buttonPanel(dialog); // 添加按钮面板
		toggleIcon(dialog);
	}
	private static void buttonPanel(Dialog dialog) {
		JButton openLogButton = newJButton("打开日志", event -> openLog());
		JButton settingsButton = newJButton("设置", event -> openSettingsDialog());
		JButton shutdownButton = newJButton("退出", event -> System.exit(0));
		JButton toggleServerButton = newJButton("启动服务端", null);
		JButton toggleClientButton = newJButton("启动客户端", null);
		toggleButtonAction(toggleServerButton, HexSync::startHTTPServer, HexSync::stopHTTPServer,
				"启动服务端", "停止服务端", dialog);
		toggleButtonAction(toggleClientButton, HexSync::startHTTPClient, HexSync::stopHTTPClient,
				"启动客户端", "停止客户端", dialog);
		// 添加按钮到按钮面板
		JPanel buttonPanel = new JPanel();
		Dimension buttonSize = new Dimension(SCREEN_SIZE.width / 17, SCREEN_SIZE.height / 40);
		buttonPanel.add(setButton(openLogButton, buttonSize));
		buttonPanel.add(setButton(settingsButton, buttonSize));
		buttonPanel.add(setButton(shutdownButton, buttonSize));
		buttonPanel.add(setButton(toggleServerButton, buttonSize));
		buttonPanel.add(setButton(toggleClientButton, buttonSize));
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(buttonPanel, BorderLayout.CENTER); // 添加按钮面板到主面板
		dialog.add(panel);  // 添加主面板到窗口
	}
	private static JButton setButton(JButton button, Dimension buttonSize) {
		button.setPreferredSize(buttonSize);
		button.setFocusPainted(false);
		return button;
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
			toggleIcon(dialog);
			button.setEnabled(true);
		});
	}
	// 切换图标
	private static void toggleIcon(Dialog dialog) {
		boolean running = serverHTTPThread != null || clientHTTPThread != null; // 状态标记
		dialog.setIconImage(getImage(running));
		if (!SystemTray.isSupported()) return;
		TrayIcon trayIcon; // 托盘图标
		trayIcon = new TrayIcon(getImage(running), HEX_SYNC_NAME);
		trayIcon.setImageAutoSize(true); // 自动调整图标大小
		trayIcon.setToolTip(HEX_SYNC_NAME + "控制面板");
		trayIcon.setPopupMenu(popupMenu(dialog));
		trayIcon.addActionListener(event -> dialog.setVisible(true));
		try {
			SystemTray systemTray = SystemTray.getSystemTray();
			if (systemTray.getTrayIcons().length == 0) systemTray.add(trayIcon);
			else {
				TrayIcon existingIcon = systemTray.getTrayIcons()[0];
				existingIcon.setImage(getImage(running));
			}
		} catch (AWTException error) {
			log(SEVERE, "添加托盘图标失败: " + error.getMessage());
		}
	}
	private static Image getImage(boolean running) {
		return Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource(running ? ICON_O : ICON_I));
	}
	// 右键菜单
	private static PopupMenu popupMenu(Dialog dialog) {
		PopupMenu popupMenu = new PopupMenu();
		popupMenu.add(createMenuItem("Open", event -> dialog.setVisible(true)));
		popupMenu.add(createMenuItem("Hide", event -> dialog.setVisible(false)));
		popupMenu.add(createMenuItem("Settings", event -> openSettingsDialog()));
		popupMenu.add(createMenuItem("About", event -> aboutButtonAction()));
		popupMenu.add(createMenuItem("Exit", event -> System.exit(0)));
		return popupMenu;
	}
	// 辅助方法创建菜单项
	private static MenuItem createMenuItem(String text, ActionListener actionListener) {
		MenuItem menuItem = new MenuItem(text);
		menuItem.addActionListener(actionListener);
		return menuItem;
	}
	// 打开设置对话框
	private static void openSettingsDialog() {
		loadConfig();
		JDialog settingsDialog = createDialog("设置");
		// 服务端选项卡
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		String[][] serverLabelsAndFields = {
				{"端口号: ", String.valueOf(serverHTTPPort)},
				{"上传速率限制(0为无限制): ", String.valueOf(serverUploadRateLimit)},
				{"服务端同步文件夹路径: ", serverSyncDirectory}
		};
		JTextField[] serverTextFields = new JTextField[3];
		String[] rateUnits = {"B/s", "KB/s", "MB/s", "GB/s"};
		JComboBox<String> serverUploadRateLimitUnitBox = new JComboBox<>(rateUnits);
		serverUploadRateLimitUnitBox.setSelectedItem(serverUploadRateLimitUnit);
		int index = 0;
		for (String[] labelAndField : serverLabelsAndFields) {
			serverPanel.add(new JLabel(labelAndField[0]));
			JTextField textField = new JTextField(labelAndField[1]);
			serverTextFields[index] = textField;
			if (labelAndField[0].startsWith("上传速率限制(0为无限制): ")) {
				JPanel rateLimitPanel = new JPanel(new GridLayout(1, 2, 5, 0));
				rateLimitPanel.add(textField);
				rateLimitPanel.add(serverUploadRateLimitUnitBox);
				serverPanel.add(rateLimitPanel);
			} else serverPanel.add(textField);
			index++;
		}
		JTextField serverPortField = serverTextFields[0];
		JTextField serverUploadRateLimitField = serverTextFields[1];
		JTextField serverSyncDirectoryPathField = serverTextFields[2];
		// 客户端选项卡
		JPanel clientPanel = new JPanel(new GridLayout(5, 2));
		JTextField[] clientTextFields = new JTextField[4];
		String[][] clientLabelsAndFields = {
				{"端口号: ", String.valueOf(clientHTTPPort)},
				{"服务器地址: ", serverAddress},
				{"客户端同步文件夹路径: ", clientSyncDirectory},
				{"仅客户端模组文件夹路径: ", clientOnlyDirectory}
		};
		for (String[] labelAndField : clientLabelsAndFields) {
			clientPanel.add(new JLabel(labelAndField[0]));
			JTextField textField = new JTextField(labelAndField[1]);
			clientTextFields[Arrays.asList(clientLabelsAndFields).indexOf(labelAndField)] = textField;
			clientPanel.add(textField);
		}
		JTextField clientPortField = clientTextFields[0];
		JTextField serverAddressField = clientTextFields[1];
		JTextField clientSyncDirectoryPathField = clientTextFields[2];
		JTextField clientOnlyDirectoryPathField = clientTextFields[3];
		// 添加选项卡到选项卡面板
		JCheckBox serverAutoStartBox = newJCheckBox("自动启动服务端", serverAutoStart, serverPanel);
		JCheckBox clientAutoStartBox = newJCheckBox("自动启动客户端", clientAutoStart, clientPanel);
		// 添加选项卡面板到设置对话框
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("服务端设置", serverPanel);
		tabbedPane.addTab("客户端设置", clientPanel);
		tabbedPane.setFocusable(false);
		settingsDialog.add(tabbedPane, BorderLayout.CENTER);
		JButton configSaveButton = newJButton("保存", event -> {
			// 定义输入框数组及其对应的提示信息和选项卡索引
			Object[][] inputs = {
					{serverPortField, "服务端端口", 0},
					{serverUploadRateLimitField, "上传速率限制", 0},
					{serverSyncDirectoryPathField, "服务端同步文件夹路径", 0},
					{clientPortField, "客户端端口", 1},
					{serverAddressField, "服务器地址", 1},
					{clientSyncDirectoryPathField, "客户端同步文件夹路径", 1},
					{clientOnlyDirectoryPathField, "仅客户端模组文件夹路径", 1},
			};
			// 检查输入框是否为空
			for (Object[] input : inputs) {
				JTextField textField = (JTextField) input[0];
				String fieldName = (String) input[1];
				int tabIndex = (int) input[2]; // 获取对应的索引
				if (textField.getText().trim().isEmpty()) {
					tabbedPane.setSelectedIndex(tabIndex); // 跳转到对应的选项卡
					selectAndFocus(textField);
					log(WARNING, fieldName + "不能为空");
					return;
				}
			}
			// 检测端口号是否是数字且在合法范围内
			if (!isPort(serverPortField.getText().trim())) selectAndFocus(serverPortField);
			if (!isPort(clientPortField.getText().trim())) selectAndFocus(clientPortField);
			// 检测上传速率上限
			String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
			if (!isNumberInLong(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
				log(WARNING, "上传速率上限不正确: " + uploadRateLimitText);
				tabbedPane.setSelectedIndex(0);
				selectAndFocus(serverUploadRateLimitField);
				return;
			}
			serverAutoStart = serverAutoStartBox.isSelected();
			clientAutoStart = clientAutoStartBox.isSelected();
			serverHTTPPort = Integer.parseInt(serverPortField.getText().trim());
			serverUploadRateLimit = Long.parseLong(uploadRateLimitText);
			serverUploadRateLimitUnit = (String) serverUploadRateLimitUnitBox.getSelectedItem();
			serverSyncDirectory = serverSyncDirectoryPathField.getText().trim();
			clientHTTPPort = Integer.parseInt(clientPortField.getText().trim());
			serverAddress = serverAddressField.getText().trim();
			clientSyncDirectory = clientSyncDirectoryPathField.getText().trim();
			clientOnlyDirectory = clientOnlyDirectoryPathField.getText().trim();
			saveConfig(); // 保存配置
			settingsDialog.dispose(); // 关闭对话框
		});
		JButton cancelButton = newJButton("取消", event -> settingsDialog.dispose());
		JButton aboutButton = newJButton("关于", event -> aboutButtonAction());
		// 按钮面板
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(configSaveButton);
		buttonPanel.add(cancelButton);
		buttonPanel.add(aboutButton);
		// 添加按钮面板到对话框的南部
		settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
		settingsDialog.pack(); // 自动调整大小
	}
	private static void selectAndFocus(JTextField textField) {
		textField.selectAll(); // 选中输入框
		textField.requestFocus(); // 聚焦输入框
	}
	private static JCheckBox newJCheckBox(String text, boolean serverAutoStart, JPanel serverPanel) {
		JCheckBox serverAutoStartBox = new JCheckBox(text);
		serverAutoStartBox.setFocusPainted(false);
		serverAutoStartBox.setSelected(serverAutoStart);
		serverPanel.add(serverAutoStartBox);
		return serverAutoStartBox;
	}
	private static JButton newJButton(String text, ActionListener actionListener) {
		JButton button = new JButton(text);
		button.setFocusPainted(false);
		button.addActionListener(actionListener);
		return button;
	}
	@Override
	public void handle(HttpExchange exchange) {
		String requestMethod = exchange.getRequestMethod();
		if (!requestMethod.equalsIgnoreCase("GET")) return;
		processHTTPRequest(exchange.getRequestURI().getPath(), exchange);
	}
}