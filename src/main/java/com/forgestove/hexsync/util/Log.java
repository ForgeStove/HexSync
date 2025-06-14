package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.gui.GUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.concurrent.*;
public class Log {
	public static final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor();
	public static PrintStream printStream;
	// 日志记录方法
	public static void info(String message) {log(Level.INFO, message);}
	public static void warn(String message) {log(Level.WARN, message);}
	public static void error(String message) {log(Level.ERROR, message);}
	// 日志核心方法
	private static void log(@NotNull Level level, String message) {
		var log = "[%s] [%s] %s%n".formatted(LocalTime.now().withNano(0), HexSync.get(level.resourceName), message);
		if (!HexSync.HEADLESS) SwingUtilities.invokeLater(() -> writeToLogPane(level, log));
		if (HexSync.ANSI) System.out.printf("%s%s\u001B[0m", level.ansi, log);
		else System.out.print(log);
		printStream.print(log);
	}
	// 在日志面板中追加日志
	private static void writeToLogPane(Level level, String log) {
		var document = GUI.logPane.getDocument();
		var root = document.getDefaultRootElement();
		var maxLines = 100;
		var lineCount = root.getElementCount();
		try {
			if (lineCount > maxLines) document.remove(0, root.getElement(lineCount - maxLines - 1).getEndOffset());
			document.insertString(document.getLength(), log, level.attr);
		} catch (Exception error) {
			System.err.println("日志输出失败: " + error.getMessage());
		}
	}
	// 初始化日志
	public static void initLog() {
		FileUtil.makeDirectory(Path.of(HexSync.NAME));
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			var writer = new StringWriter();
			throwable.printStackTrace(new PrintWriter(writer));
			Arrays.stream(writer.toString().split("\\n")).map(line -> "\t" + line.trim()).forEach(Log::error);
		});
		Runtime.getRuntime().addShutdownHook(new Thread(flushScheduler::shutdown));
		try {
			printStream = new PrintStream(Data.LOG_PATH.toFile());
		} catch (Exception error) {
			throw new RuntimeException(error);
		}
	}
	public enum Level {
		INFO("Log.info", "\u001B[32m", new Color(0, 165, 0)),
		WARN("Log.warn", "\u001B[33m", new Color(255, 140, 0)),
		ERROR("Log.error", "\u001B[31m", new Color(235, 0, 0));
		public final String resourceName;
		public final String ansi;
		public final SimpleAttributeSet attr;
		Level(String resourceName, String ansi, Color color) {
			this.resourceName = resourceName;
			this.ansi = ansi;
			attr = new SimpleAttributeSet();
			StyleConstants.setForeground(attr, color);
		}
	}
}
