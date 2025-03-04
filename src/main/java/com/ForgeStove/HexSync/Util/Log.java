package com.ForgeStove.HexSync.Util;
import javax.swing.text.*;
import java.awt.Color;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;

import static com.ForgeStove.HexSync.GUI.GUI.logPane;
import static com.ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static com.ForgeStove.HexSync.Util.Config.HEADLESS;
import static com.ForgeStove.HexSync.Util.Files.makeDirectory;
import static java.awt.Color.BLACK;
import static java.io.File.separator;
import static java.lang.System.*;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.text.StyleConstants.setForeground;
public class Log {
	public static final String LOG_PATH = HEX_SYNC_NAME + separator + "latest.log"; // 日志文件路径
	public static final boolean ANSI = getProperty("ansi", "true").equalsIgnoreCase("false"); // 是否启用ANSI控制台输出
	public static final boolean LOG = getProperty("log", "true").equalsIgnoreCase("true"); // 是否记录日志
	public static final String INFO = "信息";
	public static final String WARNING = "警告";
	public static final String SEVERE = "严重";
	public static ExecutorService logExecutor; // 日志记录线程池
	public static FileWriter logWriter; // 日志记录器
	// 记录日志
	public static void log(String level, String message) {
		if (LOG && logWriter != null && logExecutor != null) logExecutor.submit(() -> {
			try {
				String formattedLog = String.format(
						"[%s] [%s] %s%n",
						new SimpleDateFormat("HH:mm:ss").format(currentTimeMillis()),
						level,
						message
				);
				logWriter.write(formattedLog);
				logWriter.flush();
				boolean info = level.equals(INFO);
				boolean warning = level.equals(WARNING);
				boolean severe = level.equals(SEVERE);
				if (HEADLESS) {
					if (ANSI) out.print(formattedLog);
					else out.printf(
							"%s%s\u001B[0m",
							info ? "\u001B[32m" : warning ? "\u001B[33m" : severe ? "\u001B[31m" : "\u001B[0m",
							formattedLog
					);
				} else invokeLater(() -> {
					SimpleAttributeSet attributeSet = new SimpleAttributeSet();
					setForeground(
							attributeSet,
							info
									? new Color(0, 128, 0)
									: warning ? new Color(255, 165, 0) : severe ? new Color(255, 0, 0) : BLACK
					);
					if (logPane != null) try {
						Document document = logPane.getDocument();
						while (document.getDefaultRootElement().getElementCount() > 128) {
							Element element = document.getDefaultRootElement().getElement(0);
							int lineStart = element.getStartOffset();
							document.remove(lineStart, element.getEndOffset() - lineStart); // 删除第一行
						}
						document.insertString(document.getLength(), formattedLog, attributeSet);
					} catch (Exception error) {
						err.println("日志输出失败: " + error.getMessage());
					}
				});
			} catch (IOException error) {
				if (logWriter == null) initLog();
				else err.println("无法写入日志: " + error.getMessage());
			}
		});
	}
	// 初始化日志
	public static void initLog() {
		try {
			makeDirectory(HEX_SYNC_NAME);
			logWriter = new FileWriter(LOG_PATH, false);
		} catch (IOException error) {
			err.println("日志初始化失败: " + error.getMessage());
		}
		if (LOG) logExecutor = newSingleThreadExecutor();
	}
}
