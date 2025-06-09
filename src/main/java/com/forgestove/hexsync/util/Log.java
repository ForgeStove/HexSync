package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.gui.GUI;

import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.time.LocalTime;
import java.util.concurrent.*;
public enum Log {
	INFO("\u001B[32m"),
	WARN("\u001B[33m"),
	ERROR("\u001B[31m");
	public static final ExecutorService logExecutor = Executors.newSingleThreadExecutor(); // 日志记录线程池
	public static final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(); // 定时flush调度器
	public static BufferedWriter logWriter; // 日志记录器
	public final Color[] LEVEL_COLORS = new Color[]{new Color(0, 128, 0), new Color(255, 165, 0), new Color(255, 0, 0)};
	public final SimpleAttributeSet ATTR_SET = createAttrSets(); // 日志属性集
	public final String ansi; // ANSI颜色代码
	Log(String ansi) {
		this.ansi = ansi;
	}
	// 日志记录方法
	public static void info(String format, Object... args) {log(INFO, format, args);}
	public static void warn(String format, Object... args) {log(WARN, format, args);}
	public static void error(String format, Object... args) {log(ERROR, format, args);}
	// 日志核心方法
	private static void log(Log level, String message, Object... args) {
		if (!(Config.LOG && logWriter != null)) return;
		logExecutor.submit(() -> {
			var log = "[%s] %s%n".formatted(LocalTime.now().withNano(0), args == null ? message : message.formatted(args));
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
			else System.out.printf("%s%s\u001B[0m", level.ansi, log);
		});
	}
	// 在日志面板中追加日志
	private static void appendLogToPane(String formattedLog, Log level) {
		if (GUI.logPane != null) try {
			var document = GUI.logPane.getDocument();
			var root = document.getDefaultRootElement();
			var maxLines = 128;
			var lineCount = root.getElementCount();
			if (lineCount > maxLines) {
				var linesToRemove = lineCount - maxLines;
				var endOffset = root.getElement(linesToRemove - 1).getEndOffset();
				document.remove(0, endOffset);
			}
			document.insertString(document.getLength(), formattedLog, level.ATTR_SET);
		} catch (Exception error) {
			System.err.println("日志输出失败: " + error.getMessage());
		}
	}
	// 初始化日志
	public static void initLog() {
		FileUtil.makeDirectory(HexSync.NAME);
		try {
			logWriter = new BufferedWriter(new FileWriter(Config.LOG_PATH, false));
		} catch (IOException error) {
			System.err.println("无法创建日志文件: " + error.getMessage());
		}
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
	private SimpleAttributeSet createAttrSets() {
		var attributeSet = new SimpleAttributeSet();
		StyleConstants.setForeground(attributeSet, LEVEL_COLORS[ordinal()]);
		return attributeSet;
	}
}
