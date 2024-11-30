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
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.*;
public class HexSync extends JFrame {
	private static final Logger LOGGER = Logger.getLogger("SyncServer"); // 日志记录器
	private static final String SERVER_ADDRESS = "http://localhost"; // 默认服务器地址
	private static final String CONFIG_FILE_NAME = "HexSyncConfig.properties"; // 配置文件名
	private static final String LOG_FILE_NAME = "HexSync.log";
	private static final String SEPARATOR = File.separator; // 文件分隔符
	private static final String CURRENT_DIRECTORY = "."; // 相对路径根目录
	private static final String HEX_SYNC_DIRECTORY = CURRENT_DIRECTORY + SEPARATOR + "HexSync"; // 文件夹目录
	private static final String LOG_FILE = HEX_SYNC_DIRECTORY + SEPARATOR + LOG_FILE_NAME; // 日志文件路径
	private static final String CONFIG_FILE_PATH = HEX_SYNC_DIRECTORY + SEPARATOR + CONFIG_FILE_NAME; // 配置文件路径
	private static final String CLIENT_SYNC_DIRECTORY_NAME = "mods"; // 默认客户端同步文件夹名称
	private static final String CLIENT_SYNC_DIRECTORY = CURRENT_DIRECTORY + SEPARATOR + CLIENT_SYNC_DIRECTORY_NAME; // 默认客户端同步文件夹目录
	private static String clientSyncDirectory = CLIENT_SYNC_DIRECTORY; // 客户端同步文件夹目录
	private static final String SERVER_SYNC_DIRECTORY_NAME = "mods"; // 默认服务端同步文件夹名称
	private static final String SERVER_SYNC_DIRECTORY = CURRENT_DIRECTORY + SEPARATOR + SERVER_SYNC_DIRECTORY_NAME; // 默认服务端同步文件夹目录
	private static String serverSyncDirectory = SERVER_SYNC_DIRECTORY; // 服务端同步文件夹目录
	private static final String CLIENT_PORT_CONFIG = "clientPort"; // 客户端端口配置项
	private static final String SERVER_PORT_CONFIG = "serverPort"; // 服务端端口配置项
	private static final String SERVER_ADDRESS_CONFIG = "serverAddress"; // 服务器地址配置项
	private static final String CLIENT_SYNC_DIRECTORY_NAME_CONFIG = "clientSyncDirectoryName"; // 客户端同步文件夹名称配置项
	private static final String SERVER_SYNC_DIRECTORY_NAME_CONFIG = "serverSyncDirectoryName"; // 服务端同步文件夹名称配置项
	private static final String CLIENT_SYNC_DIRECTORY_PATH_CONFIG = "clientSyncDirectoryPath"; // 客户端同步文件夹路径配置项
	private static final String SERVER_SYNC_DIRECTORY_PATH_CONFIG = "serverSyncDirectoryPath"; // 服务端同步文件夹路径配置项
	private static final String UPLOAD_RATE_LIMIT_CONFIG = "uploadRateLimit"; // 上传速率限制配置项
	private static final Map<String, String> SYNC_MAP = new HashMap<>(); // 存储服务端文件名和对应的MD5数据
	private static final Map<String, String> REQUEST_MAP = new HashMap<>(); // 存储从服务端请求得到的文件名和对应的MD5数据
	private static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize(); // 获取屏幕大小
	private static final int PORT = 65535; // 默认端口
	private static final String UPLOAD_RATE_LIMIT_UNIT = "MB/s"; // 默认单位
	private static final long UPLOAD_RATE_LIMIT = 16; // 默认16MB/s
	private static final AtomicLong bytesSentThisSecond = new AtomicLong(0); // 当前秒已发送字节数
	private static String uploadRateLimitUnit = UPLOAD_RATE_LIMIT_UNIT; // 上传速率限制单位
	private static long uploadRateLimit = UPLOAD_RATE_LIMIT; // 上传速率限制值
	private static long lastTimestamp = System.currentTimeMillis(); // 上一次更新时间
	private static String clientSyncDirectoryName = CLIENT_SYNC_DIRECTORY_NAME; // 客户端同步文件夹名称
	private static String serverSyncDirectoryName = SERVER_SYNC_DIRECTORY_NAME; // 服务端同步文件夹名称
	private static int serverPort = PORT; // 端口号
	private static int clientPort = PORT; // 客户端端口号
	private static String serverAddress = SERVER_ADDRESS; // 服务器地址
	private static HttpServer server; // 用于存储服务器实例
	private static HttpURLConnection connection; // 用于存储HTTP连接实例
	private static Thread serverThread; // 服务器线程
	private static Thread clientThread; // 客户端线程
	private static boolean serverRunning; // 服务器运行状态
	private static boolean clientRunning; // 客户端运行状态
	private static JButton toggleServerButton;
	private static JButton toggleClientButton;
	private static SystemTray tray;
	private static TrayIcon trayIcon;
	public static void main(String[] args) {
		createDirectory(HEX_SYNC_DIRECTORY); // 在当前目录下创建HexSync文件夹
		initializeLogger();
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
			HexSync server = new HexSync();
			server.createUI();
			server.setVisible(true);
		});
	}
	// 初始化文件
	private static void initializeFiles(boolean isServer) {
		if (isServer) createDirectory(serverSyncDirectory); // 创建服务端同步文件夹
		else createDirectory(clientSyncDirectory); // 创建客户端同步文件夹
		createDirectory(HEX_SYNC_DIRECTORY); // 在当前目录下创建HexSync文件夹
		loadConfig(isServer); // 加载配置文件
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
	private static void loadConfig(boolean isServer) {
		File configFile = new File(CONFIG_FILE_PATH);
		if (!configFile.exists()) return;
		try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
			String line; // 临时变量,存储一行
			while ((line = reader.readLine()) != null) {
				line = line.trim();// 去除行首尾空格
				if (line.isEmpty() || line.startsWith("#")) continue; // 忽略空行和注释
				String[] parts = line.split("="); // 按等号分割
				if (parts.length != 2) {
					LOGGER.log(Level.WARNING, "配置文件中行格式不正确,跳过: " + line);
					continue; // 跳过格式错误的行
				}
				String head = parts[0].trim(); // 临时变量,存储头部
				String tail = parts[1].trim(); // 临时变量,存储尾部
				if (isServer) {
					if (head.equals(SERVER_PORT_CONFIG)) serverPort = Integer.parseInt(tail);
					if (head.equals(UPLOAD_RATE_LIMIT_CONFIG)) {
						String[] limitParts = tail.split(" "); // 按空格分割以获取数值和单位
						if (limitParts.length != 2) {
							LOGGER.log(Level.WARNING, "上传速率限制格式不正确,跳过: " + line);
							continue; // 格式不正确则跳过
						}
						uploadRateLimit = Long.parseLong(limitParts[0]); // 数值部分
						uploadRateLimitUnit = limitParts[1]; // 单位部分转换为大写
					}
					if (head.equals(SERVER_SYNC_DIRECTORY_NAME_CONFIG)) serverSyncDirectoryName = tail;
					if (head.equals(SERVER_SYNC_DIRECTORY_PATH_CONFIG)) serverSyncDirectory = tail;
				} else {
					if (head.equals(CLIENT_PORT_CONFIG)) clientPort = Integer.parseInt(tail);
					if (head.equals(SERVER_ADDRESS_CONFIG)) serverAddress = tail;
					if (head.equals(CLIENT_SYNC_DIRECTORY_NAME_CONFIG)) clientSyncDirectoryName = tail;
					if (head.equals(CLIENT_SYNC_DIRECTORY_PATH_CONFIG)) clientSyncDirectory = tail;
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
	// 从服务器请求文件名和MD5值列表
	private static void requestList() {
		LOGGER.log(Level.INFO, "开始请求列表,URL: " + serverAddress + ":" + clientPort + "/list"); // 记录请求开始日志
		try {
			URL requestURL = new URL(serverAddress + ":" + clientPort + "/list"); // 创建URL
			connection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
			connection.setRequestMethod("GET"); // 设置请求方式为GET
			LOGGER.log(Level.INFO, "发送请求到服务器..."); // 记录请求发送日志
			// 检查响应码
			int responseCode = connection.getResponseCode(); // 获取响应码
			LOGGER.log(Level.INFO, "收到响应,HTTP状态码: " + responseCode); // 记录响应码
			if (responseCode != HttpURLConnection.HTTP_OK) {
				if (clientRunning) LOGGER.log(Level.SEVERE, "请求文件列表失败,HTTP错误代码: " + responseCode); // 记录错误日志
				return; // 返回,不存储任何数据
			}
		} catch (IOException error) {
			if (clientRunning) LOGGER.log(Level.SEVERE, "连接服务器时出错: " + error.getMessage());
			return; // 返回,不存储任何数据
		} catch (Exception error) {
			if (clientRunning) LOGGER.log(Level.SEVERE, "请求文件列表时出错: " + error.getMessage());
			return; // 返回,不存储任何数据
		}
		REQUEST_MAP.clear(); // 清空之前的内容
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String fileName; // 临时变量,用于存储文件名
			while ((fileName = in.readLine()) != null) { // 读取文件名
				if (!clientRunning) return; // 客户端已停止运行,退出循环
				LOGGER.log(Level.FINE, "接收到文件名: " + fileName); // 记录接收到的文件名
				String MD5Value = in.readLine(); // 读取对应的MD5值
				if (MD5Value != null) {
					LOGGER.log(Level.FINE, "接收到MD5值: " + MD5Value); // 记录接收到的MD5值
					REQUEST_MAP.put(fileName.trim(), MD5Value.trim()); // 将文件名与MD5值放入Map
				}
			}
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "读取响应时出错: " + error.getMessage());
		}
		LOGGER.log(Level.INFO, "接收到文件列表: " + REQUEST_MAP); // 记录整体文件列表
	}
	// 从服务器下载文件
	private static String downloadFile(String fileName, String filePath) {
		File clientFile = new File(filePath); // 目标本地文件
		try {
			if (checkFile(clientFile, fileName)) return "SKIP"; // 文件已存在且校验通过,跳过下载
			// 开始下载文件
			URL requestURL = new URL(serverAddress + ":" + clientPort + "/" + REQUEST_MAP.get(fileName)); // 创建URL
			connection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
			connection.setRequestMethod("GET"); // 设置请求方式为GET
			// 检查HTTP响应码
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				LOGGER.log(Level.SEVERE, "下载失败,HTTP错误代码: " + responseCode);
				return "FAIL"; // 返回失败
			}
			// 下载成功,读取输入流并写入本地文件
			try (InputStream inputStream = connection.getInputStream(); FileOutputStream outputStream = new FileOutputStream(filePath)) {
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
		if (!localFile.exists()) {
			LOGGER.log(Level.INFO, "目标文件不存在,将开始下载: " + localFile);
			return false; // 文件不存在,直接返回
		}
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
		String serverMD5 = REQUEST_MAP.get(fileName); // 从请求的文件列表中获取MD5值
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
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] byteBuffer = new byte[8192]; // 缓冲区大小
			int bytesRead; // 读取字节数
			while ((bytesRead = fis.read(byteBuffer)) != -1) MD5.update(byteBuffer, 0, bytesRead); // 更新MD5算法
		}
		byte[] digest = MD5.digest(); // 获取MD5校验码
		StringBuilder stringBuilder = new StringBuilder();
		for (byte singleByte : digest) stringBuilder.append(String.format("%02x", singleByte)); // 转换为十六进制字符串
		return stringBuilder.toString(); // 返回MD5校验码
	}
	// 保存配置的方法
	private static void saveConfig() {
		// 保存服务端的配置
		File configFile = new File(CONFIG_FILE_PATH);
		String[] configContent = new String[10];
		configContent[0] = "# HexSync服务端配置";
		configContent[1] = SERVER_PORT_CONFIG + "=" + serverPort;
		configContent[2] = UPLOAD_RATE_LIMIT_CONFIG + "=" + uploadRateLimit + " " + uploadRateLimitUnit;
		configContent[3] = SERVER_SYNC_DIRECTORY_NAME_CONFIG + "=" + serverSyncDirectoryName;
		configContent[4] = SERVER_SYNC_DIRECTORY_PATH_CONFIG + "=" + serverSyncDirectory;
		configContent[5] = "# HexSync客户端配置";
		configContent[6] = CLIENT_PORT_CONFIG + "=" + clientPort;
		configContent[7] = SERVER_ADDRESS_CONFIG + "=" + serverAddress;
		configContent[8] = CLIENT_SYNC_DIRECTORY_NAME_CONFIG + "=" + clientSyncDirectoryName;
		configContent[9] = CLIENT_SYNC_DIRECTORY_PATH_CONFIG + "=" + clientSyncDirectory;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
			for (String line : configContent) writer.write(line + "\n"); // 写入配置内容
			LOGGER.log(Level.INFO, "写入配置: \n" + Arrays.toString(configContent));
		} catch (IOException error) {
			LOGGER.log(Level.SEVERE, "保存配置时出错: " + error.getMessage());
		}
	}
	// 发送响应
	private void sendResponse(HttpExchange exchange, byte[] responseBytes, int HTTPCode) throws IOException {
		exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 设置Content-Type
		exchange.sendResponseHeaders(HTTPCode, responseBytes.length); // 设置响应头
		try (OutputStream outputStream = exchange.getResponseBody()) {
			int totalBytesSent = 0;
			int responseLength = responseBytes.length;
			while (totalBytesSent < responseLength) {
				int bytesToSend = Math.min(1024, responseLength - totalBytesSent); // 每次最多发送1KB
				limit(bytesToSend); // 调用limit方法，限制发送速率
				outputStream.write(responseBytes, totalBytesSent, bytesToSend); // 写入数据
				totalBytesSent += bytesToSend; // 更新已发送总字节数
			}
		} catch (InterruptedException error) {
			Thread.currentThread().interrupt(); // 恢复中断状态
			LOGGER.log(Level.SEVERE, "发送响应时被中断: " + error.getMessage());
		}
	}
	// 切换服务器状态
	private void toggleServer() {
		toggleServerButton.setEnabled(false);
		if (serverRunning || server != null) stopServer();
		else startServer();
		toggleIcon(); // 图标切换
		toggleServerButton.setEnabled(true);
	}
	private void stopServer() {
		LOGGER.log(Level.INFO, "SyncServer正在关闭...");
		if (serverThread != null) {
			serverRunning = false;
			try {
				server.stop(0); // 停止服务
				serverThread.join(); // 等待线程结束
				toggleServerButton.setText("启动服务端"); // 更新按钮文本
				LOGGER.log(Level.INFO, "SyncServer已关闭");
			} catch (InterruptedException error) {
				LOGGER.log(Level.WARNING, "等待线程结束时被中断", error);
			} finally {
				serverThread = null; // 清除线程引用
			}
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
				server = HttpServer.create(new InetSocketAddress(serverPort), 0);
				server.createContext("/", new HttpRequestHandler());
				server.setExecutor(null);
				server.start();
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "服务器异常: " + error.getMessage());
			}
		});
		serverThread.start();
		serverRunning = true;
		LOGGER.log(Level.INFO, "SyncServer正在运行...端口号为: " + serverPort);
		toggleServerButton.setText("停止服务端"); // 更新按钮文本
	}
	// 切换客户端状态
	private void toggleClient() {
		toggleClientButton.setEnabled(false);
		if (clientRunning) stopClient();
		else startClient();
		toggleClientButton.setText(clientRunning ? "停止客户端" : "启动客户端");
		toggleIcon(); // 图标切换
		toggleClientButton.setEnabled(true);
	}
	private void stopClient() {
		if (clientThread != null) {
			clientRunning = false;
			toggleClientButton.setText("启动客户端"); // 更新按钮文本
			try {
				connection.disconnect(); // 断开连接
				clientThread.stop();
				clientThread.join(); // 等待线程结束
				LOGGER.log(Level.INFO, "SyncClient已停止");
			} catch (InterruptedException error) {
				LOGGER.log(Level.WARNING, "等待线程结束时被中断", error);
			} finally {
				clientThread = null; // 清除线程引用
			}
		}
	}
	private void startClient() {
		toggleClientButton.setText("停止客户端"); // 更新按钮文本
		initializeFiles(false); // 初始化文件
		clientRunning = true;
		LOGGER.log(Level.INFO, "SyncClient正在运行...服务器地址为: " + serverAddress + " 端口号为: " + serverPort);
		clientThread = new Thread(() -> {
			requestList(); // 从服务端获取文件和MD5值列表
			if (REQUEST_MAP.isEmpty()) {
				LOGGER.log(Level.WARNING, "未获取到任何文件");
				stopClient();
				return;
			}
			LOGGER.log(Level.INFO, "获取到 " + REQUEST_MAP.size() + " 个文件");
			int downloadedCount = 0;
			// 解析文件列表
			for (Map.Entry<String, String> entry : REQUEST_MAP.entrySet()) {
				String fileName = entry.getKey(); // 文件名
				String MD5Value = entry.getValue(); // MD5值
				if (fileName.isEmpty() || MD5Value.isEmpty()) continue; // 忽略空行
				String clientPath = clientSyncDirectory + SEPARATOR + fileName; // 设置下载路径
				String downloadReturn = downloadFile(fileName, clientPath); // 下载文件
				if (downloadReturn.equals("OK")) {
					downloadedCount++; // 成功下载时增加计数
					LOGGER.log(Level.INFO, "已下载文件: " + fileName + downloadedCount);
				} else if (downloadReturn.equals("SKIP")) {
					LOGGER.log(Level.INFO, "跳过文件: " + fileName);
				} else LOGGER.log(Level.SEVERE, "下载文件失败: " + fileName);
			}
			// 下载完成后的日志记录
			LOGGER.log(Level.INFO, "下载完成,总共下载了 " + downloadedCount + " 个文件。");
			stopClient();
		});
		clientThread.start();
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
		JButton settingsButton = new JButton("设置");
		toggleServerButton = new JButton("启动服务端");
		toggleClientButton = new JButton("启动客户端");
		JButton shutdownButton = new JButton("退出");
		// 添加按钮事件
		openDirectoryButton.addActionListener(event -> {
			try {
				Desktop.getDesktop().open(new File(CURRENT_DIRECTORY)); // 打开目录
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "打开目录时出错: " + error.getMessage());
			}
		});
		toggleServerButton.addActionListener(event -> toggleServer()); // 服务端按钮事件
		toggleClientButton.addActionListener(event -> toggleClient()); // 客户端按钮事件
		settingsButton.addActionListener(event -> openSettingsDialog()); // 打开设置对话框
		shutdownButton.addActionListener(event -> System.exit(0)); // 关闭程序
		// 添加按钮到按钮面板
		buttonPanel.add(openDirectoryButton);
		buttonPanel.add(settingsButton);
		buttonPanel.add(toggleServerButton);
		buttonPanel.add(toggleClientButton);
		buttonPanel.add(shutdownButton);
		panel.add(buttonPanel, BorderLayout.SOUTH); // 添加按钮面板到主面板
		add(panel);  // 添加主面板到窗口
		// 创建日志处理器并添加到 logger
		JTextPaneLogHandler logHandler = new JTextPaneLogHandler(logTextPane); // 将 JTextAreaLogHandler 替换为 JTextPaneLogHandler
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
			Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource(serverRunning ? "/IconO.png" : "/IconI.png"));
			trayIcon = new TrayIcon(image, "SyncServer");
			trayIcon.setImageAutoSize(true); // 自动调整图标大小
			trayIcon.setToolTip("SyncServer 控制面板");
			PopupMenu popup = getPopupMenu(); // 创建右键菜单
			trayIcon.setPopupMenu(popup);
			trayIcon.addActionListener(event -> setVisible(true)); // 显示窗口
			try {
				tray.add(trayIcon); // 添加托盘图标
			} catch (AWTException error) {
				LOGGER.log(Level.SEVERE, "添加托盘图标失败: " + error.getMessage());
			}
		}
	}
	// 切换图标
	private void toggleIcon() {
		Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource(serverRunning || clientRunning ? "/IconO.png" : "/IconI.png"));
		setIconImage(icon);
		if (trayIcon != null) trayIcon.setImage(icon);
	}
	private PopupMenu getPopupMenu() {
		PopupMenu popup = new PopupMenu();
		MenuItem openItem = new MenuItem("Open");
		openItem.addActionListener(event -> setVisible(true));  // 显示窗口
		MenuItem hideItem = new MenuItem("Hide");
		hideItem.addActionListener(event -> setVisible(false)); // 隐藏窗口
		MenuItem openDirectoryItem = new MenuItem("Open Directory");
		openDirectoryItem.addActionListener(event -> {
			try {
				Desktop.getDesktop().open(new File(CURRENT_DIRECTORY)); // 打开目录
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
		// 加载配置
		loadConfig(true);
		loadConfig(false);
		final int inputHeight = (int) (SCREEN_SIZE.height * 0.1); // 输入框高度为屏幕高度的10%
		final int inputWidth = (int) (SCREEN_SIZE.width * 0.2); // 输入框宽度为屏幕宽度的20%
		int borderPadding = (int) (inputHeight * 0.05); // 边距为输入框高度的5%
		JDialog settingsDialog = new JDialog(this, "设置", true);
		settingsDialog.setLayout(new BorderLayout(borderPadding, borderPadding));
		// 创建选项卡面板
		JTabbedPane tabbedPane = new JTabbedPane();
		// 服务端设置选项卡
		JPanel serverPanel = new JPanel(new GridLayout(4, 2));
		serverPanel.setBorder(BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding));
		JTextField serverPortField = new JTextField(String.valueOf(serverPort));
		JTextField serverUploadRateLimitField = new JTextField(String.valueOf(uploadRateLimit));
		JTextField serverSyncDirectoryNameField = new JTextField(serverSyncDirectoryName);
		JTextField serverSyncDirectoryPathField = new JTextField(serverSyncDirectory);
		// 创建选择单位的下拉框
		String[] uploadRateUnits = new String[]{"B/s", "KB/s", "MB/s", "GB/s"};
		JComboBox<String> unitComboBox = new JComboBox<>(uploadRateUnits);
		unitComboBox.setSelectedItem(uploadRateLimitUnit);
		// 添加标签和文本框到设置面板
		serverPanel.add(new JLabel("端口号: "));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel("上传速率限制(0为无限制): "));
		// 添加上传速率限制输入框与单位选择框
		JPanel rateLimitPanel = new JPanel(new GridLayout(1, 2, 5, 0));
		rateLimitPanel.add(serverUploadRateLimitField);
		rateLimitPanel.add(unitComboBox); // 将单位选择框添加到输入框旁边
		serverPanel.add(rateLimitPanel);
		serverPanel.add(new JLabel("服务端同步文件夹名称: "));
		serverPanel.add(serverSyncDirectoryNameField);
		serverPanel.add(new JLabel("服务端同步文件夹路径: "));
		serverPanel.add(serverSyncDirectoryPathField);
		// 设置输入框大小
		serverPortField.setSize(new Dimension(inputWidth, inputHeight));
		serverSyncDirectoryNameField.setSize(new Dimension(inputWidth, inputHeight));
		serverSyncDirectoryPathField.setSize(new Dimension(inputWidth, inputHeight));
		// 设置上传速率限制输入框和单位选择框大小
		serverUploadRateLimitField.setSize(new Dimension(inputWidth * 3 / 4, inputHeight));
		unitComboBox.setSize(new Dimension(inputWidth / 4, inputHeight));
		// 客户端设置选项卡
		JPanel clientPanel = new JPanel(new GridLayout(4, 2));
		clientPanel.setBorder(BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding));
		// 创建文本框
		JTextField clientPortField = new JTextField(String.valueOf(clientPort));
		JTextField clientAddressField = new JTextField(serverAddress);
		JTextField clientSyncDirectoryNameField = new JTextField(clientSyncDirectoryName);
		JTextField clientSyncDirectoryPathField = new JTextField(clientSyncDirectory);
		// 添加标签和文本框
		clientPanel.add(new JLabel("端口号: "));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel("服务器地址: "));
		clientPanel.add(clientAddressField);
		clientPanel.add(new JLabel("客户端同步文件夹名称: "));
		clientPanel.add(clientSyncDirectoryNameField);
		clientPanel.add(new JLabel("客户端同步文件夹路径: "));
		clientPanel.add(clientSyncDirectoryPathField);
		// 添加选项卡到选项卡面板
		tabbedPane.addTab("服务端设置", serverPanel);
		tabbedPane.addTab("客户端设置", clientPanel);
		// 添加选项卡到对话框的内容区域
		settingsDialog.add(tabbedPane, BorderLayout.CENTER);
		// 保存按钮
		JButton configSaveButton = new JButton("保存服务端设置");
		// 修改保存按钮的逻辑
		configSaveButton.addActionListener(event -> {
			// 定义输入框数组及其对应的提示信息
			Object[][] inputs = {{serverPortField, "服务端端口"}, {serverSyncDirectoryNameField, "服务端同步文件夹名称"}, {serverSyncDirectoryPathField, "服务端同步文件夹路径"}, {clientPortField, "客户端端口"}, {clientSyncDirectoryNameField, "客户端同步文件夹名称"}, {clientSyncDirectoryPathField, "客户端同步文件夹路径"}};
			// 检查输入框是否为空
			for (Object[] input : inputs) if (isTextFieldEmpty((JTextField) input[0], (String) input[1])) return;
			// 检测端口号是否是数字且在合法范围内
			if (isPortValid(serverPortField) || isPortValid(clientPortField)) return;
			// 检测上传速率上限
			String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
			if (isNotNumber(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) return;
			// 赋值配置
			serverPort = Integer.parseInt(serverPortField.getText().trim());
			uploadRateLimit = uploadRateLimitText.isEmpty() ? 0 : Integer.parseInt(uploadRateLimitText);
			uploadRateLimitUnit = (String) unitComboBox.getSelectedItem();
			serverSyncDirectoryName = serverSyncDirectoryNameField.getText().trim();
			serverSyncDirectory = serverSyncDirectoryPathField.getText().trim();
			clientPort = Integer.parseInt(clientPortField.getText().trim());
			serverAddress = clientAddressField.getText().trim();
			clientSyncDirectoryName = clientSyncDirectoryNameField.getText().trim();
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
		// 添加按钮面板到对话框的南部
		settingsDialog.add(buttonPanel, BorderLayout.SOUTH);
		// 设置对话框的基本属性
		settingsDialog.setSize(SCREEN_SIZE.width / 4, SCREEN_SIZE.height / 5);
		settingsDialog.setLocationRelativeTo(this); // 居中
		settingsDialog.setVisible(true); // 显示对话框
	}
	// 检测端口号有效性的方法
	private boolean isPortValid(JTextField portField) {
		String portText = portField.getText().trim();
		return isNotNumber(portText) || Integer.parseInt(portText) <= 0 || Integer.parseInt(portText) > 65535;
	}
	private boolean isNotNumber(String text) {
		if (text == null || text.trim().isEmpty()) return true; // 空字符串返回false
		try {
			Long.parseLong(text.trim()); // 尝试将文本转换为长整型
			return false; // 转换成功，返回true
		} catch (NumberFormatException error) {
			return true; // 转换失败，返回false
		}
	}
	private boolean isTextFieldEmpty(JTextField textField, String fieldName) {
		String text = textField.getText().trim();
		if (text.isEmpty()) {
			LOGGER.log(Level.WARNING, fieldName + "不能为空");
			return true; // 返回true表示输入框为空
		}
		return false; // 返回false表示输入框不为空
	}
	// 服务端上传速率限制器
	private synchronized void limit(long bytesToSend) throws InterruptedException {
		if (uploadRateLimit <= 0) return; // 如果速率限制为0,则不限制
		long currentTimestamp = System.currentTimeMillis(); // 获取当前时间戳
		if (currentTimestamp - lastTimestamp >= 500) { // 如果距离上次上传时间超过500毫秒
			bytesSentThisSecond.set(0); // 重置计数
			lastTimestamp = currentTimestamp; // 更新上次上传时间
		}
		long byteLimit = convertToBytes(uploadRateLimit, uploadRateLimitUnit);// 根据实际单位转换uploadRateLimit为字节数
		while (bytesSentThisSecond.get() + bytesToSend > byteLimit) wait(); // 如果当前已经达到限制，开始等待
		bytesSentThisSecond.addAndGet(bytesToSend);// 更新已发送字节数
		if (bytesSentThisSecond.get() < byteLimit) notifyAll(); // 唤醒在等待的线程
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
				LOGGER.log(Level.WARNING, "未知的单位: " + unit);
				return 0; // 返回0，表示不允许的单位
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
				} else exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1); // 如果不是GET请求,返回405状态码
			} catch (IOException error) {
				LOGGER.log(Level.SEVERE, "处理请求时出错: " + error.getMessage(), error);
			}
		}
		// 处理请求
		private void processRequest(String requestURI, HttpExchange exchange) throws IOException {
			// 解析requestURI,查看是文件请求还是文件列表请求
			if ("/list".equals(requestURI)) {
				sendList(exchange);
			} else handleFileRequest(requestURI, exchange);
		}
		private void handleFileRequest(String requestURI, HttpExchange exchange) throws IOException {
			String requestedMD5 = requestURI.substring(1);
			// 检查请求的 MD5 内容是否在 SYNC_MAP 中
			if (!SYNC_MAP.containsValue(requestedMD5)) {
				String response = "未找到对应的文件";
				byte[] responseBytes = response.getBytes(); // 转换为字节数组
				sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_NOT_FOUND); // 发送响应
				return;
			}
			// 查找对应 MD5 值的文件
			String filePath = getFilePathByMD5(requestedMD5);
			if (filePath != null) {
				sendRequestedFile(filePath, exchange); // 发送请求的文件
				LOGGER.log(Level.INFO, "已发送文件: " + filePath);
			} else {
				String response = "未找到对应的文件";
				byte[] responseBytes = response.getBytes(); // 转换为字节数组
				sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_NOT_FOUND); // 发送响应
			}
		}
		private String getFilePathByMD5(String requestedMD5) {
			for (Map.Entry<String, String> entry : SYNC_MAP.entrySet()) {
				String fileName = entry.getKey();
				String MD5Value = entry.getValue();
				// 检查 MD5 文件内容
				if (requestedMD5.equals(MD5Value)) {
					String filePath = serverSyncDirectory + SEPARATOR + fileName; // 获取对应的文件路径
					LOGGER.log(Level.INFO, "根据MD5请求找到文件: " + filePath);
					return filePath; // 找到并返回文件路径
				}
			}
			return null; // 未找到文件
		}
		// 发送文件名和MD5值列表
		private void sendList(HttpExchange exchange) throws IOException {
			StringBuilder responseBuilder = new StringBuilder(); // 用于构建响应内容
			for (Map.Entry<String, String> entry : SYNC_MAP.entrySet()) { // 遍历同步文件列表
				String fileName = entry.getKey(); // 获取文件名
				String MD5Value = entry.getValue(); // 获取MD5值
				if (MD5Value != null) {
					responseBuilder.append(fileName).append("\n").append(MD5Value).append("\n"); // 每个文件名及对应的MD5值
					LOGGER.log(Level.INFO, "添加文件名及MD5值到列表: " + fileName + " - " + MD5Value);
				} else LOGGER.log(Level.WARNING, "文件: " + fileName + " 未找到对应的MD5值");
			}
			String response = responseBuilder.toString(); // 转换为字符串
			byte[] responseBytes = response.getBytes(); // 转换为字节数组
			sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_OK); // 发送响应
		}
		// 发送请求的文件
		private void sendRequestedFile(String filePath, HttpExchange exchange) throws IOException {
			File file = new File(filePath);
			if (file.exists() && file.isFile()) {
				byte[] responseBytes = Files.readAllBytes(file.toPath()); // 读取文件内容
				sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_OK); // 发送响应
			} else {
				String response = "文件不存在: " + filePath;
				byte[] responseBytes = response.getBytes(); // 转换为字节数组
				sendResponse(exchange, responseBytes, HttpURLConnection.HTTP_NOT_FOUND); // 发送响应
			}
		}
	}
}
