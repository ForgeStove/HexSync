package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;

import javax.swing.*;
import java.awt.*;
public class SettingsJDialog {
	// 打开设置对话框
	public static void initSettingsJDialog() {
		if (CComponent.checkJDialog("设置")) return;
		Config.loadConfig();
		// 设置对话框
		var settingsJDialog = new JDialog(GUI.frame, "设置");
		var settingsPanel = new JPanel(new BorderLayout());
		settingsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// 选项卡面板
		var tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setFocusable(false);
		settingsPanel.add(tabbedPane, BorderLayout.CENTER);
		// 服务端选项卡
		var serverPanel = new JPanel(new GridLayout(5, 2));
		serverPanel.add(new JLabel("<html>端口号:"));
		var serverPortField = new JTextField(String.valueOf(Server.serverPort));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel("<html>最大上传速率:"));
		var serverUploadRateLimitField = new JTextField(String.valueOf(Config.serverUploadRateLimit));
		serverPanel.add(serverUploadRateLimitField);
		serverPanel.add(new JLabel("<html>上传速率单位:"));
		var serverUploadRateLimitUnitBox = new JComboBox<>(new String[]{
			RateUnit.BPS.unit, RateUnit.KBPS.unit, RateUnit.MBPS.unit, RateUnit.GBPS.unit
		});
		serverUploadRateLimitUnitBox.setFocusable(false);
		serverUploadRateLimitUnitBox.setSelectedItem(Config.serverUploadRateLimitUnit.unit);
		serverPanel.add(serverUploadRateLimitUnitBox);
		serverPanel.add(new JLabel("<html>服务端同步路径:"));
		var serverSyncDirectoryField = new JTextField(Config.serverSyncDirectory);
		serverPanel.add(serverSyncDirectoryField);
		var serverAutoStartBox = CComponent.newJCheckBox(serverPanel, "<html>自动启动服务端", Server.serverAutoStart);
		tabbedPane.addTab("<html>服务端设置", serverPanel);
		// 客户端选项卡
		var clientPanel = new JPanel(new GridLayout(5, 2));
		clientPanel.add(new JLabel("<html>端口号:"));
		var clientPortField = new JTextField(String.valueOf(Client.clientPort));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel("<html>服务器地址:"));
		var serverAddressField = new JTextField(Config.serverAddress);
		clientPanel.add(serverAddressField);
		clientPanel.add(new JLabel("<html>客户端同步路径:"));
		var clientSyncDirectoryField = new JTextField(Config.clientSyncDirectory);
		clientPanel.add(clientSyncDirectoryField);
		clientPanel.add(new JLabel("<html>仅客户端模组路径:"));
		var clientOnlyDirectoryField = new JTextField(Config.clientOnlyDirectory);
		clientPanel.add(clientOnlyDirectoryField);
		var clientAutoStartBox = CComponent.newJCheckBox(clientPanel, "<html>自动启动客户端", Client.clientAutoStart);
		tabbedPane.addTab("<html>客户端设置", clientPanel);
		// 按钮面板
		var buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
		CComponent.newJButton(
			buttonPanel, "保存", event -> {
				// 定义输入框数组及其对应的提示信息和选项卡索引，并检查输入框是否为空
				for (var input : new Object[][]{
					{"服务端端口", serverPortField, 0},
					{"最大上传速率", serverUploadRateLimitField, 0},
					{"上传速率单位", serverUploadRateLimitUnitBox, 0},
					{"服务端同步路径", serverSyncDirectoryField, 0},
					{"客户端端口", clientPortField, 1},
					{"服务器地址", serverAddressField, 1},
					{"客户端同步路径", clientSyncDirectoryField, 1},
					{"仅客户端模组路径", clientOnlyDirectoryField, 1}
				})
					if (input[1] instanceof JTextField textField) {
						if (textField.getText().trim().isEmpty()) {
							tabbedPane.setSelectedIndex((int) input[2]); // 跳转到对应的选项卡
							CComponent.selectAndFocus(textField);
							Log.warn(input[0] + "不能为空");
							return;
						}
					}
				// 检测输入框是否为数字且在合法范围内并尝试转换
				if (!Settings.canSetPort(serverPortField.getText().trim(), true)) CComponent.selectAndFocus(serverPortField);
				if (!Settings.canSetPort(clientPortField.getText().trim(), false)) CComponent.selectAndFocus(clientPortField);
				// 检测最大上传速率
				var uploadRateLimitText = serverUploadRateLimitField.getText().trim();
				if (Settings.isInvalidLong(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
					Log.warn("最大上传速率格式错误: " + uploadRateLimitText);
					tabbedPane.setSelectedIndex(0);
					CComponent.selectAndFocus(serverUploadRateLimitField);
					return;
				}
				Server.serverAutoStart = serverAutoStartBox.isSelected();
				Config.serverUploadRateLimit = Long.parseLong(uploadRateLimitText);
				Config.serverUploadRateLimitUnit = RateUnit.fromUnit((String) serverUploadRateLimitUnitBox.getSelectedItem());
				Config.serverSyncDirectory = serverSyncDirectoryField.getText().trim();
				Client.clientAutoStart = clientAutoStartBox.isSelected();
				Config.serverAddress = serverAddressField.getText().trim();
				Config.clientSyncDirectory = clientSyncDirectoryField.getText().trim();
				Config.clientOnlyDirectory = clientOnlyDirectoryField.getText().trim();
				Config.saveConfig(); // 保存配置
				settingsJDialog.dispose(); // 关闭对话框
			}
		);
		CComponent.newJButton(buttonPanel, "取消", event -> settingsJDialog.dispose());
		CComponent.newJButton(buttonPanel, "关于", event -> AboutJDialog.initAboutJDialog(settingsJDialog));
		settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
		settingsJDialog.add(settingsPanel);
		settingsJDialog.setSize(GUI.screenLength / 5, GUI.screenLength / 8);
		CComponent.setWindow(settingsJDialog);
	}
}
