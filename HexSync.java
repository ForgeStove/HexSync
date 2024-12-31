import com.sun.net.httpserver.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.zip.CRC32;

import static java.io.File.separator;
import static java.lang.Math.*;
import static java.lang.System.*;
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
	private static final boolean ANSI = getProperty("ansi", "true").equalsIgnoreCase("false"); // 是否启用ANSI控制台输出
	private static final boolean LOG = getProperty("log", "true").equalsIgnoreCase("true"); // 是否记录日志
	private static ExecutorService logExecutor; // 日志记录线程池
	private static FileWriter logWriter; // 日志记录器
	private static Image icon; // 程序图标
	private static JFrame frame; // 主窗口
	private static String serverSyncDirectory = "mods"; // 服务端同步文件夹路径，默认值mods
	private static String clientSyncDirectory = "mods"; // 客户端同步文件夹路径，默认值mods
	private static String clientOnlyDirectory = "clientOnlyMods"; // 仅客户端文件夹路径，默认值clientOnlyMods
	private static String serverUploadRateLimitUnit = "MB"; // 上传速率限制单位，默认MB
	private static String serverAddress = "localhost"; // 服务器地址，默认值localhost
	private static Map<String, Long> serverMap; // 存储服务端文件名和对应的校验码数据
	private static HttpServer HTTPServer; // 存储服务器实例
	private static HttpURLConnection HTTPURLConnection; // 存储客户端连接实例
	private static Thread serverThread; // 服务器线程
	private static Thread clientThread; // 客户端线程
	private static JTextPane textPane; // 日志面板
	private static boolean errorDownload; // 客户端下载文件时是否发生错误，影响客户端是否自动关闭
	private static boolean serverAutoStart; // 服务端自动启动，默认不自动启动
	private static boolean clientAutoStart; // 客户端自动启动，默认不自动启动
	private static int screenLength; // 屏幕长边
	private static int serverPort = 65535; // 服务端端口，默认值65535
	private static int clientPort = 65535; // 客户端端口，默认值65535
	private static long serverUploadRateLimit = 1; // 上传速率限制值，默认限速1MB
	private static long maxUploadRateInBytes; // 上传速率限制值对应的字节数
	public static void main(String[] args) {
		initLog();
		loadConfig();
		if (serverAutoStart) startServer();
		if (clientAutoStart) startClient();
		if (HEADLESS) headlessUI();
		else normalUI();
	}
	// 初始化日志
	private static void initLog() {
		try {
			makeDirectory(HEX_SYNC_NAME);
			logWriter = new FileWriter(LOG_PATH, false);
		} catch (IOException error) {
			err.println("日志初始化失败: " + error.getMessage());
		}
		if (LOG) logExecutor = Executors.newSingleThreadExecutor();
	}
	// 加载配置文件
	private static void loadConfig() {
		File configFile = new File(CONFIG_PATH);
		if (!configFile.isFile()) {
			saveConfig();
			return;
		}
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile))) {
			Map<String, Consumer<String>> configMap = new HashMap<>();
			configMap.put(SERVER_PORT, input -> serverPort = Integer.parseInt(input));
			configMap.put(SERVER_UPLOAD_RATE_LIMIT, HexSync::setRate);
			configMap.put(SERVER_SYNC_DIRECTORY, input -> serverSyncDirectory = input);
			configMap.put(SERVER_AUTO_START, input -> serverAutoStart = Boolean.parseBoolean(input));
			configMap.put(CLIENT_PORT, input -> clientPort = Integer.parseInt(input));
			configMap.put(SERVER_ADDRESS, input -> serverAddress = input);
			configMap.put(CLIENT_SYNC_DIRECTORY, input -> clientSyncDirectory = input);
			configMap.put(CLIENT_ONLY_DIRECTORY, input -> clientOnlyDirectory = input);
			configMap.put(CLIENT_AUTO_START, input -> clientAutoStart = Boolean.parseBoolean(input));
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (!line.matches("^[a-zA-Z].*")) continue; // 仅当首字符不是字母时跳过
				String[] parts = line.split("=", 2);
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
	// 日志记录
	private static void log(String level, String message) {
		if (LOG) logExecutor.submit(() -> {
			try {
				String formattedLog = String.format(
						"%s [%s] %s%n",
						new SimpleDateFormat("[HH:mm:ss]").format(new Date()),
						level,
						message
				);
				logWriter.write(formattedLog);
				logWriter.flush();
				boolean info = level.equals(INFO);
				boolean warning = level.equals(WARNING);
				boolean severe = level.equals(SEVERE);
				if (ANSI) out.print(formattedLog);
				else out.printf(
						"%s%s\u001B[0m",
						info ? "\u001B[32m" : warning ? "\u001B[33m" : severe ? "\u001B[31m" : "\u001B[0m",
						formattedLog
				);
				if (!HEADLESS) SwingUtilities.invokeLater(() -> {
					SimpleAttributeSet attributeSet = new SimpleAttributeSet();
					StyleConstants.setForeground(
							attributeSet,
							info
									? new Color(0, 128, 0)
									: warning ? new Color(255, 165, 0) : severe ? new Color(255, 0, 0) : Color.BLACK
					);
					Document document = textPane.getDocument();
					try {
						while (document.getDefaultRootElement().getElementCount() > 128) {
							Element element = document.getDefaultRootElement().getElement(0);
							int lineStart = element.getStartOffset();
							document.remove(lineStart, element.getEndOffset() - lineStart); // 删除第一行
						}
						document.insertString(document.getLength(), formattedLog, attributeSet);
					} catch (BadLocationException error) {
						throw new RuntimeException(error);
					}
				});
			} catch (IOException error) {
				if (logWriter == null) initLog();
				else err.println("无法写入日志: " + error.getMessage());
			}
		});
	}
	// 无头模式UI
	private static void headlessUI() {
		out.println("欢迎使用" + HEX_SYNC_NAME + "!");
		out.println("输入HELP以获取帮助.");
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", HexSync::startServer);
		map.put("RC", HexSync::startClient);
		map.put("SS", HexSync::stopServer);
		map.put("SC", HexSync::stopClient);
		map.put("SET", HexSync::headlessSettings);
		map.put("GITHUB", () -> out.println(GITHUB_URL));
		map.put(
				"HELP", () -> printlnStrings(new String[]{
						"RS     |启动服务端",
						"RC     |启动客户端",
						"SS     |停止服务端",
						"SC     |停止客户端",
						"SET    |设置",
						"GITHUB |仓库",
						"EXIT   |退出",
						"HELP   |帮助"
				})
		);
		map.put("EXIT", () -> exit(0));
		Scanner scanner = new Scanner(in);
		while (true) try {
			out.print(HEX_SYNC_NAME + ">");
			String line = scanner.nextLine().toUpperCase();
			if (line.isEmpty()) continue;
			map.getOrDefault(line, () -> err.println("无效命令,输入HELP以获取帮助.")).run();
		} catch (Exception error) {
			log(SEVERE, "命令处理时出错: " + error.getMessage());
			break;
		}
	}
	// 有头模式UI
	private static void normalUI() {
		SwingUtilities.invokeLater(() -> {
			try {
				icon = Toolkit.getDefaultToolkit().getImage(HexSync.class.getResource("icon.png"));
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
				screenLength = max(size.width, size.height);
				// 添加按钮，状态面板和托盘图标
				JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				panel.setLayout(new BorderLayout(5, 5));
				textPane = new JTextPane();
				textPane.setEditable(false);
				textPane.setOpaque(false);
				panel.add(new JScrollPane(textPane), BorderLayout.CENTER);
				frame = new JFrame(HEX_SYNC_NAME); // 主窗口
				frame.setAlwaysOnTop(true);
				JPanel buttonPanel = new JPanel(new GridLayout(2, 3));
				newJButton(buttonPanel, "设置", event -> settingsJDialog());
				newJButton(buttonPanel, "启动服务端", event -> startServer());
				newJButton(buttonPanel, "启动客户端", event -> startClient());
				newJButton(buttonPanel, "停止服务端", event -> stopServer());
				newJButton(buttonPanel, "停止客户端", event -> stopClient());
				newJButton(buttonPanel, "退出", event -> exit(0));
				panel.add(buttonPanel, BorderLayout.SOUTH);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(panel);
				frame.setSize(new Dimension(screenLength / 3, screenLength / 4));
				setWindow(frame);
			} catch (Exception error) {
				log(SEVERE, "初始化UI时出错:" + error.getMessage());
			}
		});
	}
	// 初始化文件
	private static void initFiles(boolean isServer) {
		makeDirectory(isServer ? serverSyncDirectory : clientSyncDirectory);
		makeDirectory(HEX_SYNC_NAME);
		loadConfig();
		if (isServer) serverMap = initMap(serverSyncDirectory);
		else {
			makeDirectory(clientOnlyDirectory);
			errorDownload = false;
		}
	}
	// 初始化文件名校验码键值对表
	private static Map<String, Long> initMap(String directory) {
		Map<String, Long> map = new HashMap<>();
		File[] fileList = new File(directory).listFiles(); // 获取文件夹下的所有文件
		if (fileList != null) for (File file : fileList)
			if (file.isFile()) map.put(file.getName(), calculateCRC(file));
		return map;
	}
	// 创建文件夹
	private static void makeDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		if (directory.isDirectory()) return;
		if (directory.mkdirs()) log(INFO, "文件夹已创建: " + directoryPath);
		else log(SEVERE, "无法创建文件夹: " + directoryPath);
	}
	// 地址格式化,转换为HTTP协议
	private static String formatHTTP(String address) {
		if (address.endsWith("/")) address = address.substring(0, address.length() - 1); // 去除末尾的分隔符
		return address.startsWith("http://") ? address : "http://" + address; // 添加HTTP协议头
	}
	// 计算文件校验码
	private static long calculateCRC(File file) {
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
	// 从服务器请求文件名和校验码列表
	private static Map<String, Long> requestFileCRCList() {
		String URL = formatHTTP(serverAddress) + ":" + clientPort + "/list"; // 服务器地址
		log(INFO, "正在连接到: " + URL); // 记录请求开始日志
		Map<String, Long> requestMap = new HashMap<>(); // 复制请求列表
		try {
			int responseCode = getResponseCode(new URL(URL));
			if (responseCode != HttpURLConnection.HTTP_OK) {
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
				requestMap.put(fileName, Long.parseLong(bufferedReader.readLine())); // 将文件名与校验码放入Map
			}
		} catch (IOException error) {
			log(SEVERE, "读取响应时出错: " + error.getMessage());
			errorDownload = true;
		}
		log(INFO, "获取到 [" + requestMap.size() + "] 个文件"); // 记录请求成功日志
		return requestMap;
	}
	// 从服务器下载文件
	private static boolean successDownloadFile(String filePath, Map<String, Long> toDownloadMap) {
		if (clientThread == null) return false; // 客户端线程已关闭
		File clientFile = new File(filePath); // 目标本地文件
		Long requestCRC = toDownloadMap.get(filePath.substring(clientSyncDirectory.length() + 1));
		if (requestCRC == null) {
			log(SEVERE, "无法获取请求的校验码: " + clientFile);
			return false;
		}
		try {
			int responseCode = getResponseCode(new URL(String.format(
					"%s:%d/download/%s",
					formatHTTP(serverAddress),
					clientPort,
					requestCRC
			)));
			if (responseCode != HttpURLConnection.HTTP_OK) {
				log(SEVERE, "下载失败,错误代码: " + responseCode);
				return false;
			}
		} catch (IOException error) {
			log(SEVERE, "无法连接至服务器: " + error.getMessage());
			return false;
		}
		// 读取输入流并写入本地文件
		try (
				InputStream inputStream = HTTPURLConnection.getInputStream();
				FileOutputStream outputStream = new FileOutputStream(clientFile)
		) {
			byte[] buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, bytesRead);
		} catch (IOException error) {
			log(SEVERE, "读取响应时出错: " + error.getMessage());
			return false;
		}
		// 校验下载的文件
		if (!requestCRC.equals(calculateCRC(clientFile))) {
			log(SEVERE, "校验失败,文件可能已损坏: " + clientFile);
			if (!clientFile.delete()) log(SEVERE, "无法删除损坏的文件: " + clientFile);
			return false;
		}
		return true; // 下载成功且校验通过
	}
	// 获取响应码
	private static int getResponseCode(URL requestURL) throws IOException {
		HTTPURLConnection = (HttpURLConnection) requestURL.openConnection(); // 打开连接
		HTTPURLConnection.setRequestMethod("GET"); // 设置请求方式为GET
		HTTPURLConnection.setConnectTimeout(5000); // 设置连接超时为5秒
		HTTPURLConnection.setReadTimeout(5000); // 设置读取超时为5秒
		return HTTPURLConnection.getResponseCode(); // 返回响应码
	}
	// 保存配置
	private static void saveConfig() {
		String[][] configEntries = {
				{"# 服务端配置"},
				{SERVER_PORT, String.valueOf(serverPort)},
				{SERVER_UPLOAD_RATE_LIMIT, serverUploadRateLimit + " " + serverUploadRateLimitUnit},
				{SERVER_SYNC_DIRECTORY, serverSyncDirectory},
				{SERVER_AUTO_START, String.valueOf(serverAutoStart)},
				{"# 客户端配置"},
				{CLIENT_PORT, String.valueOf(clientPort)},
				{SERVER_ADDRESS, serverAddress},
				{CLIENT_SYNC_DIRECTORY, clientSyncDirectory},
				{CLIENT_ONLY_DIRECTORY, clientOnlyDirectory},
				{CLIENT_AUTO_START, String.valueOf(clientAutoStart)}
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
		try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(configFile))) {
			bufferedWriter.write(configContent.toString());// 写入配置文件
			log(INFO, "配置已保存: " + lineSeparator() + configContent);
		} catch (IOException error) {
			log(SEVERE, "配置保存失败: " + error.getMessage());
		}
	}
	// 字符串转端口
	private static boolean getPort(String portInput, boolean isServer) {
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
	// 检测数字输入是否不在Long范围内
	private static boolean invalidLong(String numberInput) {
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
	// 关于
	private static void aboutJDialog(Window parent) {
		if (checkJDialog("关于")) return;
		JDialog aboutDialog = new JDialog(parent, "关于");
		JTextPane aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("<span style=\"font-weight: bold;font-family: Arial;\">"
				+ HEX_SYNC_NAME
				+ "<br>By: ForgeStove<br>GitHub: <a href=\""
				+ GITHUB_URL
				+ "\">"
				+ GITHUB_URL
				+ "</a><br>开源许可: <a href=\"file:LICENSE\">GNU General Public License v3.0</a></span>");
		aboutTextPane.addHyperlinkListener(event -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) try {
				String url = event.getURL().toString();
				if (url.equals(GITHUB_URL)) {
					Desktop.getDesktop().browse(event.getURL().toURI());
				} else if (url.equals("file:LICENSE")) {
					if (checkJDialog("许可证")) return;
					try (
							BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
									HexSync.class.getResourceAsStream("LICENSE"))))
					) {
						StringBuilder licenseContent = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) licenseContent.append(line).append(lineSeparator());
						JTextArea licenseTextArea = new JTextArea(licenseContent.toString());
						licenseTextArea.setEditable(false);
						licenseTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						JDialog licenseJDialog = new JDialog(aboutDialog, "许可证");
						licenseJDialog.add(new JScrollPane(licenseTextArea));
						licenseJDialog.pack();
						setWindow(licenseJDialog);
					}
				}
			} catch (Exception error) {
				log(WARNING, "无法打开超链接: " + error.getMessage());
			}
		});
		aboutDialog.add(new JScrollPane(aboutTextPane));
		aboutDialog.pack();
		setWindow(aboutDialog);
	}
	// 检测是否有同名窗口并显示
	private static boolean checkJDialog(String title) {
		for (Window window : Window.getWindows()) {
			if (!(window instanceof JDialog)) continue;
			JDialog dialog = (JDialog) window;
			if (!dialog.getTitle().equals(title)) continue;
			dialog.setVisible(true);
			dialog.toFront();
			return true;
		}
		return false;
	}
	// 无头模式设置
	private static void headlessSettings() {
		Map<String, Consumer<String[]>> map = new HashMap<>();
		map.put("SP", args -> getPort(args[1], true));
		map.put("SL", args -> setRate(args[1] + " " + args[2]));
		map.put("SD", args -> setDirectory(args[1], "服务端同步", value -> serverSyncDirectory = value));
		map.put("SS", args -> setAutoStart(args[1], true, value -> serverAutoStart = value));
		map.put("CP", args -> getPort(args[1], false));
		map.put(
				"SA", args -> {
					serverAddress = args[1];
					out.println("服务器地址已设置为: " + serverAddress);
				}
		);
		map.put("CD", args -> setDirectory(args[1], "客户端同步", value -> clientSyncDirectory = value));
		map.put("CO", args -> setDirectory(args[1], "仅客户端模组", value -> clientOnlyDirectory = value));
		map.put("CS", args -> setAutoStart(args[1], false, value -> clientAutoStart = value));
		map.put("SAVE", args -> saveConfig());
		map.put(
				"HELP", args -> printlnStrings(new String[]{
						"SP [端口号]            |设置服务端端口",
						"SL [速率] [B/KB/MB/GB] |设置服务端最大上传速率",
						"SD [目录]              |设置服务端同步目录",
						"SS [Y/N]               |设置服务端自动启动",
						"CP [端口号]            |设置客户端端口",
						"SA [地址]              |设置服务器地址",
						"CD [目录]              |设置客户端同步目录",
						"CO [目录]              |设置客户端仅客户端目录",
						"CS [Y/N]               |设置客户端自动启动",
						"SAVE                   |保存并退出设置",
						"EXIT                   |退出设置而不保存",
						"HELP                   |帮助"
				})
		);
		out.println("进入设置模式,输入命令或输入HELP以获取帮助.");
		Scanner scanner = new Scanner(in);
		while (true) try {
			out.print(HEX_SYNC_NAME + "Settings>");
			String[] parts = scanner.nextLine().split("\\s+");
			if (parts.length == 0) continue;
			if (parts[0].equalsIgnoreCase("EXIT")) break;
			map.getOrDefault(parts[0].toUpperCase(), args -> err.println("无效命令,输入HELP以获取帮助.")).accept(parts);
			if (parts[0].equalsIgnoreCase("SAVE")) break;
		} catch (Exception error) {
			err.println("无效命令,输入HELP以获取帮助.");
		}
	}
	// 输出帮助信息的通用方法
	private static void printlnStrings(String[] messages) {
		for (String message : messages) out.println(message);
	}
	// 设置最大上传速率
	private static void setRate(String input) {
		String[] parts = input.split("\\s+");
		if (input.matches("\\d+(\\s+B|\\s+KB|\\s+MB|\\s+GB)") && !invalidLong(parts[0])) {
			serverUploadRateLimit = Long.parseLong(parts[0]);
			serverUploadRateLimitUnit = parts[1];
		} else if (HEADLESS) err.println("速率格式错误");
	}
	// 设置文件夹路径
	private static void setDirectory(String directory, String log, Consumer<String> consumer) {
		if (!directory.isEmpty() && !directory.contains(separator)) {
			consumer.accept(directory);
			out.println(log + "文件夹路径已设置为: " + directory);
		} else err.println("路径格式错误,请输入绝对路径或相对路径.");
	}
	// 设置自动启动
	private static void setAutoStart(String input, Boolean isServer, Consumer<Boolean> consumer) {
		if (input.matches("[yYnN]")) {
			boolean value = input.matches("[yY]");
			consumer.accept(value);
			out.println(isServer ? "服务端" : "客户端" + "自动启动已设置为: " + value);
		} else err.println("无效输入,请输入Y/N.");
	}
	// 停止服务端
	private static void stopServer() {
		if (serverThread == null || HTTPServer == null) return;
		serverMap.clear();
		HTTPServer.stop(0);
		serverThread = null;
		log(INFO, HEX_SYNC_NAME + "Server已关闭");
	}
	// 启动服务端
	private static void startServer() {
		if (serverThread != null) return;
		serverThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Server正在启动...");
			initFiles(true);
			if (serverMap.isEmpty()) {
				log(WARNING, serverSyncDirectory + "没有文件,无法启动服务器");
				stopServer();
				return;
			}
			try {
				ExecutorService executorService = Executors.newFixedThreadPool(8);
				maxUploadRateInBytes = convertToBytes(serverUploadRateLimit, serverUploadRateLimitUnit);
				HTTPServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
				HTTPServer.setExecutor(executorService);
				HTTPServer.createContext("/", exchange -> executorService.submit(() -> processRequest(exchange)));
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
		if (clientThread != null) return;
		clientThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Client正在启动...");
			initFiles(false);
			Map<String, Long> requestMap = requestFileCRCList();
			if (!requestMap.isEmpty()) {
				deleteFilesNotInMaps(requestMap, initMap(clientOnlyDirectory)); // 删除多余文件
				Map<String, Long> clientMap = initMap(clientSyncDirectory); // 初始化客户端文件列表
				requestMap.entrySet().removeIf(entry -> clientMap.containsValue(entry.getValue()));
				downloadMissingFiles(requestMap);// 下载文件
				copyDirectory(clientOnlyDirectory, clientSyncDirectory);// 复制仅客户端模组文件夹中的文件到客户端同步文件夹
			}
			stopClient();
		});
		clientThread.start();
	}
	// 删除指定路径下的文件
	private static void deleteFilesNotInMaps(Map<String, Long> requestMap, Map<String, Long> clientOnlyMap) {
		File[] fileList = new File(clientSyncDirectory).listFiles();
		if (fileList != null) for (File file : fileList)
			if (file.isFile()) {
				long CRC = calculateCRC(file);
				if (requestMap.containsValue(CRC) || clientOnlyMap.containsValue(CRC)) continue;
				if (file.delete()) log(INFO, "已删除文件: " + file);
				else log(SEVERE, "删除文件失败: " + file);
			}
	}
	private static void copyDirectory(String source, String target) {
		makeDirectory(target);
		File[] fileList = new File(source).listFiles();
		if (fileList == null) return;
		try {
			for (File file : fileList) {
				String targetFileName = file.getName();
				File targetFile = new File(target, targetFileName);
				if (new File(target, targetFileName + ".disable").exists()) continue; // 跳过此文件
				if (file.isDirectory()) {
					copyDirectory(String.valueOf(file), String.valueOf(targetFile));
				} else if (!targetFile.exists()) {
					Files.copy(file.toPath(), targetFile.toPath());
					log(INFO, "已复制: " + file + " -> " + targetFile);
				}
			}
		} catch (IOException error) {
			log(SEVERE, "复制失败: " + error.getMessage());
		}
	}
	// 从服务端同步文件夹下载客户端缺少的文件
	private static void downloadMissingFiles(Map<String, Long> toDownloadMap) {
		if (toDownloadMap.isEmpty()) {
			log(INFO, "模组已经是最新版本");
			return;
		}
		log(INFO, "开始下载 " + toDownloadMap.size() + " 个文件");
		int count = 0;
		int toDownloadMapSize = toDownloadMap.size();
		for (Map.Entry<String, Long> entry : toDownloadMap.entrySet()) {
			String filePath = clientSyncDirectory + separator + entry.getKey(); // 设置下载路径
			if (successDownloadFile(filePath, toDownloadMap)) {
				count++; // 成功下载时增加计数
				log(INFO, "已下载: [" + count + "/" + toDownloadMapSize + "] " + filePath);
			} else {
				log(SEVERE, "下载失败: " + filePath);
				errorDownload = true; // 记录下载失败
			}
		}
		log(
				INFO, (
						errorDownload ? "下载失败" : "下载完成"
				) + ": [" + count + "/" + toDownloadMapSize + "]"
		);
		if (clientAutoStart) exit(0); // 自动退出
	}
	// 基础复选框框架
	private static JCheckBox newJCheckBox(JPanel panel, String text, boolean selected) {
		JCheckBox checkBox = new JCheckBox(text);
		checkBox.setFocusPainted(false);
		checkBox.setSelected(selected);
		panel.add(checkBox);
		return checkBox;
	}
	// 基础按钮框架
	private static void newJButton(JPanel panel, String text, ActionListener actionListener) {
		JButton button = new JButton("<html>" + text);
		button.setFocusPainted(false);
		button.setPreferredSize(new Dimension(0, screenLength / 55));
		button.addActionListener(actionListener);
		panel.add(button);
	}
	// 设置窗口属性
	private static void setWindow(Window window) {
		setFont(window, new Font("Arial", Font.PLAIN, 14));
		window.setIconImage(icon);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}
	// 打开设置对话框
	private static void settingsJDialog() {
		if (checkJDialog("设置")) return;
		loadConfig();
		// 设置对话框
		JDialog settingsJDialog = new JDialog(frame, "设置");
		JPanel settingsPanel = new JPanel(new BorderLayout());
		settingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// 选项卡面板
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setFocusable(false);
		settingsPanel.add(tabbedPane, BorderLayout.CENTER);
		// 服务端选项卡
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		serverPanel.add(new JLabel("<html>端口号:"));
		JTextField serverPortField = new JTextField(String.valueOf(serverPort));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel("<html>最大上传速率:"));
		JTextField serverUploadRateLimitField = new JTextField(String.valueOf(serverUploadRateLimit));
		serverPanel.add(serverUploadRateLimitField);
		serverPanel.add(new JLabel("<html>上传速率单位(每秒):"));
		JComboBox<String> serverUploadRateLimitUnitBox = new JComboBox<>(new String[]{"B", "KB", "MB", "GB"});
		serverUploadRateLimitUnitBox.setFocusable(false);
		serverUploadRateLimitUnitBox.setSelectedItem(serverUploadRateLimitUnit);
		serverPanel.add(serverUploadRateLimitUnitBox);
		serverPanel.add(new JLabel("<html>服务端同步路径:"));
		JTextField serverSyncDirectoryField = new JTextField(serverSyncDirectory);
		serverPanel.add(serverSyncDirectoryField);
		JCheckBox serverAutoStartBox = newJCheckBox(serverPanel, "<html>自动启动服务端", serverAutoStart);
		tabbedPane.addTab("<html>服务端设置", serverPanel);
		// 客户端选项卡
		JPanel clientPanel = new JPanel(new GridLayout(5, 2));
		clientPanel.add(new JLabel("<html>端口号:"));
		JTextField clientPortField = new JTextField(String.valueOf(clientPort));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel("<html>服务器地址:"));
		JTextField serverAddressField = new JTextField(serverAddress);
		clientPanel.add(serverAddressField);
		clientPanel.add(new JLabel("<html>客户端同步路径:"));
		JTextField clientSyncDirectoryField = new JTextField(clientSyncDirectory);
		clientPanel.add(clientSyncDirectoryField);
		clientPanel.add(new JLabel("<html>仅客户端模组路径:"));
		JTextField clientOnlyDirectoryField = new JTextField(clientOnlyDirectory);
		clientPanel.add(clientOnlyDirectoryField);
		JCheckBox clientAutoStartBox = newJCheckBox(clientPanel, "<html>自动启动客户端", clientAutoStart);
		tabbedPane.addTab("<html>客户端设置", clientPanel);
		// 按钮面板
		JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
		newJButton(
				buttonPanel, "保存", event -> {
					// 定义输入框数组及其对应的提示信息和选项卡索引，并检查输入框是否为空
					for (Object[] input : new Object[][]{
							{"服务端端口", serverPortField, 0},
							{"最大上传速率", serverUploadRateLimitField, 0},
							{"上传速率单位(每秒)", serverUploadRateLimitUnitBox, 0},
							{"服务端同步文件夹路径", serverSyncDirectoryField, 0},
							{"客户端端口", clientPortField, 1},
							{"服务器地址", serverAddressField, 1},
							{"客户端同步文件夹路径", clientSyncDirectoryField, 1},
							{"仅客户端模组文件夹路径", clientOnlyDirectoryField, 1}
					})
						if (input[1] instanceof JTextField) {
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
					// 检测最大上传速率
					String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
					if (invalidLong(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
						log(WARNING, "最大上传速率格式错误: " + uploadRateLimitText);
						tabbedPane.setSelectedIndex(0);
						selectAndFocus(serverUploadRateLimitField);
						return;
					}
					serverAutoStart = serverAutoStartBox.isSelected();
					serverUploadRateLimit = Long.parseLong(uploadRateLimitText);
					serverUploadRateLimitUnit = (String) serverUploadRateLimitUnitBox.getSelectedItem();
					serverSyncDirectory = serverSyncDirectoryField.getText().trim();
					clientAutoStart = clientAutoStartBox.isSelected();
					serverAddress = serverAddressField.getText().trim();
					clientSyncDirectory = clientSyncDirectoryField.getText().trim();
					clientOnlyDirectory = clientOnlyDirectoryField.getText().trim();
					saveConfig(); // 保存配置
					settingsJDialog.dispose(); // 关闭对话框
				}
		);
		newJButton(buttonPanel, "取消", event -> settingsJDialog.dispose());
		newJButton(buttonPanel, "关于", event -> aboutJDialog(settingsJDialog));
		settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
		settingsJDialog.add(settingsPanel);
		settingsJDialog.setSize(screenLength / 5, screenLength / 8);
		setWindow(settingsJDialog);
	}
	// 聚焦并全选输入框
	private static void selectAndFocus(JTextField textField) {
		textField.requestFocus(); // 聚焦输入框
		textField.selectAll(); // 选中输入框
	}
	// 设置字体的通用方法
	private static void setFont(Container container, Font font) {
		for (Component component : container.getComponents()) {
			if (component instanceof Container) setFont((Container) component, font); // 递归设置子组件的字体
			component.setFont(font); // 设置字体
		}
	}
	// 处理请求
	private static void processRequest(HttpExchange exchange) {
		if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) return;
		String requestURI = exchange.getRequestURI().getPath();
		if (requestURI.startsWith("/download/")) {
			long requestCRC = Long.parseLong(requestURI.substring(requestURI.lastIndexOf("/") + 1));
			String filePath = null;
			for (Map.Entry<String, Long> entry : serverMap.entrySet())
				if (entry.getValue() == requestCRC) {
					filePath = serverSyncDirectory + separator + entry.getKey();
					break;
				}
			if (filePath == null) return;
			File file = new File(filePath);
			try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
				sendData(exchange, inputStream, file.length());
				log(INFO, "发送文件: " + file);
			} catch (IOException error) {
				log(SEVERE, "发送文件时出错: " + error.getMessage());
			}
		} else if (requestURI.startsWith("/list")) {
			StringBuilder responseBuilder = new StringBuilder();
			for (Map.Entry<String, Long> entry : serverMap.entrySet())
				responseBuilder.append(entry.getKey())
						.append(lineSeparator())
						.append(entry.getValue())
						.append(lineSeparator());
			byte[] bytes = responseBuilder.toString().getBytes();
			try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
				sendData(exchange, inputStream, bytes.length);
				log(INFO, "发送列表");
			} catch (IOException error) {
				log(SEVERE, "发送列表时出错: " + error.getMessage());
			}
		}
	}
	// 发送数据
	private static void sendData(HttpExchange exchange, InputStream inputStream, long responseBytesLength) {
		if (inputStream == null) return;
		try (OutputStream outputStream = exchange.getResponseBody()) {
			exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBytesLength); // 设置响应头
			byte[] buffer = new byte[16384];
			long totalBytesSent = 0; // 记录已发送字节数
			long lastFillTime = currentTimeMillis(); // 最近一次填充时间
			while (totalBytesSent < responseBytesLength) {
				if (serverUploadRateLimit == 0) { // 无限制
					int bytesRead = inputStream.read(buffer);
					if (bytesRead == -1) break;
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					continue;
				}
				long currentTime = currentTimeMillis();
				AVAILABLE_TOKENS.addAndGet((currentTime - lastFillTime) * maxUploadRateInBytes / 1000);
				lastFillTime = currentTime; // 更新时间
				long bytesToSend = Math.min(16384, responseBytesLength - totalBytesSent);
				if (AVAILABLE_TOKENS.get() >= bytesToSend) {
					int bytesRead = inputStream.read(buffer, 0, (int) bytesToSend);
					outputStream.write(buffer, 0, bytesRead); // 写入数据
					totalBytesSent += bytesRead; // 更新已发送字节数
					AVAILABLE_TOKENS.addAndGet(-bytesRead); // 减少可用令牌
				} else Thread.sleep((bytesToSend - AVAILABLE_TOKENS.get()) * 1000 / maxUploadRateInBytes);
			}
		} catch (Exception error) {
			log(SEVERE, "发送响应时出错: " + error.getMessage());
		} finally {
			try {
				inputStream.close();
			} catch (IOException error) {
				log(WARNING, "关闭输入流时出错: " + error.getMessage());
			}
		}
	}
	// 单位转换方法
	private static long convertToBytes(long value, String unit) {
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
}