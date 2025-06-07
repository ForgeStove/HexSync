package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.gui.GUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;
public enum Log {
	INFO,
	WARN,
	ERROR;
	public static final String LOG_PATH = HexSync.NAME + File.separator + "latest.log"; // 日志文件路径
	public static final boolean ANSI = System.getProperty("ansi", "true").equalsIgnoreCase("false"); // 是否启用ANSI控制台输出
	public static final boolean LOG = System.getProperty("log", "true").equalsIgnoreCase("true"); // 是否记录日志
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	public static final String[] ANSI_COLORS = {"\u001B[32m", "\u001B[33m", "\u001B[31m", "\u001B[0m"};
	public static final Color[] LEVEL_COLORS = {new Color(0x8000), new Color(0xffa500), new Color(0xff0000)};
	private static final SimpleAttributeSet[] ATTR_SETS = {
		createSet(LEVEL_COLORS[0]), createSet(LEVEL_COLORS[1]), createSet(LEVEL_COLORS[2])
	};
	public static ExecutorService logExecutor; // 日志记录线程池
	public static FileWriter logWriter; // 日志记录器
	// 日志方法
	public static void info(String format, Object... args) {
		log(INFO, format, args);
	}
	public static void warn(String format, Object... args) {
		log(WARN, format, args);
	}
	public static void error(String format, Object... args) {
		log(ERROR, format, args);
	}
	// 记录日志
	private static void log(Log level, String message, Object... args) {
		if (!(LOG && logWriter != null && logExecutor != null)) return;
		logExecutor.submit(() -> {
			var log = "[%s] %s%n".formatted(TIME_FORMAT.format(System.currentTimeMillis()), message.formatted(args));
			writeLogToFile(log);
			if (!Config.HEADLESS) {
				SwingUtilities.invokeLater(() -> appendLogToPane(log, level));
				return;
			}
			if (ANSI) System.out.printf(log);
			else System.out.printf("%s%s\u001B[0m", ANSI_COLORS[level.ordinal()], log);
		});
	}
	// 写入日志文件
	private static void writeLogToFile(String log) {
		try {
			logWriter.write(log);
			logWriter.flush();
		} catch (IOException error) {
			System.err.println("无法写入日志: " + error.getMessage());
		}
	}
	// 在日志面板中追加日志
	private static void appendLogToPane(String formattedLog, Log level) {
		if (GUI.logPane != null) try {
			var document = GUI.logPane.getDocument();
			while (document.getDefaultRootElement().getElementCount() > 128) {
				var element = document.getDefaultRootElement().getElement(0);
				var lineStart = element.getStartOffset();
				document.remove(lineStart, element.getEndOffset() - lineStart); // 删除第一行
			}
			document.insertString(document.getLength(), formattedLog, ATTR_SETS[level.ordinal()]);
		} catch (Exception error) {
			System.err.println("日志输出失败: " + error.getMessage());
		}
	}
	private static @NotNull SimpleAttributeSet createSet(Color color) {
		var attributeSet = new SimpleAttributeSet();
		StyleConstants.setForeground(attributeSet, color);
		return attributeSet;
	}
	// 初始化日志
	public static void initLog() {
		try {
			FileUtil.makeDirectory(HexSync.NAME);
			logWriter = new FileWriter(LOG_PATH, false);
		} catch (IOException error) {
			System.err.println("日志初始化失败: " + error.getMessage());
		}
		if (LOG) logExecutor = Executors.newSingleThreadExecutor();
	}
}
