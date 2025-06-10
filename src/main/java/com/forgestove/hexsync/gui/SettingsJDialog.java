package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Config;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;

import javax.swing.*;
import java.awt.*;
public class SettingsJDialog {
	// 打开设置对话框
	public SettingsJDialog() {
		if (ComponentUtil.checkJDialog("设置")) return;
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
			RateUnit.BPS.name(), RateUnit.KBPS.name(), RateUnit.MBPS.name(), RateUnit.GBPS.name()
		});
		serverUploadRateLimitUnitBox.setFocusable(false);
		serverUploadRateLimitUnitBox.setSelectedItem(Config.serverUploadRateLimitUnit.name());
		serverPanel.add(serverUploadRateLimitUnitBox);
		serverPanel.add(new JLabel("<html>服务端同步路径:"));
		var serverSyncDirectoryField = new JTextField(Config.serverSyncDirectory);
		serverPanel.add(serverSyncDirectoryField);
		var serverAutoStartBox = ComponentUtil.newJCheckBox(serverPanel, "<html>自动启动服务端", Server.serverAutoStart);
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
		var clientAutoStartBox = ComponentUtil.newJCheckBox(clientPanel, "<html>自动启动客户端", Client.clientAutoStart);
		tabbedPane.addTab("<html>客户端设置", clientPanel);
		// 按钮面板
		var buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
		ComponentUtil.newJButton(
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
							ComponentUtil.selectAndFocus(textField);
							Log.warn("%s不能为空", input[0]);
							return;
						}
					}
				// 检测输入框是否为数字且在合法范围内并尝试转换
				if (!Settings.canSetPort(serverPortField.getText().trim(), true)) ComponentUtil.selectAndFocus(serverPortField);
				if (!Settings.canSetPort(clientPortField.getText().trim(), false)) ComponentUtil.selectAndFocus(clientPortField);
				// 检测最大上传速率
				var uploadRateLimitText = serverUploadRateLimitField.getText().trim();
				if (Settings.isInvalidLong(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
					Log.warn("最大上传速率格式错误: " + uploadRateLimitText);
					tabbedPane.setSelectedIndex(0);
					ComponentUtil.selectAndFocus(serverUploadRateLimitField);
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
		ComponentUtil.newJButton(buttonPanel, "取消", event -> settingsJDialog.dispose());
		ComponentUtil.newJButton(buttonPanel, "关于", event -> new AboutJDialog(GUI.frame, "关于"));
		settingsPanel.add(buttonPanel, BorderLayout.SOUTH);
		settingsJDialog.add(settingsPanel);
		settingsJDialog.setMinimumSize(new Dimension(320, 240));
		settingsJDialog.pack();
		ComponentUtil.setWindow(settingsJDialog);
	}
}
