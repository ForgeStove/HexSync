package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Config;
import com.forgestove.hexsync.gui.GUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.time.LocalTime;
import java.util.concurrent.*;
public enum Log {
	INFO("Log.info", "\u001B[32m", new Color(0, 165, 0)),
	WARN("Log.warn", "\u001B[33m", new Color(255, 140, 0)),
	ERROR("Log.error", "\u001B[31m", new Color(200, 0, 0));
	public static final ExecutorService logExecutor = Executors.newSingleThreadExecutor(); // 日志记录线程池
	public static final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor(); // 定时flush调度器
	public static BufferedWriter logWriter; // 日志记录器
	public final String ansi; // ANSI颜色代码
	public final SimpleAttributeSet set; // 日志属性集
	public final Color color;
	public final String resourceName;
	Log(String resourceName, String ansi, Color color) {
		this.resourceName = resourceName;
		this.ansi = ansi;
		this.color = color;
		set = new SimpleAttributeSet();
		StyleConstants.setForeground(set, color);
	}
	// 日志记录方法
	public static void info(String message) {log(INFO, message);}
	public static void info(@NotNull String format, Object... args) {log(INFO, format.formatted(args));}
	public static void warn(String message) {log(WARN, message);}
	public static void warn(@NotNull String format, Object... args) {log(WARN, format.formatted(args));}
	public static void error(String message) {log(ERROR, message);}
	public static void error(@NotNull String format, Object... args) {log(ERROR, format.formatted(args));}
	// 日志核心方法
	private static void log(Log level, String message) {
		if (!(Config.LOG && logWriter != null)) return;
		logExecutor.submit(() -> {
			var log = "[%s] [%s] %s%n".formatted(LocalTime.now().withNano(0), HexSync.lang.getString(level.resourceName), message);
			try {
				logWriter.write(log);
			} catch (IOException error) {
				System.err.println("无法写入日志: " + error.getMessage());
			}
			if (!Config.HEADLESS) {
				SwingUtilities.invokeLater(() -> appendLogToPane(level, log));
				return;
			}
			if (Config.ANSI) System.out.printf(log);
			else System.out.printf("%s%s\u001B[0m", level.ansi, log);
		});
	}
	// 在日志面板中追加日志
	private static void appendLogToPane(Log level, String formattedLog) {
		try {
			var document = GUI.logPane.getDocument();
			var root = document.getDefaultRootElement();
			var maxLines = 100;
			var lineCount = root.getElementCount();
			if (lineCount > maxLines) {
				var linesToRemove = lineCount - maxLines;
				var endOffset = root.getElement(linesToRemove - 1).getEndOffset();
				document.remove(0, endOffset);
			}
			document.insertString(document.getLength(), formattedLog, level.set);
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
}
