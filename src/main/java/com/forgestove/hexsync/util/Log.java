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
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
	public static final String[] ANSI_COLORS = {"\u001B[32m", "\u001B[33m", "\u001B[31m", "\u001B[0m"};
	public static final Color[] LEVEL_COLORS = {new Color(0x8000), new Color(0xffa500), new Color(0xff0000)};
	public static final SimpleAttributeSet[] ATTR_SETS = createAttrSets(); // 日志属性集
	public static final ExecutorService logExecutor = Executors.newSingleThreadExecutor(); // 日志记录线程池
	public static final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(); // 定时flush调度器
	public static final BufferedWriter logWriter; // 日志记录器
	static {
		try {
			logWriter = new BufferedWriter(new FileWriter(Config.LOG_PATH, false));
		} catch (IOException error) {
			throw new RuntimeException("无法创建日志文件", error);
		}
	}
	// 日志记录方法
	public static void info(String format, Object... args) {log(INFO, format, args);}
	public static void warn(String format, Object... args) {log(WARN, format, args);}
	public static void error(String format, Object... args) {log(ERROR, format, args);}
	// 日志核心方法
	private static void log(Log level, String message, Object... args) {
		if (!(Config.LOG && logWriter != null)) return;
		logExecutor.submit(() -> {
			var log = "[%s] %s%n".formatted(TIME_FORMAT.format(System.currentTimeMillis()), message.formatted(args));
			try {
				logWriter.write(log);
			} catch (IOException error) {
				System.err.println("无法写入日志: " + error.getMessage());
			}
			if (!Config.HEADLESS) {
				SwingUtilities.invokeLater(() -> appendLogToPane(log, level));
				return;
			}
			if (Config.ANSI) System.out.printf(log);
			else System.out.printf("%s%s\u001B[0m", ANSI_COLORS[level.ordinal()], log);
		});
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
	// 初始化日志
	public static void initLog() {
		FileUtil.makeDirectory(HexSync.NAME);
		flushScheduler.scheduleAtFixedRate(
			() -> {
				try {
					if (logWriter != null) logWriter.flush();
				} catch (IOException ignored) {}
			}, 5, 5, TimeUnit.SECONDS
		);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (logWriter != null) logWriter.close();
				flushScheduler.shutdown();
				logExecutor.shutdown();
			} catch (IOException ignored) {}
		}));
	}
	// 创建日志属性集
	private static SimpleAttributeSet @NotNull [] createAttrSets() {
		var attributeSets = new SimpleAttributeSet[LEVEL_COLORS.length];
		for (var i = 0; i < LEVEL_COLORS.length; i++) {
			var attributeSet = new SimpleAttributeSet();
			StyleConstants.setForeground(attributeSet, LEVEL_COLORS[i]);
			attributeSets[i] = attributeSet;
		}
		return attributeSets;
	}
}
