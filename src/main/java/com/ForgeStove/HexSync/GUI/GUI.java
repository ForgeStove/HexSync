package com.ForgeStove.HexSync.GUI;
import com.ForgeStove.HexSync.HexSync;

import javax.swing.*;
import java.awt.*;

import static com.ForgeStove.HexSync.Client.Client.*;
import static com.ForgeStove.HexSync.GUI.ComponentFactory.*;
import static com.ForgeStove.HexSync.GUI.SettingsJDialog.initSettingsJDialog;
import static com.ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static com.ForgeStove.HexSync.Server.Server.*;
import static com.ForgeStove.HexSync.Util.Log.*;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.lang.Math.max;
import static java.lang.System.exit;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import static javax.swing.UIManager.*;
public class GUI {
	public static Image icon; // 程序图标
	public static JFrame frame; // 主窗口
	public static JTextPane logPane; // 日志面板
	public static int screenLength; // 屏幕长边
	// 图形化界面
	public static void initGUI() {
		SwingUtilities.invokeLater(() -> {
			try {
				icon = getDefaultToolkit().getImage(HexSync.class.getClassLoader().getResource("icon.png"));
				setLookAndFeel(getSystemLookAndFeelClassName());
				var size = getDefaultToolkit().getScreenSize();
				screenLength = max(size.width, size.height);
				var panel = new JPanel(); // 主面板
				panel.setBorder(createEmptyBorder(5, 5, 5, 5));
				panel.setLayout(new BorderLayout(5, 5));
				logPane = new JTextPane(); // 日志面板
				logPane.setEditable(false);
				logPane.setOpaque(false);
				panel.add(new JScrollPane(logPane), BorderLayout.CENTER);
				frame = new JFrame(HEX_SYNC_NAME); // 主窗口
				frame.setAlwaysOnTop(true);
				var buttonPanel = new JPanel(new GridLayout(2, 3));
				newJButton(buttonPanel, "启动服务端", event -> runServer());
				newJButton(buttonPanel, "启动客户端", event -> runClient());
				newJButton(buttonPanel, "设置", event -> initSettingsJDialog());
				newJButton(buttonPanel, "停止服务端", event -> stopServer());
				newJButton(buttonPanel, "停止客户端", event -> stopClient());
				newJButton(buttonPanel, "退出", event -> exit(0));
				panel.add(buttonPanel, BorderLayout.SOUTH);
				frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
				frame.add(panel);
				frame.setSize(new Dimension(screenLength / 3, screenLength / 4));
				setWindow(frame); // 设置窗口属性
			} catch (Exception error) {
				log(SEVERE, "初始化UI时出错:" + error.getMessage());
			}
		});
	}
}
