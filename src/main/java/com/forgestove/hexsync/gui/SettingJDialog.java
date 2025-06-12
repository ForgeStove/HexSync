package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.util.*;
import com.forgestove.hexsync.util.Rate.Unit;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.util.Arrays;
public class SettingJDialog extends JDialog {
	// 打开设置对话框
	public SettingJDialog(Window owner, String title) {
		super(owner, title, ModalityType.MODELESS);
		ConfigUtil.loadConfig();
		// 设置对话框
		var settingPanel = new JPanel(new BorderLayout());
		settingPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		// 选项卡面板
		var tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tabbedPane.setFocusable(false);
		settingPanel.add(tabbedPane, BorderLayout.CENTER);
		// 设置布局
		var layout = new GridLayout(0, 2);
		// 服务端选项卡
		var serverPanel = new JPanel(layout);
		serverPanel.add(new JLabel("端口号:"));
		var serverPortField = new JTextField(String.valueOf(Data.serverPort));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel("最大上传速率:"));
		var rateField = new JTextField(String.valueOf(Data.serverUploadRate.get().value));
		serverPanel.add(rateField);
		serverPanel.add(new JLabel("上传速率单位:"));
		var rateUnitBox = new JComboBox<>(Unit.values());
		rateUnitBox.setFocusable(false);
		rateUnitBox.setSelectedItem(Data.serverUploadRate.get().unit);
		serverPanel.add(rateUnitBox);
		serverPanel.add(new JLabel("服务端同步路径:"));
		var serverSyncField = new JTextField(Data.serverSyncDirectory.get());
		serverPanel.add(serverSyncField);
		var serverAutoStartBox = ComponentUtil.newJCheckBox(serverPanel, "自动启动服务端", Data.serverAutoStart.get());
		tabbedPane.addTab("服务端设置", serverPanel);
		// 客户端选项卡
		var clientPanel = new JPanel(layout);
		clientPanel.add(new JLabel("端口号:"));
		var clientPortField = new JTextField(String.valueOf(Data.clientPort));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel("远程地址:"));
		var remoteAddressField = new JTextField(Data.remoteAddress.get());
		clientPanel.add(remoteAddressField);
		clientPanel.add(new JLabel("客户端同步路径:"));
		var clientSyncField = new JTextField(Data.clientSyncDirectory.get());
		clientPanel.add(clientSyncField);
		clientPanel.add(new JLabel("仅客户端模组路径:"));
		var clientOnlyField = new JTextField(Data.clientOnlyDirectory.get());
		clientPanel.add(clientOnlyField);
		var clientAutoStartBox = ComponentUtil.newJCheckBox(clientPanel, "自动启动客户端", Data.clientAutoStart.get());
		tabbedPane.addTab("客户端设置", clientPanel);
		// 其他选项卡
		var otherPanel = new JPanel(layout);
		otherPanel.add(new JLabel("主题:"));
		var themeBox = new JComboBox<>(Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(LookAndFeelInfo::getName)
			.toArray(String[]::new));
		themeBox.setFocusable(false);
		themeBox.setSelectedItem(Data.theme.get());
		otherPanel.add(themeBox);
		otherPanel.add(new JLabel());
		otherPanel.add(new JLabel());
		otherPanel.add(new JLabel());
		otherPanel.add(new JLabel());
		otherPanel.add(new JLabel());
		tabbedPane.addTab("其他设置", otherPanel);
		// 按钮面板
		var buttonPanel = new JPanel(new GridLayout(1, 0));
		ComponentUtil.newJButton(buttonPanel, "保存", event -> {
			for (var input : new Object[][]{
				{"服务端端口", serverPortField, 0},
				{"最大上传速率", rateField, 0},
				{"上传速率单位", rateUnitBox, 0},
				{"服务端同步路径", serverSyncField, 0},
				{"客户端端口", clientPortField, 1},
				{"远程地址", remoteAddressField, 1},
				{"客户端同步路径", clientSyncField, 1},
				{"仅客户端模组路径", clientOnlyField, 1}
			})
				if (input[1] instanceof JTextField textField && textField.getText().trim().isEmpty()) {
					tabbedPane.setSelectedIndex((int) input[2]); // 跳转到对应的选项卡
					ComponentUtil.selectAndFocus(textField);
					Log.warn("%s不能为空", input[0]);
					return;
				}
			// 检测输入框是否为数字且在合法范围内并尝试转换
			if (!SettingUtil.canSetPort(new Port(serverPortField.getText().trim()), true)) ComponentUtil.selectAndFocus(serverPortField);
			if (!SettingUtil.canSetPort(new Port(clientPortField.getText().trim()), false)) ComponentUtil.selectAndFocus(clientPortField);
			// 检测最大上传速率
			var rateText = rateField.getText().trim();
			if (SettingUtil.isInvalidLong(rateText) || Long.parseLong(rateText) < 0) {
				Log.warn("最大上传速率格式错误: " + rateText);
				tabbedPane.setSelectedIndex(0);
				ComponentUtil.selectAndFocus(rateField);
				return;
			}
			Data.serverAutoStart.set(serverAutoStartBox.isSelected());
			Data.serverUploadRate.set(new Rate(Long.parseLong(rateText), (Unit) rateUnitBox.getSelectedItem()));
			Data.serverSyncDirectory.set(serverSyncField.getText().trim());
			Data.clientAutoStart.set(clientAutoStartBox.isSelected());
			Data.remoteAddress.set(remoteAddressField.getText().trim());
			Data.clientSyncDirectory.set(clientSyncField.getText().trim());
			Data.clientOnlyDirectory.set(clientOnlyField.getText().trim());
			var themeItem = (String) themeBox.getSelectedItem();
			if (!Data.theme.get().equals(themeItem)) {
				Data.theme.set(themeItem);
				ComponentUtil.setTheme(ComponentUtil.getClassName(themeItem));
				for (var window : getWindows()) SwingUtilities.updateComponentTreeUI(window); // 更新所有窗口的UI
			}
			ConfigUtil.saveConfig(); // 保存配置
			dispose(); // 关闭对话框
		});
		ComponentUtil.newJButton(buttonPanel, "取消", event -> dispose());
		ComponentUtil.newJButton(buttonPanel, "关于", event -> new AboutJDialog(owner, "关于"));
		settingPanel.add(buttonPanel, BorderLayout.SOUTH);
		add(settingPanel);
		setMinimumSize(new Dimension(320, 0));
		ComponentUtil.setWindow(this);
	}
}
