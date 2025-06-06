package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import java.awt.*;
public class GUI {
	public static Image icon; // 程序图标
	public static JFrame frame; // 主窗口
	public static JTextPane logPane; // 日志面板
	public static int screenLength; // 屏幕长边
	// 图形化界面
	public static void initGUI() {
		SwingUtilities.invokeLater(() -> {
			try {
				icon = Toolkit.getDefaultToolkit().getImage(HexSync.class.getClassLoader().getResource("icon.png"));
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				var size = Toolkit.getDefaultToolkit().getScreenSize();
				screenLength = Math.max(size.width, size.height);
				var panel = new JPanel(); // 主面板
				panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				panel.setLayout(new BorderLayout(5, 5));
				logPane = new JTextPane(); // 日志面板
				logPane.setEditable(false);
				logPane.setOpaque(false);
				panel.add(new JScrollPane(logPane), BorderLayout.CENTER);
				frame = new JFrame(HexSync.NAME); // 主窗口
				frame.setAlwaysOnTop(true);
				var buttonPanel = new JPanel(new GridLayout(2, 3));
				CComponent.newJButton(buttonPanel, "启动服务端", event -> Server.runServer());
				CComponent.newJButton(buttonPanel, "启动客户端", event -> Client.runClient());
				CComponent.newJButton(buttonPanel, "设置", event -> SettingsJDialog.initSettingsJDialog());
				CComponent.newJButton(buttonPanel, "停止服务端", event -> Server.stopServer());
				CComponent.newJButton(buttonPanel, "停止客户端", event -> Client.stopClient());
				CComponent.newJButton(buttonPanel, "退出", event -> System.exit(0));
				panel.add(buttonPanel, BorderLayout.SOUTH);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(panel);
				frame.setSize(new Dimension(screenLength / 3, screenLength / 4));
				CComponent.setWindow(frame); // 设置窗口属性
			} catch (Exception error) {
				Log.error("初始化UI时出错:" + error.getMessage());
			}
		});
	}
}
