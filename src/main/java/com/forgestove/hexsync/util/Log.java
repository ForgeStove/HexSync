package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.gui.GUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.io.File;
import java.time.LocalTime;
import java.util.concurrent.*;
public class Log {
	public static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
	public static final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor();
	// 日志记录方法
	public static void info(String message) {log(Level.INFO, message);}
	public static void info(@NotNull String format, Object... args) {log(Level.INFO, format.formatted(args));}
	public static void warn(String message) {log(Level.WARN, message);}
	public static void warn(@NotNull String format, Object... args) {log(Level.WARN, format.formatted(args));}
	public static void error(String message) {log(Level.ERROR, message);}
	public static void error(@NotNull String format, Object... args) {log(Level.ERROR, format.formatted(args));}
	// 日志核心方法
	private static void log(Level level, String message) {
		logExecutor.submit(() -> {
			var log = "[%s] [%s] %s%n".formatted(LocalTime.now().withNano(0), HexSync.lang.getString(level.resourceName), message);
			FileUtil.appendLine(new File(Data.LOG_PATH), log);
			if (!HexSync.HEADLESS) {
				SwingUtilities.invokeLater(() -> writeToLogPane(level, log));
				return;
			}
			System.out.printf(HexSync.ANSI ? log : "%s%s\u001B[0m", level.ansi, log);
		});
	}
	// 在日志面板中追加日志
	private static void writeToLogPane(Level level, String log) {
		try {
			var document = GUI.logPane.getDocument();
			var root = document.getDefaultRootElement();
			var maxLines = 100;
			var lineCount = root.getElementCount();
			if (lineCount > maxLines) document.remove(0, root.getElement(lineCount - maxLines - 1).getEndOffset());
			document.insertString(document.getLength(), log, level.attr);
		} catch (Exception error) {
			System.err.println("日志输出失败: " + error.getMessage());
		}
	}
	// 初始化日志
	public static void initLog() {
		FileUtil.makeDirectory(HexSync.NAME);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			flushScheduler.shutdown();
			logExecutor.shutdown();
		}));
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> error(throwable.getMessage()));
	}
	public enum Level {
		INFO("Log.info", "\u001B[32m", new Color(0, 165, 0)),
		WARN("Log.warn", "\u001B[33m", new Color(255, 140, 0)),
		ERROR("Log.error", "\u001B[31m", new Color(235, 0, 0));
		public final String ansi;
		public final SimpleAttributeSet attr;
		public final String resourceName;
		Level(String resourceName, String ansi, Color color) {
			this.resourceName = resourceName;
			this.ansi = ansi;
			attr = new SimpleAttributeSet();
			StyleConstants.setForeground(attr, color);
		}
	}
}
