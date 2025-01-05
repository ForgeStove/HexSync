// Copyright (C) 2025 ForgeStove
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package ForgeStove;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Objects;

import static ForgeStove.Client.*;
import static ForgeStove.Config.*;
import static ForgeStove.HexSync.*;
import static ForgeStove.Log.*;
import static ForgeStove.Server.*;
import static ForgeStove.Util.*;
import static java.lang.Math.max;
import static java.lang.System.*;
public class NormalUI {
	public static final boolean HEADLESS = GraphicsEnvironment.isHeadless(); // 是否处于无头模式
	public static Image icon; // 程序图标
	public static JFrame frame; // 主窗口
	public static JTextPane textPane; // 日志面板
	public static int screenLength; // 屏幕长边
	// 有头模式UI
	public static void normalUI() {
		SwingUtilities.invokeLater(() -> {
			try {
				icon = Toolkit.getDefaultToolkit().getImage(HexSync.class.getClassLoader().getResource("icon.png"));
				javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
				Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
				screenLength = max(size.width, size.height);
				// 添加按钮，状态面板和托盘图标
				JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				panel.setLayout(new BorderLayout(5, 5));
				textPane = new JTextPane();
				textPane.setEditable(false);
				textPane.setOpaque(false);
				panel.add(new JScrollPane(textPane), BorderLayout.CENTER);
				frame = new JFrame(HEX_SYNC_NAME); // 主窗口
				frame.setAlwaysOnTop(true);
				JPanel buttonPanel = new JPanel(new GridLayout(2, 3));
				newJButton(buttonPanel, "设置", event -> settingsJDialog());
				newJButton(buttonPanel, "启动服务端", event -> startServer());
				newJButton(buttonPanel, "启动客户端", event -> startClient());
				newJButton(buttonPanel, "停止服务端", event -> stopServer());
				newJButton(buttonPanel, "停止客户端", event -> stopClient());
				newJButton(buttonPanel, "退出", event -> exit(0));
				panel.add(buttonPanel, BorderLayout.SOUTH);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(panel);
				frame.setSize(new Dimension(screenLength / 3, screenLength / 4));
				setWindow(frame);
			} catch (Exception error) {
				log(SEVERE, "初始化UI时出错:" + error.getMessage());
			}
		});
	}
	// 打开设置对话框
	public static void settingsJDialog() {
		if (checkJDialog("设置")) return;
		loadConfig();
		// 设置对话框
		JDialog settingsJDialog = new JDialog(frame, "设置");
		JPanel settingsPanel = new JPanel(new BorderLayout());
		settingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// 选项卡面板
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setFocusable(false);
		settingsPanel.add(tabbedPane, BorderLayout.CENTER);
		// 服务端选项卡
		JPanel serverPanel = new JPanel(new GridLayout(5, 2));
		serverPanel.add(new JLabel("<html>端口号:"));
		JTextField serverPortField = new JTextField(String.valueOf(serverPort));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel("<html>最大上传速率:"));
		JTextField serverUploadRateLimitField = new JTextField(String.valueOf(serverUploadRateLimit));
		serverPanel.add(serverUploadRateLimitField);
		serverPanel.add(new JLabel("<html>上传速率单位(每秒):"));
		JComboBox<String> serverUploadRateLimitUnitBox = new JComboBox<>(new String[]{"B", "KB", "MB", "GB"});
		serverUploadRateLimitUnitBox.setFocusable(false);
		serverUploadRateLimitUnitBox.setSelectedItem(serverUploadRateLimitUnit);
		serverPanel.add(serverUploadRateLimitUnitBox);
		serverPanel.add(new JLabel("<html>服务端同步路径:"));
		JTextField serverSyncDirectoryField = new JTextField(serverSyncDirectory);
		serverPanel.add(serverSyncDirectoryField);
		JCheckBox serverAutoStartBox = newJCheckBox(serverPanel, "<html>自动启动服务端", serverAutoStart);
		tabbedPane.addTab("<html>服务端设置", serverPanel);
		// 客户端选项卡
		JPanel clientPanel = new JPanel(new GridLayout(5, 2));
		clientPanel.add(new JLabel("<html>端口号:"));
		JTextField clientPortField = new JTextField(String.valueOf(clientPort));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel("<html>服务器地址:"));
		JTextField serverAddressField = new JTextField(serverAddress);
		clientPanel.add(serverAddressField);
		clientPanel.add(new JLabel("<html>客户端同步路径:"));
		JTextField clientSyncDirectoryField = new JTextField(clientSyncDirectory);
		clientPanel.add(clientSyncDirectoryField);
		clientPanel.add(new JLabel("<html>仅客户端模组路径:"));
		JTextField clientOnlyDirectoryField = new JTextField(clientOnlyDirectory);
		clientPanel.add(clientOnlyDirectoryField);
		JCheckBox clientAutoStartBox = newJCheckBox(clientPanel, "<html>自动启动客户端", clientAutoStart);
		tabbedPane.addTab("<html>客户端设置", clientPanel);
		// 按钮面板
		JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
		newJButton(
				buttonPanel, "保存", event -> {
					// 定义输入框数组及其对应的提示信息和选项卡索引，并检查输入框是否为空
					for (Object[] input : new Object[][]{
							{"服务端端口", serverPortField, 0},
							{"最大上传速率", serverUploadRateLimitField, 0},
							{"上传速率单位(每秒)", serverUploadRateLimitUnitBox, 0},
							{"服务端同步文件夹路径", serverSyncDirectoryField, 0},
							{"客户端端口", clientPortField, 1},
							{"服务器地址", serverAddressField, 1},
							{"客户端同步文件夹路径", clientSyncDirectoryField, 1},
							{"仅客户端模组文件夹路径", clientOnlyDirectoryField, 1}
					})
						if (input[1] instanceof JTextField) {
							JTextField textField = (JTextField) input[1];
							if (textField.getText().trim().isEmpty()) {
								tabbedPane.setSelectedIndex((int) input[2]); // 跳转到对应的选项卡
								selectAndFocus(textField);
								log(WARNING, input[0] + "不能为空");
								return;
							}
						}
					// 检测输入框是否为数字且在合法范围内并尝试转换
					if (!getPort(serverPortField.getText().trim(), true)) selectAndFocus(serverPortField);
					if (!getPort(clientPortField.getText().trim(), false)) selectAndFocus(clientPortField);
					// 检测最大上传速率
					String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
					if (invalidLong(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
						log(WARNING, "最大上传速率格式错误: " + uploadRateLimitText);
						tabbedPane.setSelectedIndex(0);
						selectAndFocus(serverUploadRateLimitField);
						return;
					}
					serverAutoStart = serverAutoStartBox.isSelected();
					serverUploadRateLimit = Long.parseLong(uploadRateLimitText);
					serverUploadRateLimitUnit = (String) serverUploadRateLimitUnitBox.getSelectedItem();
					serverSyncDirectory = serverSyncDirectoryField.getText().trim();
					clientAutoStart = clientAutoStartBox.isSelected();
					serverAddress = serverAddressField.getText().trim();
					clientSyncDirectory = clientSyncDirectoryField.getText().trim();
					clientOnlyDirectory = clientOnlyDirectoryField.getText().trim();
					saveConfig(); // 保存配置
					settingsJDialog.dispose(); // 关闭对话框
				}
		);
		newJButton(buttonPanel, "取消", event -> settingsJDialog.dispose());
		newJButton(buttonPanel, "关于", event -> aboutJDialog(settingsJDialog));
		settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
		settingsJDialog.add(settingsPanel);
		settingsJDialog.setSize(screenLength / 5, screenLength / 8);
		setWindow(settingsJDialog);
	}
	// 关于
	public static void aboutJDialog(Window parent) {
		if (checkJDialog("关于")) return;
		JDialog aboutDialog = new JDialog(parent, "关于");
		JTextPane aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("<span style=\"font-weight: bold;font-family: Arial;\">"
				+ HEX_SYNC_NAME
				+ "<br>By: ForgeStove<br>GitHub: <a href=\""
				+ GITHUB_URL
				+ "\">"
				+ GITHUB_URL
				+ "</a><br>开源许可: <a href=\"file:LICENSE\">GNU General Public License v3.0</a></span>");
		aboutTextPane.addHyperlinkListener(event -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) try {
				String url = event.getURL().toString();
				if (url.equals(GITHUB_URL)) {
					Desktop.getDesktop().browse(event.getURL().toURI());
				} else if (url.equals("file:LICENSE")) {
					if (checkJDialog("许可证")) return;
					try (
							BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
									HexSync.class.getResourceAsStream("LICENSE"))))
					) {
						StringBuilder licenseContent = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) licenseContent.append(line).append(lineSeparator());
						JTextArea licenseTextArea = new JTextArea(licenseContent.toString());
						licenseTextArea.setEditable(false);
						licenseTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						JDialog licenseJDialog = new JDialog(aboutDialog, "许可证");
						licenseJDialog.add(new JScrollPane(licenseTextArea));
						licenseJDialog.pack();
						setWindow(licenseJDialog);
					}
				}
			} catch (Exception error) {
				log(WARNING, "无法打开超链接: " + error.getMessage());
			}
		});
		aboutDialog.add(new JScrollPane(aboutTextPane));
		aboutDialog.pack();
		setWindow(aboutDialog);
	}
	// 聚焦并全选输入框
	public static void selectAndFocus(JTextField textField) {
		textField.requestFocus(); // 聚焦输入框
		textField.selectAll(); // 选中输入框
	}
	// 设置字体的通用方法
	public static void setFont(Container container, Font font) {
		for (Component component : container.getComponents()) {
			if (component instanceof Container) setFont((Container) component, font); // 递归设置子组件的字体
			component.setFont(font); // 设置字体
		}
	}
	// 检测是否有同名窗口并显示
	public static boolean checkJDialog(String title) {
		for (Window window : Window.getWindows()) {
			if (!(window instanceof JDialog)) continue;
			JDialog dialog = (JDialog) window;
			if (!dialog.getTitle().equals(title)) continue;
			dialog.setVisible(true);
			dialog.toFront();
			return true;
		}
		return false;
	}
	// 基础复选框框架
	public static JCheckBox newJCheckBox(JPanel panel, String text, boolean selected) {
		JCheckBox checkBox = new JCheckBox(text);
		checkBox.setFocusPainted(false);
		checkBox.setSelected(selected);
		panel.add(checkBox);
		return checkBox;
	}
	// 基础按钮框架
	public static void newJButton(JPanel panel, String text, ActionListener actionListener) {
		JButton button = new JButton("<html>" + text);
		button.setFocusPainted(false);
		button.setPreferredSize(new Dimension(0, screenLength / 55));
		button.addActionListener(actionListener);
		panel.add(button);
	}
	// 设置窗口属性
	public static void setWindow(Window window) {
		setFont(window, new Font("Arial", Font.PLAIN, 14));
		window.setIconImage(icon);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}
}