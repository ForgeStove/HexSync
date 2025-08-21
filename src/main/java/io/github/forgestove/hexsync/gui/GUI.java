package io.github.forgestove.hexsync.gui;
import io.github.forgestove.hexsync.HexSync;
import io.github.forgestove.hexsync.client.Client;
import io.github.forgestove.hexsync.config.Data;
import io.github.forgestove.hexsync.gui.common.*;
import io.github.forgestove.hexsync.gui.common.Component;
import io.github.forgestove.hexsync.server.Server;

import javax.swing.*;
import java.awt.*;
public class GUI extends JFrame implements Runnable {
	/** 日志文本面板，用于显示应用程序日志信息。 */
	public static final JTextPane logPane = new LogPane();
	/** 进度显示面板，用于显示下载和同步进度 */
	public static final ProgressPanel progressPanel = new ProgressPanel();
	private static GUI instance;
	/**
	 * 私有构造函数，用于初始化 GUI 界面。
	 * 创建了主要布局，包括日志显示区域和控制按钮面板。
	 */
	private GUI() {
		super(HexSync.NAME);
		add(new JPanel() {{
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			add(
				new JPanel() {{
					setLayout(new BorderLayout());
					// 日志滚动面板
					add(
						new JScrollPane(logPane) {{
							setBorder(BorderFactory.createTitledBorder(HexSync.get("log")));
						}}, BorderLayout.CENTER
					);
					// 进度显示面板
					add(progressPanel, BorderLayout.SOUTH);
				}}, BorderLayout.CENTER
			);
			// 按钮面板
			add(
				new JPanel(new GridLayout(0, 3)) {{
					add(new CButton(HexSync.get("start", "server"), event -> Server.start()));
					add(new CButton(HexSync.get("start", "client"), event -> Client.start()));
					add(new CButton(HexSync.get("settings"), event -> new SettingJDialog(GUI.this, HexSync.get("settings")), Icons.cog));
					add(new CButton(HexSync.get("stop", "server"), event -> Server.stop()));
					add(new CButton(HexSync.get("stop", "client"), event -> Client.stop()));
					add(new CButton(HexSync.get("exit"), event -> System.exit(0), Icons.exit));
				}}, BorderLayout.SOUTH
			);
		}});
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(640, 480));
	}
	/**
	 * 获取 GUI 的单例实例。
	 * 如果实例不存在，则创建一个新的 GUI 实例。
	 *
	 * @return GUI的单例实例
	 */
	public static synchronized GUI getInstance() {
		if (instance == null) instance = new GUI();
		return instance;
	}
	/** 在 Swing 事件调度线程中启动 GUI。 */
	public static synchronized void start() {SwingUtilities.invokeLater(() -> getInstance().run());}
	/**
	 * 实现 Runnable 接口的 run 方法。<p>
	 * 设置应用程序的主题并初始化窗口。
	 */
	public synchronized void run() {
		Component.setTheme(Data.theme.get());
		Component.setWindow(this);
	}
}
