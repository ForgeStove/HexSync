package io.github.forgestove.hexsync.util;
import io.github.forgestove.hexsync.HexSync;
import io.github.forgestove.hexsync.config.Data;
import io.github.forgestove.hexsync.gui.GUI;
import org.jetbrains.annotations.*;
import picocli.CommandLine.Help.Ansi;

import javax.swing.SwingUtilities;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.concurrent.*;
public class Log {
	public static final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor();
	public static PrintStream printStream;
	// 日志记录方法
	public static void info(String message) {log(Level.INFO, message);}
	public static void warn(String message) {log(Level.WARN, message);}
	public static void error(String message) {log(Level.ERROR, message);}
	public static void stackTrace(@NotNull Throwable throwable) {
		var writer = new StringWriter();
		throwable.printStackTrace(new PrintWriter(writer));
		error(writer.toString());
	}
	// 日志核心方法
	private static void log(@NotNull Level level, String message) {
		var timeStamp = "[%s]".formatted(LocalTime.now().withNano(0));
		var levelTag = "[%s]".formatted(HexSync.get(level.resourceName));
		var content = "%s%n".formatted(message);
		var logParts = new LogParts(timeStamp, levelTag, content);
		if (!HexSync.HEADLESS) SwingUtilities.invokeLater(() -> writeToLogPane(level, logParts));
		if (Ansi.ON.enabled()) {
			System.out.print(logParts.timeStamp + " ");
			System.out.printf("%s%s\u001B[0m", level.ansi, logParts.levelTag);
			System.out.print(" " + logParts.content);
		} else System.out.print(logParts.getFullLog());
		if (printStream != null) printStream.print(logParts.getFullLog());
	}
	// 在日志面板中追加日志
	private static void writeToLogPane(Level level, LogParts logParts) {
		var document = GUI.logPane.getDocument();
		var root = document.getDefaultRootElement();
		var maxLines = 256;
		var lineCount = root.getElementCount();
		try {
			if (lineCount > maxLines) document.remove(0, root.getElement(lineCount - maxLines - 1).getEndOffset());
			document.insertString(document.getLength(), logParts.timeStamp + " ", null);
			document.insertString(document.getLength(), logParts.levelTag, level.attr);
			document.insertString(document.getLength(), " " + logParts.content, null);
		} catch (Exception e) {
			System.err.println("日志输出失败: " + e.getMessage());
		}
	}
	// 初始化日志
	public static void init() {
		FileUtil.makeDirectory(Path.of(HexSync.NAME));
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> stackTrace(throwable));
		Runtime.getRuntime().addShutdownHook(new Thread(flushScheduler::shutdown));
		try {
			printStream = new PrintStream(Data.LOG_PATH.toFile(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public enum Level {
		INFO("info", "\u001B[32m", new Color(0, 165, 0)),
		WARN("warn", "\u001B[33m", new Color(255, 140, 0)),
		ERROR("error", "\u001B[31m", new Color(235, 0, 0));
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
	// 存储日志的不同部分
	public record LogParts(String timeStamp, String levelTag, String content) {
		@Contract(pure = true)
		public @NotNull String getFullLog() {
			return timeStamp + " " + levelTag + " " + content;
		}
	}
}
