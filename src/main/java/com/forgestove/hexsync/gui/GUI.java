package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.server.Server;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
public class GUI {
	public static final JTextPane logPane = new JTextPane(); // 日志面板
	public static FlatSVGIcon icon; // 程序图标
	public static JFrame frame; // 主窗口
	// 图形化界面
	public static void runGUI() {
		SwingUtilities.invokeLater(() -> {
			icon = new FlatSVGIcon("icon.svg");
			ComponentUtil.setTheme(ComponentUtil.getClassName(Data.theme.get()));
			var panel = new JPanel(); // 主面板
			panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			panel.setLayout(new BorderLayout(5, 5));
			logPane.setEditable(false);
			logPane.setOpaque(false);
			var scrollPane = new JScrollPane(logPane);
			scrollPane.setBorder(BorderFactory.createTitledBorder(HexSync.get("GUI.log")));
			panel.add(scrollPane, BorderLayout.CENTER);
			frame = new JFrame(HexSync.NAME); // 主窗口
			var buttonPanel = new JPanel(new GridLayout(0, 3));
			buttonPanel.add(new CButton(HexSync.get("GUI.startServer"), event -> Server.start()));
			buttonPanel.add(new CButton(HexSync.get("GUI.startClient"), event -> Client.start()));
			buttonPanel.add(new CButton(HexSync.get("GUI.settings"), event -> new SettingJDialog(frame, HexSync.get("Settings.title"))));
			buttonPanel.add(new CButton(HexSync.get("GUI.stopServer"), event -> Server.stop()));
			buttonPanel.add(new CButton(HexSync.get("GUI.stopClient"), event -> Client.stop()));
			buttonPanel.add(new CButton(HexSync.get("GUI.exit"), event -> System.exit(0)));
			panel.add(buttonPanel, BorderLayout.SOUTH);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.add(panel);
			frame.setPreferredSize(new Dimension(512, 512));
			ComponentUtil.setWindow(frame); // 设置窗口属性
		});
	}
}
