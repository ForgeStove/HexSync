import com.sun.net.httpserver.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.io.File.separator;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.lang.Math.*;
import static java.lang.String.*;
import static java.lang.System.*;
import static java.net.HttpURLConnection.*;
import static java.nio.file.Files.*;
public class HexSync {
	private static final String HEX_SYNC_NAME = HexSync.class.getName(); // 程序名称
	private static final AtomicLong AVAILABLE_TOKENS = new AtomicLong(0); // 当前可用令牌数量
	private static final String LOG_PATH = HEX_SYNC_NAME + separator + "latest.log"; // 日志文件路径
	private static final String CONFIG_PATH = HEX_SYNC_NAME + separator + "config.properties"; // 配置文件路径
	private static final String SERVER_PORT = "serverPort"; // 服务端端口配置项
	private static final String SERVER_SYNC_DIRECTORY = "serverSyncDirectory"; // 服务端同步文件夹路径配置项
	private static final String SERVER_UPLOAD_RATE_LIMIT = "serverUploadRateLimit"; // 上传速率限制配置项
	private static final String SERVER_AUTO_START = "serverAutoStart"; // 服务端自动启动配置项
	private static final String CLIENT_PORT = "clientPort"; // 客户端端口配置项
	private static final String SERVER_ADDRESS = "serverAddress"; // 服务器地址配置项
	private static final String CLIENT_SYNC_DIRECTORY = "clientSyncDirectory"; // 客户端同步文件夹路径配置项
	private static final String CLIENT_ONLY_DIRECTORY = "clientOnlyDirectory"; // 仅客户端文件夹路径配置项
	private static final String CLIENT_AUTO_START = "clientAutoStart"; // 客户端自动启动配置项
	private static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync"; // 项目GitHub地址
	private static final String INFO = "信息";
	private static final String WARNING = "警告";
	private static final String SEVERE = "严重";
	private static final boolean HEADLESS = GraphicsEnvironment.isHeadless(); // 是否处于无头模式
	private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();// 创建日志记录线程池
	private static int logMaxLines; // 日志面板最大行数
	private static FileWriter logWriter; // 日志记录器
	private static String htmlLog = ""; // 日志面板内容
	private static String serverSyncDirectory = "mods"; // 服务端同步文件夹路径，默认值mods
	private static String clientSyncDirectory = "mods"; // 客户端同步文件夹路径，默认值mods
	private static String clientOnlyDirectory = "clientOnlyMods"; // 仅客户端文件夹路径，默认值clientOnlyMods
	private static String serverUploadRateLimitUnit = "MB/s"; // 上传速率限制单位，默认MB/s
	private static String serverAddress = "localhost"; // 服务器地址，默认值localhost
	private static Map<String, String> serverMap; // 存储服务端文件名和对应的校验码数据
	private static HttpServer HTTPServer; // 存储服务器实例
	private static HttpURLConnection HTTPURLConnection; // 存储客户端连接实例
	private static Thread serverThread; // 服务器线程
	private static Thread clientThread; // 客户端线程
	private static JTextPane textPane; // 状态标签
	private static boolean errorDownload; // 客户端下载文件时是否发生错误，影响客户端是否自动关闭
	private static boolean serverAutoStart; // 服务端自动启动，默认不自动启动
	private static boolean clientAutoStart; // 客户端自动启动，默认不自动启动
	private static int screenLength; // 屏幕长边
	private static int serverPort = 65535; // 服务端端口，默认值65535
	private static int clientPort = 65535; // 客户端端口，默认值65535
	private static long serverUploadRateLimit = 1; // 上传速率限制值，默认限速1MB/s
	public static void main(String[] args) {
		initLog();
		loadConfig();
		initUI();
	}
	// 初始化日志
	private static void initLog() {
		try {
			makeDirectory(HEX_SYNC_NAME);
			logWriter = new FileWriter(LOG_PATH, false);
		} catch (IOException error) {
			err.println("日志初始化失败: " + error.getMessage());
		}
		try {
			logMaxLines = parseInt(getProperty("logMaxLines", "50"));
		} catch (NumberFormatException error) {
			log(SEVERE, "日志面板最大行数格式错误: " + error.getMessage());
			logMaxLines = 50;
		}
	}
	// 日志记录
	private static void log(String level, String message) {
		if (getProperty("log", "true").equalsIgnoreCase("true")) logExecutor.submit(() -> {
			try {
				String format = new SimpleDateFormat("[HH:mm:ss]").format(new Date()) + " [" + level + "] " + message;
				logWriter.write(format + lineSeparator());
				logWriter.flush();
				boolean info = level.equals(INFO);
				boolean warning = level.equals(WARNING);
				boolean severe = level.equals(SEVERE);
				if (getProperty("ansi", "true").equalsIgnoreCase("false")) out.println(format);
				else out.printf(
						"%s%s\u001B[0m%n",
						info ? "\u001B[32m" : warning ? "\u001B[33m" : severe ? "\u001B[31m" : "\u001B[0m",
						format
				);
				if (textPane != null) SwingUtilities.invokeLater(() -> {
					htmlLog = (
							htmlLog + "<span style='color: " + (
									info ? "green" : warning ? "orange" : severe ? "red" : "black"
							) + ";'>" + format + "</span><br>"
					).replace(lineSeparator(), "<br>"); // 添加新的日志行
					String[] lines = htmlLog.split("<br>"); // 根据 <br> 计算当前行数
					if (lines.length > logMaxLines) {
						StringBuilder newHtmlLog = new StringBuilder();
						for (int i = lines.length - logMaxLines; i < lines.length; i++)
							newHtmlLog.append(lines[i]).append("<br>"); // 只保留最新的 MAX_LINES 行
						htmlLog = newHtmlLog.toString(); // 更新 HTML 日志
					}
					textPane.setText("<span style='font-family: Consolas;'>" + htmlLog + "</span>");
					JScrollBar vertical = ((JScrollPane) textPane.getParent().getParent()).getVerticalScrollBar();
					vertical.setValue(vertical.getMaximum()); // 自动滚动到最新内容
				});
			} catch (IOException error) {
				if (logWriter == null) initLog();
				else err.println("无法写入日志: " + error.getMessage());
			}
		});
	}
	// 初始化UI
	private static void initUI() {
		if (serverAutoStart) startServer();
		if (clientAutoStart) startClient();
		if (HEADLESS) headlessUI(); // 无头模式
		else normalUI(); // 有头模式
	}
	// 无头模式UI
	private static void headlessUI() {
		out.println("欢迎使用" + HEX_SYNC_NAME + "!");
		out.println("输入 HELP 以获取帮助.");
		Scanner scanner = new Scanner(in);
		// 命令映射
		Map<String, Runnable> map = getRunnableMap();
		while (true) try {
			out.print(HEX_SYNC_NAME + ">");
			map.getOrDefault(
					scanner.nextLine().trim().toUpperCase(),
					() -> out.println("无效命令,输入 HELP 以获取帮助.")
			).run();
		} catch (Exception error) {
			log(SEVERE, "命令处理时出错: " + error.getMessage());
			break;
		}
	}
	// 有头模式UI
	private static void normalUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(() -> {
				Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
				screenLength = max(size.width, size.height);
				addPanel(newJDialog(screenLength / 4, screenLength / 4, HEX_SYNC_NAME));
			});
		} catch (Exception error) {
			log(SEVERE, "初始化UI时出错:" + error.getMessage());
		}
	}
	// 初始化文件
	private static void initFiles(boolean isServer) {
		makeDirectory(isServer ? serverSyncDirectory : clientSyncDirectory);
		makeDirectory(HEX_SYNC_NAME);
		loadConfig();
		if (isServer) serverMap = initFileSHAMap(serverSyncDirectory);
		else makeDirectory(clientOnlyDirectory);
		log(INFO, isServer ? "服务端初始化完成" : "客户端初始化完成");
	}
	// 初始化文件名校验码键值对表
	private static Map<String, String> initFileSHAMap(String directory) {
		Map<String, String> map = new HashMap<>();
		File[] fileList = new File(directory).listFiles(); // 获取文件夹下的所有文件
		if (fileList == null) return null;
		for (File file : fileList) if (file.isFile()) map.put(file.getName(), calculateSHA(file));
		return map;
	}
	// 创建文件夹
	private static void makeDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.isDirectory()) return;
		if (directory.mkdirs()) log(INFO, "文件夹已创建: " + directoryPath);
		else log(SEVERE, "无法创建文件夹: " + directoryPath);
	}
	// 加载配置文件
	private static void loadConfig() {
		File configFile = new File(CONFIG_PATH);
		if (!configFile.isFile()) {
			saveConfig();
			return;
		}
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile))) {
			String line;
			Map<String, Consumer<String>> configMap = getConfigMap();
			while ((line = bufferedReader.readLine()) != null) {
				if (line.startsWith("#")) continue;
				String[] parts = line.split("=");
				if (parts.length != 2) {
					log(WARNING, "配置格式错误: " + line);
					continue;
				}
				Consumer<String> action = configMap.get(parts[0].trim());
				if (action != null) action.accept(parts[1].trim());
				else log(WARNING, "配置项错误: " + line);
			}
		} catch (IOException error) {
			log(SEVERE, "配置读取失败: " + error.getMessage());
		}
	}
	// 构建配置映射
	private static Map<String, Consumer<String>> getConfigMap() {
		Map<String, Consumer<String>> configMap = new HashMap<>();
		configMap.put(SERVER_PORT, input -> serverPort = parseInt(input));
		configMap.put(
				SERVER_UPLOAD_RATE_LIMIT, input -> {
					String[] limitParts = input.split(" ");
					if (limitParts.length != 2) log(WARNING, "上传速率限制格式错误");
					else {
						serverUploadRateLimit = parseLong(limitParts[0]);
						serverUploadRateLimitUnit = limitParts[1];
					}
				}
		);
		configMap.put(SERVER_SYNC_DIRECTORY, input -> serverSyncDirectory = input);
		configMap.put(SERVER_AUTO_START, input -> serverAutoStart = parseBoolean(input));
		configMap.put(CLIENT_PORT, input -> clientPort = parseInt(input));
		configMap.put(SERVER_ADDRESS, input -> serverAddress = input);
		configMap.put(CLIENT_SYNC_DIRECTORY, input -> clientSyncDirectory = input);
		configMap.put(CLIENT_ONLY_DIRECTORY, input -> clientOnlyDirectory = input);
		configMap.put(CLIENT_AUTO_START, input -> clientAutoStart = parseBoolean(input));
		return configMap;
	}
	// 地址格式化,转换为HTTP协议
	private static String formatHTTP(String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("http://") ? address : "http://" + address; // 添加HTTP协议头
	}
	// 计算文件校验码
	private static String calculateSHA(File file) {
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] byteBuffer = new byte[16384];
			int bytesRead;
			MessageDigest SHA = MessageDigest.getInstance("SHA-256");
			while ((bytesRead = fileInputStream.read(byteBuffer)) != -1) SHA.update(byteBuffer, 0, bytesRead);
			StringBuilder stringBuilder = new StringBuilder();
			for (byte singleByte : SHA.digest()) stringBuilder.append(format("%02x", singleByte));
			return stringBuilder.toString();
		} catch (Exception error) {
			log(SEVERE, "计算校验码时出错: " + error.getMessage());
			return null;
		}
	}
	// 从服务器请求文件名和校验码列表
	private static Map<String, String> requestFileSHAList() {
		String URL = formatHTTP(serverAddress) + ":" + clientPort + "/list"; // 服务器地址
		log(INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, String> requestMap = new HashMap<>(); // 复制请求列表
		try {
			int responseCode = getResponseCode(new URL(URL));
			if (responseCode != HTTP_OK) {
				if (clientThread != null) log(SEVERE, "请求列表失败,错误代码: " + responseCode);
				errorDownload = true;
				return requestMap;
			}
		} catch (IOException error) {
			if (clientThread != null) log(SEVERE, "无法连接至服务器: " + error.getMessage());
			errorDownload = true;
			return requestMap;
		}
		try (
				BufferedReader bufferedReader =
						new BufferedReader(new InputStreamReader(HTTPURLConnection.getInputStream()))
		) {
			String fileName;
			while ((fileName = bufferedReader.readLine()) != null) { // 读取文件名
				String SHAValue = bufferedReader.readLine(); // 读取对应的校验码
				if (SHAValue != null) requestMap.put(fileName, SHAValue); // 将文件名与校验码放入Map
			}
		} catch (IOException error) {
			log(SEVERE, "读取响应时出错: " + error.getMessage());
			errorDownload = true;
		}
		log(INFO, "获取到 [" + requestMap.size() + "] 个文件"); // 记录请求成功日志
		return requestMap;
	}
	// 从服务器下载文件
	private static boolean successDownloadFile(String filePath, Map<String, String> toDownloadMap) {
		if (clientThread == null) return false; // 客户端线程已关闭
		File clientFile = new File(filePath); // 目标本地文件
		String requestSHA = toDownloadMap.get(filePath.substring(clientSyncDirectory.length() + 1));
		try {
			int responseCode = getResponseCode(new URL(format(
					"%s:%d/download/%s",
					formatHTTP(serverAddress),
					clientPort,
					requestSHA
			)));
			if (responseCode != HTTP_OK) {
				log(SEVERE, "下载失败,错误代码: " + responseCode);
				return false;
			}
		} catch (IOException error) {
			log(SEVERE, "无法连接至服务器: " + error.getMessage());
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
			log(SEVERE, "无法获取请求的校验码: " + filePath);
			return false;
		}
		if (requestSHA.equals(calculateSHA(clientFile))) return true; // 下载成功且校验通过
		log(SEVERE, "校验失败,文件可能已损坏: " + filePath);
		if (!clientFile.delete()) log(SEVERE, "无法删除损坏的文件: " + clientFile.getPath());
		return false;
	}
	// 获取响应码
	private static int getResponseCode(URL requestURL) throws IOException {
		HTTPURLConnection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
		HTTPURLConnection.setRequestMethod("GET"); // 设置请求方式为GET
		HTTPURLConnection.setConnectTimeout(3000); // 设置连接超时
		HTTPURLConnection.setReadTimeout(3000); // 设置读取超时
		return HTTPURLConnection.getResponseCode(); // 返回响应码
	}
	// 保存配置
	private static void saveConfig() {
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
			else configContent.append(entry[0])
					.append("=")
					.append(entry.length > 1 ? entry[1] : "")
					.append(lineSeparator());
		}
		configContent.deleteCharAt(configContent.length() - 1);// 去除末尾的换行符
		File configFile = new File(CONFIG_PATH);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFile))) {
			writer.write(configContent.toString());// 写入配置文件
			log(INFO, "配置已保存: " + lineSeparator() + configContent);
		} catch (IOException error) {
			log(SEVERE, "配置保存失败: " + error.getMessage());
		}
	}
	// 检查文件是否已存在并进行校验
	private static boolean checkNoFile(File file, String fileName, Map<String, String> map) {
		if (!file.exists()) return true;
		return !map.get(fileName).equals(calculateSHA(file));
	}
	// 字符串转端口
	private static boolean getPort(String portInput, boolean isServer) {
		try {
			int port = parseInt(portInput);
			if (port > 0 && port < 65536) {
				// 设置端口并记录日志
				if (isServer) serverPort = port;
				else clientPort = port;
				log(INFO, (isServer ? "服务端" : "客户端") + "端口已设置为: " + port);
				return true;
			} else {
				log(WARNING, "端口号范围错误: " + portInput);
				return false;
			}
		} catch (NumberFormatException error) {
			log(WARNING, "端口号格式错误: " + portInput);
			return false;
		}
	}
	// 检测数字输入是否不在Long范围内
	private static boolean invalidLong(String numberInput) {
		String trimmedInput = numberInput.trim();
		if (trimmedInput.isEmpty()) return true;
		try {
			parseLong(trimmedInput);
			return false;
		} catch (NumberFormatException error) {
			log(WARNING, "错误的数字格式或超出范围: " + numberInput);
			return true;
		}
	}
	// 关于按钮
	private static void aboutButtonAction() {
		SwingUtilities.invokeLater(() -> newJDialog(screenLength / 3, screenLength / 12, "关于").getContentPane()
				.add(getJScrollPane("<span style=\"font-weight: bold;font-family: Consolas;\">"
						+ HEX_SYNC_NAME
						+ "<br>By: ForgeStove<br>GitHub: <a href=\""
						+ GITHUB_URL
						+ "\">"
						+ GITHUB_URL
						+ "</a></span>")));
	}
	// HTML内容转JScrollPane
	private static JScrollPane getJScrollPane(String html) {
		JTextPane textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setText(html);
		textPane.setEditable(false);
		textPane.addHyperlinkListener(event -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) try {
				Desktop.getDesktop().browse(event.getURL().toURI());
			} catch (Exception error) {
				log(SEVERE, "无法打开超链接: " + error.getMessage());
			}
		});
		return new JScrollPane(textPane);
	}
	// 构建命令映射
	private static Map<String, Runnable> getRunnableMap() {
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", HexSync::startServer);
		map.put("RC", HexSync::startClient);
		map.put("SS", HexSync::stopServer);
		map.put("SC", HexSync::stopClient);
		map.put("SET", HexSync::headlessSettings);
		map.put("GITHUB", () -> out.println(GITHUB_URL));
		map.put("HELP", HexSync::headlessHelp);
		map.put("EXIT", () -> exit(0));
		return map;
	}
	// 无头模式设置
	private static void headlessSettings() {
		headlessSettingsHelp();
		// 命令映射
		Map<String, Consumer<String[]>> map = getConsumerMap();
		while (true) try {
			out.print(HEX_SYNC_NAME + "Settings>");
			String[] parts = new Scanner(in).nextLine().split("\\s+");
			map.getOrDefault(parts[0].toUpperCase(), args -> err.println("无效命令,输入 HELP 以获取帮助."))
					.accept(parts);
			if (parts[0].equalsIgnoreCase("SAVE")) break;
		} catch (Exception error) {
			err.println("无效命令,输入 HELP 以获取帮助.");
		}
	}
	// 构建设置命令映射
	private static Map<String, Consumer<String[]>> getConsumerMap() {
		Map<String, Consumer<String[]>> map = new HashMap<>();
		map.put("SP", args -> getPort(args[1], true));
		map.put("SL", args -> setRate(args[1]));
		map.put("SD", args -> setDirectory(args[1], "服务端同步", value -> serverSyncDirectory = value));
		map.put("SS", args -> setAutoStart(args[1], true, value -> serverAutoStart = value));
		map.put("CP", args -> getPort(args[1], false));
		map.put("SA", args -> setAddress(args[1]));
		map.put("CD", args -> setDirectory(args[1], "客户端同步", value -> clientSyncDirectory = value));
		map.put("CO", args -> setDirectory(args[1], "仅客户端模组", value -> clientOnlyDirectory = value));
		map.put("CS", args -> setAutoStart(args[1], false, value -> clientAutoStart = value));
		map.put("SAVE", args -> saveConfig());
		map.put("HELP", args -> headlessSettingsHelp());
		return map;
	}
	// 无头模式帮助
	private static void headlessHelp() {
		printlnStrings(new String[]{
				"RS     |启动服务端",
				"RC     |启动客户端",
				"SS     |停止服务端",
				"SC     |停止客户端",
				"SET    |设置",
				"GITHUB |仓库",
				"EXIT   |退出",
				"HELP   |帮助"
		});
	}
	// 无头模式设置帮助
	private static void headlessSettingsHelp() {
		printlnStrings(new String[]{
				"SP [端口号]                    |设置服务端端口",
				"SL [速率] [B/s/KB/s/MB/s/GB/s] |设置服务端上传速率",
				"SD [目录]                      |设置服务端同步目录",
				"SS [y/Y/n/N]                   |设置服务端自动启动",
				"CP [端口号]                    |设置客户端端口",
				"SA [地址]                      |设置服务器地址",
				"CD [目录]                      |设置客户端同步目录",
				"CO [目录]                      |设置客户端仅客户端目录",
				"CS [y/Y/n/N]                   |设置客户端自动启动",
				"SAVE                           |保存并退出",
				"HELP                           |帮助"
		});
	}
	// 输出帮助信息的通用方法
	private static void printlnStrings(String[] messages) {
		for (String message : messages) out.println(message);
	}
	// 设置上传速率
	private static void setRate(String input) {
		String rateInput;
		try {
			rateInput = input.substring(input.indexOf(" ") + 1);
		} catch (IndexOutOfBoundsException error) {
			err.println("无效输入,请输入数字及单位.");
			return;
		}
		if (rateInput.matches("\\d+(\\s+B/s|\\s+KB/s|\\s+MB/s|\\s+GB/s)")) {
			String[] rateParts = rateInput.split("\\s+");
			if (invalidLong(rateParts[0])) {
				err.println("无效输入,请输入数字.");
				return;
			}
			serverUploadRateLimit = parseLong(rateParts[0]);
			serverUploadRateLimitUnit = rateParts[1];
			log(INFO, "服务端上传速率已设置为: " + serverUploadRateLimit + " " + serverUploadRateLimitUnit);
		} else err.println("无效输入,请输入数字及单位.");
	}
	// 设置服务端地址
	private static void setAddress(String addressInput) {
		if (addressInput.matches("\\d+\\.\\d+")) {
			serverAddress = addressInput;
			log(INFO, "服务端地址已设置为: " + serverAddress);
		} else err.println("无效输入,请输入IP地址.");
	}
	// 设置目录
	private static void setDirectory(String directory, String log, Consumer<String> setter) {
		if (!directory.isEmpty() && !directory.contains(separator)) {
			setter.accept(directory);
			log(INFO, log + "目录已设置为: " + directory);
		} else err.println("目录格式错误,请输入绝对路径或相对路径.");
	}
	// 设置自动启动
	private static void setAutoStart(String input, Boolean isServer, Consumer<Boolean> setter) {
		if (input.matches("[yYnN]")) {
			boolean value = input.matches("[yY]");
			setter.accept(value);
			log(INFO, isServer ? "服务端" : "客户端" + "自动启动已设置为: " + value);
		} else err.println("无效输入,请输入 y/Y 或 n/N.");
	}
	// 停止服务端
	private static void stopServer() {
		if (serverThread == null || HTTPServer == null) return;
		HTTPServer.stop(0);
		serverThread = null;
		serverMap.clear();
		log(INFO, HEX_SYNC_NAME + "Server已关闭");
	}
	// 启动服务端
	private static void startServer() {
		serverThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Server正在启动...");
			initFiles(true);
			errorDownload = false; // 重置下载错误状态
			if (serverMap.isEmpty()) {
				log(WARNING, serverSyncDirectory + "没有文件,无法启动服务器");
				stopServer();
				return;
			}
			try {
				HTTPServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
				HTTPServer.createContext("/", HexSync::processRequest);
				HTTPServer.setExecutor(null);
				HTTPServer.start();
			} catch (IOException error) {
				log(SEVERE, HEX_SYNC_NAME + "Server无法启动: " + error.getMessage());
				return;
			}
			log(INFO, HEX_SYNC_NAME + "Server正在运行...端口号为: " + serverPort);
		});
		serverThread.start();
	}
	// 停止客户端
	private static void stopClient() {
		if (clientThread == null || HTTPURLConnection == null) return;
		HTTPURLConnection.disconnect();
		clientThread = null;
		log(INFO, HEX_SYNC_NAME + "Client已关闭");
		if (clientAutoStart && !errorDownload) exit(0);
	}
	// 启动客户端
	private static void startClient() {
		clientThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Client正在启动...");
			initFiles(false);
			Map<String, String> requestMap = requestFileSHAList();
			if (!requestMap.isEmpty()) {
				deleteFilesNotInMaps(requestMap, initFileSHAMap(clientOnlyDirectory)); // 删除多余文件
				download(makeToDownloadMap(requestMap, initFileSHAMap(clientSyncDirectory)));// 下载文件
				copyAllFiles(clientOnlyDirectory, clientSyncDirectory);// 复制仅客户端模组文件夹中的文件到客户端同步文件夹
			}
			stopClient();
		});
		clientThread.start();
	}
	// 删除同时不存在于服务端同步文件夹和仅客户端模组文件夹的文件
	private static void deleteFilesNotInMaps(Map<String, String> requestMap, Map<String, String> clientOnlyMap) {
		File[] fileList = new File(clientSyncDirectory).listFiles();
		if (fileList != null) for (File file : fileList) {
			String fileName = file.getName();
			if (!file.isFile() || requestMap.containsKey(fileName) || clientOnlyMap.containsKey(fileName)) continue;
			if (file.delete()) log(INFO, "已删除文件: " + fileName);
			else log(SEVERE, "删除文件失败: " + fileName);
		}
	}
	// 复制客户端仅同步文件夹中的文件到客户端同步文件夹
	private static void copyAllFiles(String source, String target) {
		File targetDirectory = new File(target);
		makeDirectory(valueOf(targetDirectory));
		File[] fileList = new File(source).listFiles();
		if (fileList != null) for (File file : fileList) {
			File targetFile = new File(targetDirectory, file.getName());
			// 递归复制子目录
			String fileAbsolutePath = file.getAbsolutePath();
			String targetFileAbsolutePath = targetFile.getAbsolutePath();
			if (file.isDirectory()) copyAllFiles(fileAbsolutePath, targetFileAbsolutePath);
			else {
				if (targetFile.exists()) continue;
				try (
						InputStream inputStream = newInputStream(file.toPath());
						OutputStream outputStream = newOutputStream(targetFile.toPath())
				) {
					byte[] buffer = new byte[16384];
					int length;
					while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
					log(INFO, "已复制: " + fileAbsolutePath + " 到 " + targetFileAbsolutePath);
				} catch (IOException error) {
					log(SEVERE, "复制" + fileAbsolutePath + "失败: " + error.getMessage());
				}
			}
		}
	}
	// 构建需要下载的文件列表
	private static Map<String, String> makeToDownloadMap(
			Map<String, String> requestMap,
			Map<String, String> clientMap
	) {
		Map<String, String> toDownloadMap = new HashMap<>();
		for (Map.Entry<String, String> entry : requestMap.entrySet()) {
			String fileName = entry.getKey();
			String SHA = entry.getValue();
			if (fileName.isEmpty() || SHA.isEmpty()) continue;
			if (!clientMap.containsKey(fileName) && !clientMap.containsValue(SHA) && checkNoFile(
					new File(clientSyncDirectory + separator + fileName), fileName, requestMap))
				toDownloadMap.put(fileName, SHA);
		}
		return toDownloadMap;
	}
	// 从服务端同步文件夹下载客户端缺少的文件
	private static void download(Map<String, String> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			log(INFO, "已是最新,无需下载.");
			if (HEADLESS || errorDownload) return;
			newJDialog(screenLength / 5, 0, "已是最新,无需下载.");
			return;
		}
		log(INFO, "开始下载 " + toDownloadMap.size() + " 个文件");
		int downloadedCount = 0;
		int toDownloadMapSize = toDownloadMap.size();
		JDialog progressDialog = null;
		if (!HEADLESS) progressDialog = createProgressDialog(toDownloadMapSize);
		for (Map.Entry<String, String> entry : toDownloadMap.entrySet()) {
			String filePath = clientSyncDirectory + separator + entry.getKey(); // 设置下载路径
			if (successDownloadFile(filePath, toDownloadMap)) {
				downloadedCount++; // 成功下载时增加计数
				log(INFO, "已下载: [" + downloadedCount + "/" + toDownloadMapSize + "] " + filePath);
				if (HEADLESS) continue;
				JProgressBar progressBar = (JProgressBar) progressDialog.getContentPane().getComponent(0);
				progressBar.setValue(downloadedCount);
			} else {
				log(SEVERE, "下载失败: " + filePath);
				errorDownload = true; // 记录下载失败
			}
		}
		if (progressDialog != null) progressDialog.dispose();
		if (!HEADLESS) newJDialog(
				screenLength / 5,
				0,
				(errorDownload ? "下载失败: " : "下载完成: ") + "[" + downloadedCount + "/" + toDownloadMapSize + "]"
		);
		log(INFO, "下载完成: [" + downloadedCount + "/" + toDownloadMapSize + "]");
		if (clientAutoStart) exit(0); // 自动退出
	}
	// 基础对话框框架
	private static JDialog newJDialog(int width, int height, String title) {
		JDialog dialog = new JDialog();
		dialog.setTitle(title);
		dialog.setIconImage(getImage());
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
		dialog.setSize(width, height);
		dialog.setLocationRelativeTo(null);
		return dialog;
	}
	// 基础选定按钮框架
	private static JCheckBox newJCheckBox(JPanel panel, String text, boolean selected) {
		JCheckBox checkBox = new JCheckBox("<html>" + text);
		checkBox.setFocusPainted(false);
		checkBox.setSelected(selected);
		panel.add(checkBox);
		return checkBox;
	}
	// 基础按钮框架
	private static void newJButton(JPanel panel, String text, ActionListener actionListener) {
		JButton button = new JButton("<html>" + text);
		button.setFocusPainted(false);
		button.setPreferredSize(new Dimension(screenLength / 15, screenLength / 60));
		button.addActionListener(actionListener);
		panel.add(button);
	}
	// 创建进度条对话框
	private static JDialog createProgressDialog(int totalFiles) {
		JDialog dialog = newJDialog(screenLength / 5, screenLength / 30, "下载进度");
		JProgressBar progressBar = new JProgressBar(0, totalFiles);
		progressBar.setStringPainted(true);
		progressBar.setForeground(Color.getColor("#008080"));
		progressBar.setBackground(Color.getColor("#D3D3D3"));
		progressBar.setFont(UIManager.getFont("Label.font").deriveFont(18f));
		dialog.add(progressBar, BorderLayout.CENTER);
		return dialog;
	}
	// 设置托盘图标
	private static void setSystemTray(JDialog dialog) {
		if (!SystemTray.isSupported()) return;
		TrayIcon trayIcon; // 托盘图标
		trayIcon = new TrayIcon(getImage(), HEX_SYNC_NAME);
		trayIcon.setImageAutoSize(true); // 自动调整图标大小
		trayIcon.setToolTip(HEX_SYNC_NAME);
		PopupMenu popupMenu = new PopupMenu();
		newMenuItem(popupMenu, "Open", event -> dialog.setVisible(true));
		popupMenu.addSeparator();
		newMenuItem(popupMenu, "Hide", event -> dialog.setVisible(false));
		popupMenu.addSeparator();
		newMenuItem(popupMenu, "Settings", event -> openSettingsDialog());
		popupMenu.addSeparator();
		newMenuItem(popupMenu, "About", event -> aboutButtonAction());
		popupMenu.addSeparator();
		newMenuItem(popupMenu, "Exit", event -> exit(0));
		trayIcon.setPopupMenu(popupMenu);
		trayIcon.addActionListener(event -> dialog.setVisible(true));
		try {
			SystemTray systemTray = SystemTray.getSystemTray();
			if (systemTray.getTrayIcons().length == 0) systemTray.add(trayIcon);
			else {
				TrayIcon existingIcon = systemTray.getTrayIcons()[0];
				existingIcon.setImage(getImage());
			}
		} catch (AWTException error) {
			log(SEVERE, "添加托盘图标失败: " + error.getMessage());
		}
	}
	// 获取图标
	private static Image getImage() {
		return Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource("icon.png"));
	}
	// 添加按钮，状态面板和托盘图标
	private static void addPanel(JDialog dialog) {
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panel.setLayout(new BorderLayout());
		textPane = new JTextPane();
		textPane.setContentType("text/html");
		textPane.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textPane);
		panel.add(scrollPane, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 5, 0));
		newJButton(buttonPanel, "设置", event -> openSettingsDialog());
		newJButton(buttonPanel, "启动服务端", event -> startServer());
		newJButton(buttonPanel, "启动客户端", event -> startClient());
		newJButton(buttonPanel, "停止服务端", event -> stopServer());
		newJButton(buttonPanel, "停止客户端", event -> stopClient());
		newJButton(buttonPanel, "退出", event -> exit(0));
		panel.add(buttonPanel, BorderLayout.SOUTH);
		dialog.add(panel);
		setSystemTray(dialog);
	}
	// 辅助方法创建菜单项
	private static void newMenuItem(PopupMenu popupMenu, String text, ActionListener actionListener) {
		MenuItem menuItem = new MenuItem("<html>" + text);
		menuItem.addActionListener(actionListener);
		popupMenu.add(menuItem);
	}
	// 打开设置对话框
	private static void openSettingsDialog() {
		loadConfig();
		JDialog settingsDialog = newJDialog(screenLength / 5, screenLength / 8, "设置");
		JPanel settingsPanel = new JPanel(new BorderLayout(5, 5));
		settingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// 服务端选项卡
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		JTextField[] serverTextFields = new JTextField[3];
		JComboBox<String> serverUploadRateLimitUnitBox = new JComboBox<>(new String[]{"B/s", "KB/s", "MB/s", "GB/s"});
		serverUploadRateLimitUnitBox.setSelectedItem(serverUploadRateLimitUnit);
		int index = 0;
		for (String[] labelAndField : formatHtmlStrings(new String[][]{
				{"端口号:", valueOf(serverPort)},
				{"上传速率限制(0为无限制):", valueOf(serverUploadRateLimit)},
				{"服务端同步文件夹路径:", serverSyncDirectory}
		})) {
			serverPanel.add(new JLabel(labelAndField[0]));
			JTextField textField = new JTextField(labelAndField[1]);
			serverTextFields[index] = textField;
			if (labelAndField[0].startsWith("上传速率限制(0为无限制):")) {
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
		String[][] clientLabelsAndFields = formatHtmlStrings(new String[][]{
				{"端口号:", valueOf(clientPort)},
				{"服务器地址:", serverAddress},
				{"客户端同步文件夹路径:", clientSyncDirectory},
				{"仅客户端模组文件夹路径:", clientOnlyDirectory}
		});
		// 使用普通计数器来填充数组
		for (int i = 0; i < clientLabelsAndFields.length; i++) {
			String[] labelAndField = clientLabelsAndFields[i];
			clientPanel.add(new JLabel(labelAndField[0]));
			JTextField textField = new JTextField(labelAndField[1]);
			clientTextFields[i] = textField; // 直接根据索引分配
			clientPanel.add(textField);
		}
		// 获取文本框实例
		JTextField clientPortField = clientTextFields[0];
		JTextField serverAddressField = clientTextFields[1];
		JTextField clientSyncDirectoryPathField = clientTextFields[2];
		JTextField clientOnlyDirectoryPathField = clientTextFields[3];
		// 添加选项卡到选项卡面板
		JCheckBox serverAutoStartBox = newJCheckBox(serverPanel, "自动启动服务端", serverAutoStart);
		JCheckBox clientAutoStartBox = newJCheckBox(clientPanel, "自动启动客户端", clientAutoStart);
		// 添加选项卡面板到设置对话框
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.addTab("<html>服务端设置", serverPanel);
		tabbedPane.addTab("<html>客户端设置", clientPanel);
		tabbedPane.setFocusable(false);
		settingsPanel.add(tabbedPane, BorderLayout.CENTER);
		// 按钮面板
		JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
		newJButton(
				buttonPanel, "保存", event -> {
					// 定义输入框数组及其对应的提示信息和选项卡索引
					Object[][] inputs = formatHtmlObjects(new Object[][]{
							{"服务端端口", serverPortField, 0},
							{"上传速率限制", serverUploadRateLimitField, 0},
							{"服务端同步文件夹路径", serverSyncDirectoryPathField, 0},
							{"客户端端口", clientPortField, 1},
							{"服务器地址", serverAddressField, 1},
							{"客户端同步文件夹路径", clientSyncDirectoryPathField, 1},
							{"仅客户端模组文件夹路径", clientOnlyDirectoryPathField, 1}
					});
					// 检查输入框是否为空
					for (Object[] input : inputs) {
						JTextField textField = (JTextField) input[1];
						if (textField.getText().trim().isEmpty()) {
							tabbedPane.setSelectedIndex((int) input[2]); // 跳转到对应的选项卡
							selectAndFocus(textField);
							log(WARNING, input[0] + "不能为空");
							return;
						}
					}
					// 检测输入框是否为数字且在合法范围内并尝试转换
					if (!getPort(serverPortField.getText().trim(), true)) selectAndFocus(serverPortField);
					if (!getPort(clientPortField.getText().trim(), false)) selectAndFocus(clientPortField);
					// 检测上传速率上限
					String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
					if (invalidLong(uploadRateLimitText) || parseLong(uploadRateLimitText) < 0) {
						log(WARNING, "上传速率上限不正确: " + uploadRateLimitText);
						tabbedPane.setSelectedIndex(0);
						selectAndFocus(serverUploadRateLimitField);
						return;
					}
					serverAutoStart = serverAutoStartBox.isSelected();
					serverUploadRateLimit = parseLong(uploadRateLimitText);
					serverUploadRateLimitUnit = (String) serverUploadRateLimitUnitBox.getSelectedItem();
					serverSyncDirectory = serverSyncDirectoryPathField.getText().trim();
					clientAutoStart = clientAutoStartBox.isSelected();
					serverAddress = serverAddressField.getText().trim();
					clientSyncDirectory = clientSyncDirectoryPathField.getText().trim();
					clientOnlyDirectory = clientOnlyDirectoryPathField.getText().trim();
					saveConfig(); // 保存配置
					settingsDialog.dispose(); // 关闭对话框
				}
		);
		newJButton(buttonPanel, "取消", event -> settingsDialog.dispose());
		newJButton(buttonPanel, "关于", event -> aboutButtonAction());
		settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
		settingsDialog.add(settingsPanel);
	}
	private static String[][] formatHtmlStrings(String[][] strings) {
		for (int i = 0; i < strings.length; i++)
			strings[i][0] = "<html>" + strings[i][0];
		return strings;
	}
	private static Object[][] formatHtmlObjects(Object[][] objects) {
		for (int i = 0; i < objects.length; i++)
			objects[i][0] = "<html>" + objects[i][0];
		return objects;
	}
	// 聚焦并全选输入框
	private static void selectAndFocus(JTextField textField) {
		textField.requestFocus(); // 聚焦输入框
		textField.selectAll(); // 选中输入框
	}
	// 处理请求
	private static void processRequest(HttpExchange exchange) {
		if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) return;
		String requestURI = exchange.getRequestURI().getPath();
		log(INFO, "收到请求: " + requestURI);
		byte[] responseBytes = "".getBytes();
		int responseCode = HTTP_NOT_FOUND;
		if ("/list".equals(requestURI)) {
			StringBuilder responseBuilder = new StringBuilder(); // 用于构建响应内容
			for (Map.Entry<String, String> entry : serverMap.entrySet()) {
				String fileName = entry.getKey();
				String SHA = entry.getValue();
				responseBuilder.append(fileName).append(lineSeparator()).append(SHA).append(lineSeparator());
			}
			responseBytes = responseBuilder.toString().getBytes();
			responseCode = HTTP_OK;
		} else if (requestURI.startsWith("/download/")) {
			String requestSHA = requestURI.substring(requestURI.lastIndexOf("/") + 1);
			String filePath = null;
			for (Map.Entry<String, String> entry : serverMap.entrySet())
				if (requestSHA.equals(entry.getValue())) {
					filePath = serverSyncDirectory + separator + entry.getKey();
					break;
				}
			if (filePath == null) {
				log(SEVERE, "无法找到对应的文件: " + requestSHA);
				sendResponse(exchange, responseBytes, responseCode);
				return;
			}
			File file = new File(filePath); // 构造文件对象
			if (!serverMap.containsValue(requestSHA) || !file.exists() || !file.isFile()) {
				log(SEVERE, "文件不存在或请求的校验码不正确: " + filePath);
				sendResponse(exchange, responseBytes, HTTP_NOT_FOUND);
				return;
			}
			try (InputStream inputStream = newInputStream(file.toPath())) {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[16384];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) byteArrayOutputStream.write(buffer, 0, bytesRead);
				responseBytes = byteArrayOutputStream.toByteArray();
			} catch (IOException error) {
				log(SEVERE, "读取文件时发生错误: " + error.getMessage());
			}
			encode(exchange, file);
			responseCode = HTTP_OK; // 发送成功,返回200
			log(INFO, "发送文件: " + filePath);
		} else log(WARNING, "未知的请求: " + requestURI);
		sendResponse(exchange, responseBytes, responseCode);
	}
	// 发送数据
	private static void sendResponse(HttpExchange exchange, byte[] responseBytes, int responseCode) {
		new Thread(() -> {
			if (responseBytes == null) return;
			int responseBytesLength = responseBytes.length;
			long maxUploadRateInBytes = convertToBytes(serverUploadRateLimit, serverUploadRateLimitUnit);
			long lastFillTime = currentTimeMillis(); // 最近一次填充时间
			try (OutputStream outputStream = exchange.getResponseBody()) {
				exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 设置Content-Type
				exchange.sendResponseHeaders(responseCode, responseBytesLength); // 设置响应头
				int totalBytesSent = 0; // 记录已发送字节数
				if (serverUploadRateLimit == 0) {
					outputStream.write(responseBytes); // 无限制发送数据
					return;
				}
				while (totalBytesSent < responseBytesLength) {
					long currentTime = currentTimeMillis();
					AVAILABLE_TOKENS.addAndGet((currentTime - lastFillTime) * maxUploadRateInBytes / 1000);
					lastFillTime = currentTime; // 更新时间
					// 尝试发送数据
					int bytesToSend = min(16384, responseBytesLength - totalBytesSent);
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
		}).start();
	}
	// 单位转换方法
	private static long convertToBytes(long value, String unit) {
		try {
			switch (unit) {
				case "B/s":
					return value;
				case "KB/s":
					return multiplyExact(value, 1024);
				case "MB/s":
					return multiplyExact(value, 1048576);
				case "GB/s":
					return multiplyExact(value, 1073741824);
				default:
					log(WARNING, "未知的上传速率单位: " + unit);
					return 0;
			}
		} catch (ArithmeticException error) {
			log(SEVERE, "上传速率溢出，自动转化为无限制: " + error.getMessage());
			return 0; // 溢出
		}
	}
	// 编码文件名
	private static void encode(HttpExchange exchange, File file) {
		try {
			String encodedName = URLEncoder.encode(file.getName(), "UTF-8").replace("+", "%20");
			exchange.getResponseHeaders().set(
					"Content-Disposition",
					"attachment; filename=\"" + encodedName + "\"; " + "filename*=UTF-8''" + encodedName
			);
		} catch (UnsupportedEncodingException error) {
			log(SEVERE, "编码文件名时出错: " + error.getMessage());
		}
	}
}