package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.gui.GUI;

import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;
@SuppressWarnings("unused")
public class Log {
	public static final String LOG_PATH = HexSync.HEX_SYNC + File.separator + "latest.log"; // 日志文件路径
	public static final boolean ANSI = System.getProperty("ansi", "true").equalsIgnoreCase("false"); // 是否启用ANSI控制台输出
	public static final boolean LOG = System.getProperty("log", "true").equalsIgnoreCase("true"); // 是否记录日志
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	private static final String INFO = "信息";
	private static final String WARNING = "警告";
	private static final String ERROR = "错误";
	public static ExecutorService logExecutor; // 日志记录线程池
	public static FileWriter logWriter; // 日志记录器
	public static void info(String format, Object... args) {
		log(INFO, String.format(format, args));
	}
	// 记录日志
	private static void log(String level, String message) {
		if (!(LOG && logWriter != null && logExecutor != null)) return;
		logExecutor.submit(() -> {
			var formattedLog = String.format("[%s] [%s] %s%n", TIME_FORMAT.format(System.currentTimeMillis()), level, message);
			try {
				logWriter.write(formattedLog);
				logWriter.flush();
			} catch (IOException error) {
				System.err.println("无法写入日志: " + error.getMessage());
				return;
			}
			var info = level.equals(INFO);
			var warning = level.equals(WARNING);
			var severe = level.equals(ERROR);
			if (Config.HEADLESS) {
				if (ANSI) System.out.print(formattedLog);
				else System.out.printf(
					"%s%s\u001B[0m",
					info ? "\u001B[32m" : warning ? "\u001B[33m" : severe ? "\u001B[31m" : "\u001B[0m",
					formattedLog
				);
			} else appendLogToPane(formattedLog, info, warning, severe);
		});
	}
	// 在日志面板中追加日志
	private static void appendLogToPane(String formattedLog, boolean info, boolean warning, boolean severe) {
		SwingUtilities.invokeLater(() -> {
			var attributeSet = new SimpleAttributeSet();
			StyleConstants.setForeground(
				attributeSet,
				info ? new Color(0, 128, 0) : warning ? new Color(255, 165, 0) : severe ? new Color(255, 0, 0) : Color.BLACK
			);
			if (GUI.logPane != null) try {
				var document = GUI.logPane.getDocument();
				while (document.getDefaultRootElement().getElementCount() > 128) {
					var element = document.getDefaultRootElement().getElement(0);
					var lineStart = element.getStartOffset();
					document.remove(lineStart, element.getEndOffset() - lineStart); // 删除第一行
				}
				document.insertString(document.getLength(), formattedLog, attributeSet);
			} catch (Exception error) {
				System.err.println("日志输出失败: " + error.getMessage());
			}
		});
	}
	public static void info(String message) {
		log(INFO, message);
	}
	public static void warn(String format, Object... args) {
		log(WARNING, String.format(format, args));
	}
	public static void warn(String message) {
		log(WARNING, message);
	}
	public static void error(String format, Object... args) {
		log(ERROR, String.format(format, args));
	}
	public static void error(String message) {
		log(ERROR, message);
	}
	// 初始化日志
	public static void initLog() {
		try {
			FileUtil.makeDirectory(HexSync.HEX_SYNC);
			logWriter = new FileWriter(LOG_PATH, false);
		} catch (IOException error) {
			System.err.println("日志初始化失败: " + error.getMessage());
		}
		if (LOG) logExecutor = Executors.newSingleThreadExecutor();
	}
}
