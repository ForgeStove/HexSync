package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;
public class GUI extends JFrame implements Runnable {
	private static GUI instance;
	/**
	 * 私有构造函数，用于初始化 GUI 界面。
	 * 创建了主要布局，包括日志显示区域和控制按钮面板。
	 */
	private GUI() {
		super(HexSync.NAME);
		add(new JPanel() {{
			setLayout(new BorderLayout(10, 5));
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			// 日志滚动面板
			add(new JScrollPane(logPane) {{
				setBorder(BorderFactory.createTitledBorder(HexSync.get("GUI.log")));
			}}, BorderLayout.CENTER);
			// 按钮面板
			add(new JPanel(new GridLayout(0, 3)) {{
				add(new CButton(HexSync.get("GUI.startServer"), event -> Server.start()));
				add(new CButton(HexSync.get("GUI.startClient"), event -> Client.start()));
				add(new CButton(HexSync.get("GUI.settings"),
					event -> new SettingJDialog(GUI.this, HexSync.get("Setting.title")),
					Icons.cog));
				add(new CButton(HexSync.get("GUI.stopServer"), event -> Server.stop()));
				add(new CButton(HexSync.get("GUI.stopClient"), event -> Client.stop()));
				add(new CButton(HexSync.get("GUI.exit"), event -> System.exit(0), Icons.exit));
			}}, BorderLayout.SOUTH);
		}});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(640, 480));
	}
	public static GUI getInstance() {
		if (instance == null) instance = new GUI();
		return instance;
	}
	/** 在 Swing 事件调度线程中启动 GUI。 */
	public static void start() {SwingUtilities.invokeLater(() -> getInstance().run());}
	/**
	 * 实现 Runnable 接口的 run 方法。<p>
	 * 设置应用程序的主题并初始化窗口。
	 */
	public void run() {
		Component.setTheme(Data.theme.get());
		Component.setWindow(this);
	}
	/**
	 * 日志文本面板，用于显示应用程序日志信息。
	 * 面板不可编辑，并添加了复制和清除功能的右键菜单。
	 */
	public static final JTextPane logPane = new JTextPane() {{
		setOpaque(false);
		setEditable(false);
		// 弹出菜单
		setComponentPopupMenu(new JPopupMenu() {{
			add(new JMenuItem(HexSync.get("GUI.copy"), Icons.copy) {{
				addActionListener(event -> {
					if (getSelectedText() == null) selectAll();
					copy();
				});
			}});
			add(new JMenuItem(HexSync.get("GUI.clear"), Icons.clear) {{addActionListener(event -> logPane.setText(""));}});
			add(new JMenuItem(HexSync.get("GUI.refresh"), Icons.refresh) {{addActionListener(event -> System.gc());}});
			add(new JMenuItem(HexSync.get("GUI.memory"), Icons.memory) {{
				addActionListener(event -> {
					var memBar = new JProgressBar(0, 100) {{
						setStringPainted(true);
						setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
					}};
					var timer = new Timer(100, e -> {
						var runtime = Runtime.getRuntime();
						var used = runtime.totalMemory() - runtime.freeMemory();
						var total = runtime.totalMemory();
						var percentage = (int) ((used * 100) / total);
						memBar.setValue(percentage);
						memBar.setString("%d%% (%dMB/%dMB)".formatted(percentage, used / 1024 / 1024, total / 1024 / 1024));
					}) {{start();}};
					new JDialog(instance, HexSync.get("GUI.memoryUsage"), ModalityType.MODELESS) {{
						setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						addWindowListener(new WindowAdapter() {
							public void windowClosing(WindowEvent e) {timer.stop();}
						});
						add(memBar);
						setSize(360, 90);
						setLocationRelativeTo(instance);
						setVisible(true);
					}};
				});
			}});
			add(new JMenuItem(HexSync.get("GUI.openLog"), Icons.open) {{
				addActionListener(event -> {
					try {
						Desktop.getDesktop().open(Data.LOG_PATH.getParent().toFile());
					} catch (Exception error) {
						Log.error("无法打开日志: " + error);
					}
				});
			}});
		}});
	}};
}
