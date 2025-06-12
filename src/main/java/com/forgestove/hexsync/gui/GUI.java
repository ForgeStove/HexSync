package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.server.Server;

import javax.swing.*;
import java.awt.*;
public class GUI {
	public static final JTextPane logPane = new JTextPane(); // 日志面板
	public static Image icon; // 程序图标
	public static JFrame frame; // 主窗口
	// 图形化界面
	public static void runGUI() {
		SwingUtilities.invokeLater(() -> {
			icon = Toolkit.getDefaultToolkit().getImage(HexSync.class.getClassLoader().getResource("icon.png"));
			ComponentUtil.setTheme(ComponentUtil.getClassName(Data.theme.get()));
			var panel = new JPanel(); // 主面板
			panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			panel.setLayout(new BorderLayout(5, 5));
			logPane.setEditable(false);
			logPane.setOpaque(false);
			panel.add(new JScrollPane(logPane), BorderLayout.CENTER);
			frame = new JFrame(HexSync.NAME); // 主窗口
			var buttonPanel = new JPanel(new GridLayout(2, 3));
			ComponentUtil.newJButton(buttonPanel, "启动服务端", event -> Server.runServer());
			ComponentUtil.newJButton(buttonPanel, "启动客户端", event -> Client.runClient());
			ComponentUtil.newJButton(buttonPanel, "设置", event -> new SettingJDialog(frame, "设置"));
			ComponentUtil.newJButton(buttonPanel, "停止服务端", event -> Server.stopServer());
			ComponentUtil.newJButton(buttonPanel, "停止客户端", event -> Client.stopClient());
			ComponentUtil.newJButton(buttonPanel, "退出", event -> System.exit(0));
			panel.add(buttonPanel, BorderLayout.SOUTH);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(panel);
			frame.setPreferredSize(new Dimension(480, 360));
			ComponentUtil.setWindow(frame); // 设置窗口属性
		});
	}
}
