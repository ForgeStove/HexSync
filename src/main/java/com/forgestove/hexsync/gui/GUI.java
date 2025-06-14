package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.server.Server;
import org.jetbrains.annotations.Contract;

import javax.swing.*;
import java.awt.*;
public class GUI implements Runnable {
	public static final JTextPane logPane = new JTextPane() {{
		setEditable(false);
		setOpaque(false);
	}}; // 日志面板
	@Contract(pure = true)
	private GUI() {}
	public static void start() {SwingUtilities.invokeLater(() -> new GUI().run());}
	public void run() {
		var frame = new JFrame(HexSync.NAME); // 主窗口
		var scrollPane = new JScrollPane(logPane); // 日志滚动面板
		var buttonPanel = new JPanel(new GridLayout(0, 3)); // 按钮面板
		scrollPane.setBorder(BorderFactory.createTitledBorder(HexSync.get("GUI.log")));
		buttonPanel.add(new CButton(HexSync.get("GUI.startServer"), event -> Server.start()));
		buttonPanel.add(new CButton(HexSync.get("GUI.startClient"), event -> Client.start()));
		buttonPanel.add(new CButton(HexSync.get("GUI.settings"), event -> new SettingJDialog(frame, HexSync.get("Setting.title")),
			IconManager.cog));
		buttonPanel.add(new CButton(HexSync.get("GUI.stopServer"), event -> Server.stop()));
		buttonPanel.add(new CButton(HexSync.get("GUI.stopClient"), event -> Client.stop()));
		buttonPanel.add(new CButton(HexSync.get("GUI.exit"), event -> System.exit(0), IconManager.exit));
		frame.setLayout(new BorderLayout(10, 10));
		frame.add(scrollPane, BorderLayout.CENTER);
		frame.add(buttonPanel, BorderLayout.SOUTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(new Dimension(640, 480));
		// 弹出菜单
		var copyItem = new JMenuItem(HexSync.get("GUI.copy"));
		var clearItem = new JMenuItem(HexSync.get("GUI.clear"));
		copyItem.addActionListener(event -> {
			if (logPane.getSelectedText() == null) logPane.selectAll();
			logPane.copy();
		});
		clearItem.addActionListener(event -> logPane.setText(""));
		var popupMenu = new JPopupMenu();
		popupMenu.add(copyItem);
		popupMenu.add(clearItem);
		logPane.setComponentPopupMenu(popupMenu);
		Component.setTheme(Data.theme.get());
		Component.setWindow(frame);
	}
}
