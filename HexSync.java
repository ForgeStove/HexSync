import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.swing.*;
import javax.swing.text.*;
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
public class HexSync extends JFrame {
	private static final Logger LOGGER = Logger.getLogger("HexSync"); // 日志记录器
	private static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize(); // 获取屏幕大小
	private static final Map<String, String> SYNC_MAP = new HashMap<>(); // 存储服务端文件名和对应的MD5数据
	private static final Map<String, String> REQUEST_MAP = new HashMap<>(); // 存储从服务端请求得到的文件名和对应的MD5数据
	private static final Map<String, String> FILES_TO_DOWNLOAD_MAP = new HashMap<>(); // 存储客户端需要下载的文件名和对应的MD5数据
	private static final String SEPARATOR = File.separator; // 文件分隔符
	private static final String lineSeparator = System.lineSeparator(); // 换行符
	private static final String HEX_SYNC_DIRECTORY = "HexSync"; // 文件夹目录
	private static final String LOG_FILE = HEX_SYNC_DIRECTORY + SEPARATOR + "HexSync.log"; // 日志文件路径
	private static final String CONFIG_FILE_PATH = HEX_SYNC_DIRECTORY + SEPARATOR + "HexSyncConfig.properties"; // 配置文件路径
	private static final String SERVER_AUTO_START_CONFIG = "serverAutoStart"; // 服务端自动启动配置项
	private static final String CLIENT_AUTO_START_CONFIG = "clientAutoStart"; // 客户端自动启动配置项
	private static final String CLIENT_PORT_CONFIG = "clientPort"; // 客户端端口配置项
	private static final String SERVER_PORT_CONFIG = "serverPort"; // 服务端端口配置项
	private static final String SERVER_ADDRESS_CONFIG = "serverAddress"; // 服务器地址配置项
	private static final String CLIENT_SYNC_DIRECTORY_CONFIG = "clientSyncDirectoryPath"; // 客户端同步文件夹路径配置项
	private static final String SERVER_SYNC_DIRECTORY_CONFIG = "serverSyncDirectoryPath"; // 服务端同步文件夹路径配置项
	private static final String UPLOAD_RATE_LIMIT_CONFIG = "uploadRateLimit"; // 上传速率限制配置项
	private static String clientSyncDirectory = "mods"; // 客户端同步文件夹目录
	private static String serverSyncDirectory = "mods"; // 服务端同步文件夹目录
	private static String uploadRateLimitUnit = "MB/s"; // 上传速率限制单位
	private static String serverAddress = "localhost"; // 服务器地址
	private static boolean isErrorDownload = false; // 客户端下载文件时是否发生错误
	private static boolean serverAutoStart = false; // 服务端自动启动
	private static boolean clientAutoStart = false; // 客户端自动启动
	private static boolean serverRunning; // 服务器运行状态
	private static boolean clientRunning; // 客户端运行状态
	private static int serverPort = 65535; // 端口号
	private static int clientPort = 65535; // 客户端端口号
	private static long uploadRateLimit = 0; // 上传速率限制值
	private static HttpServer httpServer; // 用于存储服务器实例
	private static HttpURLConnection httpURLConnection; // 用于存储HTTP连接实例
	private static Thread serverThread; // 服务器线程
	private static Thread clientThread; // 客户端线程
	private static JButton toggleServerButton;
	private static JButton toggleClientButton;
	private static SystemTray tray;
	private static TrayIcon trayIcon;
	public static void main(String[] args) {
		createDirectory(HEX_SYNC_DIRECTORY);
		initializeLogger();
		loadConfig();
		initializeUI();
	}
	// 初始化日志记录器
	private static void initializeLogger() {
		// 仅当日志文件不为空时才清空日志文件
		if (new File(LOG_FILE).length() > 0) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE))) {
				writer.write(""); // 清空内容
				LOGGER.log(Level.INFO, "已清空日志文件: " + LOG_FILE);
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "清空日志文件时出错: " + error.getMessage());
			}
		}
		// 创建日志文件处理器
		try {
			FileHandler fileHandler = new FileHandler(LOG_FILE, true); // 创建FileHandler,将日志输出到指定文件
			fileHandler.setFormatter(new SingleLineFormatter()); // 设置日志格式化器,使用自定义的单行格式化器
			LOGGER.addHandler(fileHandler); // 将FileHandler添加到日志记录器,以开始记录日志信息
			LOGGER.setLevel(Level.INFO);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "创建日志文件处理器时出错: " + error.getMessage());
		}
	}
	// 初始化UI
	private static void initializeUI() {
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
			HexSync HexSync = new HexSync();
			HexSync.createUI();
			HexSync.setVisible(!serverAutoStart);
		});
	}
	// 初始化文件
	private static void initializeFiles(boolean isServer) {
		if (isServer) createDirectory(serverSyncDirectory); // 创建服务端同步文件夹
		else createDirectory(clientSyncDirectory); // 创建客户端同步文件夹
		createDirectory(HEX_SYNC_DIRECTORY); // 在当前目录下创建HexSync文件夹
		loadConfig(); // 加载配置文件
		loadSyncFiles(); // 读取同步文件夹下的所有文件,并存储文件名和MD5值到syncFiles中
	}
	// 检测并创建文件夹
	private static void createDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.exists()) return;
		boolean isCreated = directory.mkdirs(); // 创建文件夹并保存结果
		LOGGER.log(isCreated ? Level.INFO : Level.SEVERE, (isCreated ? "文件夹已创建: " : "创建文件夹失败: ") + directory);
	}
	// 加载配置文件
	private static void loadConfig() {
		File configFile = new File(CONFIG_FILE_PATH);
		if (!configFile.exists()) return;
		try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
			String line; // 临时变量,存储一行
			while ((line = reader.readLine()) != null) {
				line = line.trim(); // 去除行首尾空格
				if (line.isEmpty() || line.startsWith("#")) continue; // 忽略空行和注释
				String[] parts = line.split("="); // 按等号分割
				if (parts.length != 2) {
					LOGGER.log(Level.WARNING, "配置文件中行格式不正确,跳过: " + line);
					continue; // 跳过格式错误的行
				}
				String head = parts[0].trim();
				String tail = parts[1].trim();
				switch (head) {
					case SERVER_AUTO_START_CONFIG:
						serverAutoStart = Boolean.parseBoolean(tail);
						break;
					case SERVER_PORT_CONFIG:
						serverPort = Integer.parseInt(tail);
						break;
					case UPLOAD_RATE_LIMIT_CONFIG:
						String[] limitParts = tail.split(" "); // 按空格分割以获取数值和单位
						if (limitParts.length != 2) LOGGER.log(Level.WARNING, "上传速率限制格式不正确,跳过: " + line);
						else {
							uploadRateLimit = Long.parseLong(limitParts[0]);
							uploadRateLimitUnit = limitParts[1];
						}
						break;
					case SERVER_SYNC_DIRECTORY_CONFIG:
						serverSyncDirectory = tail;
						break;
					case CLIENT_AUTO_START_CONFIG:
						clientAutoStart = Boolean.parseBoolean(tail);
						break;
					case CLIENT_PORT_CONFIG:
						clientPort = Integer.parseInt(tail);
						break;
					case SERVER_ADDRESS_CONFIG:
						serverAddress = tail;
						break;
					case CLIENT_SYNC_DIRECTORY_CONFIG:
						clientSyncDirectory = tail;
						break;
					default:
						LOGGER.log(Level.WARNING, "未知的配置项,跳过: " + head);
						break;
				}
			}
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "读取配置文件时出错: " + error.getMessage());
		}
	}
	// 读取目标目录下的所有文件,将文件名和MD5值存储到syncFiles中
	private static void loadSyncFiles() {
		SYNC_MAP.clear();
		File syncDirectory = new File(serverSyncDirectory);
		File[] files = syncDirectory.listFiles();
		if (files == null) return; // 提前返回,减少嵌套
		for (File file : files) {
			if (!file.isFile()) continue; // 跳过非文件项
			try {
				String MD5Value = calculateMD5(file);
				SYNC_MAP.put(file.getName(), MD5Value);
			} catch (Exception error) {
				LOGGER.log(Level.SEVERE, "计算MD5值时出错: " + error.getMessage());
			}
		}
	}
	// 地址格式化,转换为HTTP协议
	private static String addressFormat(String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		if (address.startsWith("https://")) return address.replace("https://", "http://"); // 转换为HTTP协议
		if (!address.startsWith("http://")) return "http://" + address; // 添加协议头
		return address; // 原样返回
	}
	// 从服务器请求文件名和MD5值列表
	private static boolean requestList() {
		String URL = addressFormat(serverAddress) + ":" + serverPort + "/list"; // 服务器地址
		LOGGER.log(Level.INFO, "正在连接到: " + URL); // 记录请求开始日志
		try {
			URL requestURL = new URL(URL); // 创建URL
			httpURLConnection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
			httpURLConnection.setRequestMethod("GET"); // 设置请求方式为GET
			LOGGER.log(Level.INFO, "发送请求中..."); // 记录请求发送日志
			int responseCode = httpURLConnection.getResponseCode(); // 获取响应码
			LOGGER.log(Level.INFO, "收到响应码: " + responseCode); // 记录响应码
			if (responseCode != HttpURLConnection.HTTP_OK) {
				if (clientRunning) LOGGER.log(Level.SEVERE, "请求文件列表失败,HTTP错误代码: " + responseCode); // 记录错误日志
				isErrorDownload = true;
				return false; // 返回,不存储任何数据
			}
		} catch (IOException error) {
			if (clientRunning) LOGGER.log(Level.SEVERE, "连接服务器时出错: " + error.getMessage());
			isErrorDownload = true;
			return false; // 返回,不存储任何数据
		} catch (Exception error) {
			if (clientRunning) LOGGER.log(Level.SEVERE, "请求文件列表时出错: " + error.getMessage());
			isErrorDownload = true;
			return false; // 返回,不存储任何数据
		}
		REQUEST_MAP.clear(); // 清空之前的内容
		try (BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()))) {
			String fileName; // 临时变量,用于存储文件名
			while ((fileName = in.readLine()) != null) { // 读取文件名
				LOGGER.log(Level.INFO, "接收到文件名: " + fileName); // 记录接收到的文件名
				String MD5Value = in.readLine(); // 读取对应的MD5值
				if (MD5Value != null) {
					LOGGER.log(Level.INFO, "接收到MD5值: " + MD5Value); // 记录接收到的MD5值
					REQUEST_MAP.put(fileName.trim(), MD5Value.trim()); // 将文件名与MD5值放入Map
				}
			}
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "读取响应时出错: " + error.getMessage());
			isErrorDownload = true;
		}
		if (REQUEST_MAP.isEmpty()) {
			LOGGER.log(Level.SEVERE, "请求文件列表为空"); // 记录请求失败日志
			isErrorDownload = true;
			return false;
		}
		// 记录整体文件列表
		StringBuilder fileListBuilder = new StringBuilder("接收到文件列表:\n");
		for (Map.Entry<String, String> entry : REQUEST_MAP.entrySet())
			fileListBuilder.append("文件名: ").append(entry.getKey()).append(", MD5值: ").append(entry.getValue()).append("\n");
		LOGGER.log(Level.INFO, fileListBuilder.toString());
		return true;
	}
	// 从服务器下载文件
	private static String downloadFile(String fileName, String filePath) {
		File clientFile = new File(filePath); // 目标本地文件
		try {
			if (checkFile(clientFile, fileName)) return "SKIP"; // 文件已存在且校验通过,跳过下载
			URL requestURL = new URL(addressFormat(serverAddress) + ":" + clientPort + "/" + REQUEST_MAP.get(fileName)); // 创建URL
			httpURLConnection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
			httpURLConnection.setRequestMethod("GET"); // 设置请求方式为GET
			// 检查HTTP响应码
			int responseCode = httpURLConnection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				LOGGER.log(Level.SEVERE, "下载失败,HTTP错误代码: " + responseCode);
				return "FAIL"; // 返回失败
			}
			// 下载成功,读取输入流并写入本地文件
			try (InputStream inputStream = httpURLConnection.getInputStream(); FileOutputStream outputStream = new FileOutputStream(filePath)) {
				byte[] buffer = new byte[8192]; // 8KB缓冲区
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead); // 写入文件
			}
			// 进行MD5校验
			return validateMD5(clientFile, fileName);
		} catch (Exception error) {
			LOGGER.log(Level.SEVERE, "连接异常: " + error.getMessage());
			return "FAIL"; // 返回失败
		}
	}
	// 检查文件是否已存在并进行MD5校验
	private static boolean checkFile(File localFile, String fileName) throws Exception {
		if (!localFile.exists()) return false; // 文件不存在,直接返回
		// 获取同步文件中的MD5值
		String serverMD5 = REQUEST_MAP.get(fileName); // 从服务端请求的文件列表中获取MD5值
		if (serverMD5 == null) {
			LOGGER.log(Level.WARNING, "缺少MD5,将重新下载: " + fileName);
			return false; // MD5值不存在,直接返回
		}
		// 计算本地文件的MD5值
		String clientMD5 = calculateMD5(localFile);
		if (!serverMD5.equals(clientMD5)) {
			LOGGER.log(Level.WARNING, "MD5校验失败,准备重新下载: " + fileName);
			return false; // MD5校验失败,直接返回
		}
		LOGGER.log(Level.INFO, "校验通过,跳过下载: " + localFile.getPath());
		return true; // MD5校验通过,返回成功
	}
	// 验证MD5校验
	private static String validateMD5(File localFile, String fileName) {
		String serverMD5 = FILES_TO_DOWNLOAD_MAP.get(fileName); // 从请求的文件列表中获取MD5值
		if (serverMD5 == null) {
			LOGGER.log(Level.SEVERE, "无法获取服务器的MD5值: " + fileName);
			return "FAIL"; // 无法获取MD5值,返回失败
		}
		String clientMD5;
		try {
			clientMD5 = calculateMD5(localFile);
		} catch (Exception error) {
			LOGGER.log(Level.SEVERE, "计算本地文件的MD5值时出错: " + error.getMessage());
			return "FAIL"; // 计算MD5值失败,返回失败
		}
		if (serverMD5.equals(clientMD5)) return "OK"; // 下载成功且MD5校验通过
		LOGGER.log(Level.SEVERE, "MD5校验失败,文件可能已损坏: " + fileName);
		if (!localFile.delete()) LOGGER.log(Level.SEVERE, "无法删除损坏的文件: " + localFile.getPath());
		return "FAIL"; // 返回失败,表明文件可能已损坏
	}
	// 计算文件的MD5校验码
	private static String calculateMD5(File file) throws Exception {
		MessageDigest MD5 = MessageDigest.getInstance("MD5"); // 获取MD5算法
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] byteBuffer = new byte[8192]; // 缓冲区大小
			int bytesRead; // 读取字节数
			while ((bytesRead = fileInputStream.read(byteBuffer)) != -1) MD5.update(byteBuffer, 0, bytesRead);
		}
		byte[] digest = MD5.digest(); // 获取MD5校验码
		StringBuilder stringBuilder = new StringBuilder();
		for (byte singleByte : digest) stringBuilder.append(String.format("%02x", singleByte)); // 转换为十六进制字符串
		return stringBuilder.toString(); // 返回MD5校验码
	}
	// 保存配置的方法
	private static void saveConfig() {
		File configFile = new File(CONFIG_FILE_PATH);
		StringBuilder configContent = new StringBuilder();
		// 使用常量数组保存配置参数及其值
		String[][] configEntries = {
				{SERVER_AUTO_START_CONFIG, String.valueOf(serverAutoStart)},
				{SERVER_PORT_CONFIG, String.valueOf(serverPort)},
				{UPLOAD_RATE_LIMIT_CONFIG, uploadRateLimit + " " + uploadRateLimitUnit},
				{SERVER_SYNC_DIRECTORY_CONFIG, serverSyncDirectory},
				{CLIENT_AUTO_START_CONFIG, String.valueOf(clientAutoStart)},
				{CLIENT_PORT_CONFIG, String.valueOf(clientPort)},
				{SERVER_ADDRESS_CONFIG, serverAddress},
				{CLIENT_SYNC_DIRECTORY_CONFIG, clientSyncDirectory}};
		// 构建配置内容
		for (String[] entry : configEntries)
			configContent.append(entry[0]).append("=").append(entry.length > 1 ? entry[1] : "").append(lineSeparator);
		// 写入配置文件
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
			writer.write(configContent.toString());
			LOGGER.log(Level.INFO, "保存配置成功: " + lineSeparator + configContent);
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "保存配置失败: " + error.getMessage(), error);
		}
	}
	// 发送响应
	private void sendResponse(HttpExchange exchange, byte[] responseBytes, int HTTPCode) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 设置Content-Type
		exchange.sendResponseHeaders(HTTPCode, responseBytes.length); // 设置响应头
		try (OutputStream outputStream = exchange.getResponseBody()) {
			long startTime = System.currentTimeMillis(); // 记录开始时间
			long uploadRateInBytes = convertToBytes(uploadRateLimit, uploadRateLimitUnit); // 转换为字节
			int responseLength = responseBytes.length;
			int totalBytesSent = 0; // 记录已发送字节数
			if (uploadRateLimit == 0) {
				outputStream.write(responseBytes);
				return; // 直接返回，不再执行后续代码
			}
			while (totalBytesSent < responseLength) {
				int bytesToSend = Math.min(16384, responseLength - totalBytesSent); // 每次最多发送16KB
				outputStream.write(responseBytes, totalBytesSent, bytesToSend); // 写入数据
				totalBytesSent += bytesToSend; // 更新已发送总字节数
				// 控制上传速率
				long elapsedTime = System.currentTimeMillis() - startTime; // 计算经过的时间
				long expectedBytesSent = (elapsedTime * uploadRateInBytes) / 1000; // 计算期望已发送字节数
				if (totalBytesSent > expectedBytesSent) {
					long sleepTime = (totalBytesSent - expectedBytesSent) * 1000 / uploadRateInBytes; // 计算需要等待的时间
					Thread.sleep(sleepTime); // 暂停
				}
			}
		} catch (InterruptedException error) {
			Thread.currentThread().interrupt(); // 重置中断状态
		}
	}
	// 切换服务器状态
	private void toggleServer() {
		toggleServerButton.setEnabled(false);
		if (serverRunning) stopServer();
		else startServer();
		toggleServerButton.setEnabled(true);
	}
	private void stopServer() {
		LOGGER.log(Level.INFO, "SyncServer正在关闭...");
		serverRunning = false;
		try {
			toggleServerButton.setEnabled(false);
			httpServer.stop(0); // 停止服务
		} finally {
			serverThread = null; // 清除线程引用
			LOGGER.log(Level.INFO, "HexSyncServer已关闭");
			toggleServerButton.setText("启动服务端");
			toggleServerButton.setEnabled(true);
			toggleIcon();
		}
	}
	private void startServer() {
		initializeFiles(true); // 初始化文件
		if (SYNC_MAP.isEmpty()) {
			LOGGER.log(Level.WARNING, "没有同步文件,无法启动服务器");
			return;
		}
		serverThread = new Thread(() -> {
			try {
				httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
				httpServer.createContext("/", new HttpRequestHandler());
				httpServer.setExecutor(null);
				httpServer.start();
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "服务器异常: " + error.getMessage());
			}
		});
		serverThread.start();
		serverRunning = true;
		LOGGER.log(Level.INFO, "HexSyncServer正在运行...端口号为: " + serverPort);
		toggleServerButton.setText("停止服务端");
		toggleIcon();
	}
	// 切换客户端状态
	private void toggleClient() {
		toggleClientButton.setEnabled(false);
		if (clientRunning) stopClient();
		else startClient();
		toggleClientButton.setEnabled(true);
	}
	private void stopClient() {
		clientRunning = false;
		try {
			toggleClientButton.setEnabled(false);
			httpURLConnection.disconnect();
			clientThread.stop(); // 停止线程
		} finally {
			clientThread = null;
			LOGGER.log(Level.INFO, "HexSyncClient已停止");
			toggleClientButton.setText("启动客户端");
			toggleClientButton.setEnabled(true);
			if (clientAutoStart && !isErrorDownload) System.exit(0); // 自动退出
			toggleIcon();
		}
	}
	private void startClient() {
		initializeFiles(false); // 初始化文件
		clientThread = new Thread(() -> {
			clientRunning = true;
			LOGGER.log(Level.INFO, "HexSyncClient正在运行...服务器地址为: " + serverAddress + " 端口号为: " + serverPort);
			toggleClientButton.setText("停止客户端");
			if (!requestList()) stopClient();
			LOGGER.log(Level.INFO, "获取到 " + REQUEST_MAP.size() + " 个文件");
			// 构建需要下载的文件列表
			for (Map.Entry<String, String> entry : REQUEST_MAP.entrySet()) {
				String fileName = entry.getKey(); // 文件名
				String MD5Value = entry.getValue(); // MD5值
				if (fileName.isEmpty() || MD5Value.isEmpty()) continue; // 忽略空行
				String clientPath = clientSyncDirectory + SEPARATOR + fileName; // 设置下载路径
				File clientFile = new File(clientPath); // 目标本地文件
				try {
					boolean fileCheckResult = checkFile(clientFile, fileName);
					if (fileCheckResult) continue; // 如果文件存在且校验通过，跳过下载
				} catch (Exception error) {
					LOGGER.log(Level.SEVERE, "检查文件时出错: " + error.getMessage());
					isErrorDownload = true; // 记录错误
				}
				FILES_TO_DOWNLOAD_MAP.put(fileName, MD5Value); // 添加到需要下载的文件列表
			}
			if (FILES_TO_DOWNLOAD_MAP.isEmpty()) {
				LOGGER.log(Level.INFO, "没有需要下载的文件");
				stopClient();
				return;
			}
			LOGGER.log(Level.INFO, "需要下载文件: " + FILES_TO_DOWNLOAD_MAP);
			int downloadedCount = 0;
			int filesToDownloadMapSize = FILES_TO_DOWNLOAD_MAP.size();
			for (Map.Entry<String, String> entry : FILES_TO_DOWNLOAD_MAP.entrySet()) {
				String fileName = entry.getKey(); // 文件名
				String filePath = clientSyncDirectory + SEPARATOR + fileName; // 设置下载路径
				switch (downloadFile(fileName, filePath)) {
					case "OK":
						downloadedCount++; // 成功下载时增加计数
						LOGGER.log(Level.INFO, "已下载文件: [" + downloadedCount + "/" + filesToDownloadMapSize + "] " + filePath);
						break;
					case "SKIP":
						LOGGER.log(Level.INFO, "跳过已有文件: " + filePath);
						break;
					default:
						LOGGER.log(Level.SEVERE, "下载文件失败: " + filePath);
						isErrorDownload = true; // 记录下载失败
						break;
				}
			}
			// 下载完成后的日志记录
			LOGGER.log(Level.INFO, "下载完成: [" + downloadedCount + "/" + filesToDownloadMapSize + "]");
			stopClient();
		});
		clientThread.start();
		toggleIcon();
	}
	// 创建用户界面
	private void createUI() {
		toggleIcon(); // 图标切换
		Font systemFont = UIManager.getFont("Label.font"); // 获取系统字体
		// 设置全局字体
		UIManager.put("Button.font", systemFont);
		UIManager.put("Label.font", systemFont);
		UIManager.put("TextArea.font", systemFont);
		UIManager.put("TextField.font", systemFont);
		// 设置窗口基本属性
		setTitle("HexSync控制面板");
		setSize(SCREEN_SIZE.width / 2, SCREEN_SIZE.height / 2);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setLocationRelativeTo(null); // 居中显示
		// 创建面板
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		// 创建日志区域
		JTextPane logTextPane = new JTextPane();
		logTextPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(logTextPane);
		panel.add(scrollPane, BorderLayout.CENTER);
		// 创建按钮面板
		JPanel buttonPanel = new JPanel();
		JButton openDirectoryButton = new JButton("目录");
		JButton openLogButton = new JButton("日志");
		JButton settingsButton = new JButton("设置");
		toggleServerButton = new JButton("启动服务端");
		toggleClientButton = new JButton("启动客户端");
		JButton shutdownButton = new JButton("退出");
		// 添加按钮事件
		openDirectoryButton.addActionListener(event -> {
			try {
				Desktop.getDesktop().open(new File(HEX_SYNC_DIRECTORY)); // 打开目录
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "打开目录时出错: " + error.getMessage());
			}
		});
		openLogButton.addActionListener(event -> {
			try {
				Desktop.getDesktop().open(new File(LOG_FILE)); // 打开日志文件
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "打开日志文件时出错: " + error.getMessage());
			}
		});
		toggleServerButton.addActionListener(event -> toggleServer()); // 服务端按钮事件
		toggleClientButton.addActionListener(event -> toggleClient()); // 客户端按钮事件
		settingsButton.addActionListener(event -> openSettingsDialog()); // 打开设置对话框
		shutdownButton.addActionListener(event -> System.exit(0)); // 关闭程序
		// 添加按钮到按钮面板
		buttonPanel.add(openDirectoryButton);
		buttonPanel.add(openLogButton);
		buttonPanel.add(settingsButton);
		buttonPanel.add(toggleServerButton);
		buttonPanel.add(toggleClientButton);
		buttonPanel.add(shutdownButton);
		panel.add(buttonPanel, BorderLayout.SOUTH); // 添加按钮面板到主面板
		add(panel);  // 添加主面板到窗口
		JTextPaneLogHandler logHandler = new JTextPaneLogHandler(logTextPane);
		logHandler.setFormatter(new SingleLineFormatter()); // 使用自定义格式化器
		LOGGER.addHandler(logHandler);
		// 添加窗口监听器
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent windowEvent) {
				if (SystemTray.isSupported()) setVisible(false); // 隐藏窗口而不是退出程序
				else System.exit(0);
			}
		});
		// 创建系统托盘
		if (SystemTray.isSupported() && tray == null) {
			tray = SystemTray.getSystemTray();
			trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource(serverRunning || clientRunning ? "IconO.png" : "IconI.png")), "HexSync");
			trayIcon.setImageAutoSize(true); // 自动调整图标大小
			trayIcon.setToolTip("HexSync控制面板");
			PopupMenu popup = getPopupMenu(); // 创建右键菜单
			trayIcon.setPopupMenu(popup);
			trayIcon.addActionListener(event -> setVisible(true));
			try {
				tray.add(trayIcon); // 添加托盘图标
			} catch (AWTException error) {
				LOGGER.log(Level.SEVERE, "添加托盘图标失败: " + error.getMessage());
			}
		}
		if (serverAutoStart) startServer(); // 启动服务端
		if (clientAutoStart) startClient(); // 启动客户端
	}
	// 切换图标
	private void toggleIcon() {
		Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(serverRunning || clientRunning ? "IconO.png" : "IconI.png"));
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
		final int inputHeight = (int) (SCREEN_SIZE.height * 0.1); // 输入框高度为屏幕高度的10%
		final int inputWidth = (int) (SCREEN_SIZE.width * 0.2); // 输入框宽度为屏幕宽度的20%
		int borderPadding = (int) (inputHeight * 0.05); // 边距为输入框高度的5%
		JDialog settingsDialog = new JDialog(this, "设置", true);
		settingsDialog.setLayout(new BorderLayout(borderPadding, borderPadding));
		// 创建选项卡面板
		JTabbedPane tabbedPane = new JTabbedPane();
		// 服务端设置选项卡
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		serverPanel.setBorder(BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding));
		JTextField serverPortField = new JTextField(String.valueOf(serverPort));
		JTextField uploadRateLimitField = new JTextField(String.valueOf(uploadRateLimit));
		JTextField serverSyncDirectoryPathField = new JTextField(serverSyncDirectory);
		// 创建选择单位的下拉框
		String[] uploadRateUnits = new String[]{"B/s", "KB/s", "MB/s", "GB/s"};
		JComboBox<String> uploadRateLimitUnitBox = new JComboBox<>(uploadRateUnits);
		uploadRateLimitUnitBox.setSelectedItem(uploadRateLimitUnit);
		// 添加标签和文本框到设置面板
		serverPanel.add(new JLabel("端口号: "));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel("上传速率限制(0为无限制): "));
		// 添加上传速率限制输入框与单位选择框
		JPanel rateLimitPanel = new JPanel(new GridLayout(1, 2, 5, 0));
		rateLimitPanel.add(uploadRateLimitField);
		rateLimitPanel.add(uploadRateLimitUnitBox); // 将单位选择框添加到输入框旁边
		serverPanel.add(rateLimitPanel);
		serverPanel.add(new JLabel("服务端同步文件夹路径: "));
		serverPanel.add(serverSyncDirectoryPathField);
		// 设置输入框大小
		serverPortField.setSize(new Dimension(inputWidth, inputHeight));
		serverSyncDirectoryPathField.setSize(new Dimension(inputWidth, inputHeight));
		// 设置上传速率限制输入框和单位选择框大小
		uploadRateLimitField.setSize(new Dimension(inputWidth * 3 / 4, inputHeight));
		uploadRateLimitUnitBox.setSize(new Dimension(inputWidth / 4, inputHeight));
		// 客户端设置选项卡
		JPanel clientPanel = new JPanel(new GridLayout(5, 2));
		clientPanel.setBorder(BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding));
		// 创建文本框
		JTextField clientPortField = new JTextField(String.valueOf(clientPort));
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
				LOGGER.log(Level.SEVERE, "打开关于页面时出错: " + error.getMessage());
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
			Object[][] inputs = {{serverPortField, "服务端端口", 0}, // 服务器端设置的索引
					{uploadRateLimitField, "上传速率限制", 0}, {serverSyncDirectoryPathField, "服务端同步文件夹路径", 0}, {clientPortField, "客户端端口", 1}, // 客户端设置的索引
					{clientAddressField, "服务器地址", 1}, {clientSyncDirectoryPathField, "客户端同步文件夹路径", 1}};
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
			String uploadRateLimitText = uploadRateLimitField.getText().trim();
			if (isNotNumber(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
				LOGGER.log(Level.WARNING, "上传速率上限不正确: " + uploadRateLimitText);
				tabbedPane.setSelectedIndex(0);
				uploadRateLimitField.selectAll(); // 选中输入框
				uploadRateLimitField.requestFocus(); // 聚焦输入框
				return; // 上传速率上限不合法
			}
			serverAutoStart = serverAutoStartBox.isSelected();
			clientAutoStart = clientAutoStartBox.isSelected();
			serverPort = Integer.parseInt(serverPortField.getText().trim());
			uploadRateLimit = Integer.parseInt(uploadRateLimitText);
			uploadRateLimitUnit = (String) uploadRateLimitUnitBox.getSelectedItem();
			serverSyncDirectory = serverSyncDirectoryPathField.getText().trim();
			clientPort = Integer.parseInt(clientPortField.getText().trim());
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
		if (invalid) {
			LOGGER.log(Level.WARNING, "端口号不在有效范围内: " + portText);
			portField.selectAll(); // 选中输入框
			portField.requestFocus(); // 聚焦输入框
			return true; // 端口号不合法
		}
		return false; // 端口号合法
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
	// 单位转换方法
	private long convertToBytes(long value, String unit) {
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
	// 日志格式化
	private static class SingleLineFormatter extends SimpleFormatter {
		@Override
		public String format(LogRecord record) {
			// 格式化为 "等级 - 时间 - 消息"
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
	// 日志颜色处理
	private static class JTextPaneLogHandler extends StreamHandler {
		private final StyledDocument doc;
		private final SingleLineFormatter formatter;
		private final JTextPane textPane;
		JTextPaneLogHandler(JTextPane textPane) {
			this.textPane = textPane;
			this.doc = new DefaultStyledDocument();
			this.textPane.setStyledDocument(doc);
			this.formatter = new SingleLineFormatter();
		}
		@Override
		public void publish(LogRecord record) {
			Map<String, Color> logLevelColors = new HashMap<>();
			logLevelColors.put("INFO", Color.BLACK);
			logLevelColors.put("WARNING", Color.BLUE);
			logLevelColors.put("SEVERE", Color.RED);
			String logLevel = record.getLevel().getName();
			Color foregroundColor = logLevelColors.getOrDefault(logLevel, Color.GRAY);
			String message = formatter.format(record);
			SimpleAttributeSet attrs = new SimpleAttributeSet();
			StyleConstants.setForeground(attrs, foregroundColor);
			SwingUtilities.invokeLater(() -> {
				try {
					doc.insertString(doc.getLength(), message, attrs);
					textPane.setCaretPosition(doc.getLength()); // 滚动到最新日志
				} catch (BadLocationException error) {
					LOGGER.log(Level.SEVERE, "写入日志时出错: " + error.getMessage(), error);
				}
			});
		}
	}
	// HTTP请求处理器
	private class HttpRequestHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) {
			try {
				String requestMethod = exchange.getRequestMethod(); // 获取请求方法
				if ("GET".equalsIgnoreCase(requestMethod)) {
					String requestURI = exchange.getRequestURI().getPath(); // 获取请求URI
					LOGGER.log(Level.INFO, "客户端请求: " + requestURI); // 记录客户端请求信息
					processRequest(requestURI, exchange); // 处理请求
				} else exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "处理请求时出错: " + error.getMessage(), error);
			}
		}
		// 处理请求
		private void processRequest(String requestURI, HttpExchange exchange) throws IOException {
			// 解析requestURI,查看是文件请求还是文件列表请求
			if ("/list".equals(requestURI)) sendList(exchange);
			else {
				String requestedMD5 = requestURI.substring(1);
				// 检查请求的 MD5 内容是否在 SYNC_MAP 中
				if (!SYNC_MAP.containsValue(requestedMD5)) {
					String response = "未找到对应的文件";
					byte[] responseBytes = response.getBytes(); // 转换为字节数组
					sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_NOT_FOUND); // 发送响应
					return;
				}
				// 查找对应 MD5 值的文件
				String filePath = null;
				for (Map.Entry<String, String> entry : SYNC_MAP.entrySet()) {
					String fileName = entry.getKey();
					String MD5Value = entry.getValue();
					// 检查 MD5 文件内容
					if (requestedMD5.equals(MD5Value))
						filePath = serverSyncDirectory + SEPARATOR + fileName; // 找到并返回文件路径
				}
				if (filePath != null) {
					File file = new File(filePath);
					if (file.exists() && file.isFile()) {
						byte[] responseBytes = Files.readAllBytes(file.toPath()); // 读取文件内容
						sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_OK); // 发送响应
					} else {
						String response = "文件不存在: " + filePath;
						byte[] responseBytes = response.getBytes(); // 转换为字节数组
						sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_NOT_FOUND); // 发送响应
					}
					LOGGER.log(Level.INFO, "已发送文件: " + filePath);
				} else {
					String response = "未找到对应的文件";
					byte[] responseBytes = response.getBytes(); // 转换为字节数组
					sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_NOT_FOUND); // 发送响应
				}
			}
		}
		// 发送文件名和MD5值列表
		private void sendList(HttpExchange exchange) throws IOException {
			StringBuilder responseBuilder = new StringBuilder(); // 用于构建响应内容
			for (Map.Entry<String, String> entry : SYNC_MAP.entrySet()) { // 遍历同步文件列表
				String fileName = entry.getKey(); // 获取文件名
				String MD5Value = entry.getValue(); // 获取MD5值
				if (MD5Value != null)
					responseBuilder.append(fileName).append(lineSeparator).append(MD5Value).append(lineSeparator);
				else LOGGER.log(Level.WARNING, "文件: " + fileName + " 未找到对应的MD5值");
			}
			String response = responseBuilder.toString(); // 转换为字符串
			byte[] responseBytes = response.getBytes(); // 转换为字节数组
			sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_OK); // 发送响应
			LOGGER.log(Level.INFO, "已发送文件列表");
		}
	}
}
