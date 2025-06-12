package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.util.*;
import com.forgestove.hexsync.util.Rate.Unit;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.util.*;
public class SettingJDialog extends JDialog {
	// 打开设置对话框
	public SettingJDialog(Window owner, String title) {
		super(owner, title, ModalityType.MODELESS);
		ConfigUtil.loadConfig();
		// 设置对话框
		var settingPanel = new JPanel(new BorderLayout());
		settingPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		// 选项卡面板
		var tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		settingPanel.add(tabbedPane, BorderLayout.CENTER);
		// 设置布局
		var layout = new GridLayout(0, 2);
		// 服务端选项卡
		var serverPanel = new JPanel(layout);
		serverPanel.add(new JLabel(HexSync.lang.getString("Settings.port")));
		var serverPortField = new JTextField(String.valueOf(Data.serverPort));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel(HexSync.lang.getString("Settings.maxUploadRate")));
		var rateField = new JTextField(String.valueOf(Data.serverUploadRate.get().value));
		serverPanel.add(rateField);
		serverPanel.add(new JLabel(HexSync.lang.getString("Settings.rateUnit")));
		var rateUnitBox = new JComboBox<>(Unit.values());
		rateUnitBox.setSelectedItem(Data.serverUploadRate.get().unit);
		serverPanel.add(rateUnitBox);
		serverPanel.add(new JLabel(HexSync.lang.getString("Settings.serverSyncPath")));
		var serverSyncField = new JTextField(Data.serverSyncDirectory.get());
		serverPanel.add(serverSyncField);
		var serverAutoStartBox = new JCheckBox(HexSync.lang.getString("Settings.autoStartServer"), Data.serverAutoStart.get());
		serverPanel.add(serverAutoStartBox);
		tabbedPane.addTab(HexSync.lang.getString("Settings.serverSettings"), serverPanel);
		// 客户端选项卡
		var clientPanel = new JPanel(layout);
		clientPanel.add(new JLabel(HexSync.lang.getString("Settings.port")));
		var clientPortField = new JTextField(String.valueOf(Data.clientPort));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel(HexSync.lang.getString("Settings.remoteAddress")));
		var remoteAddressField = new JTextField(Data.remoteAddress.get());
		clientPanel.add(remoteAddressField);
		clientPanel.add(new JLabel(HexSync.lang.getString("Settings.clientSyncPath")));
		var clientSyncField = new JTextField(Data.clientSyncDirectory.get());
		clientPanel.add(clientSyncField);
		// 保留原有"仅客户端模组路径"标签，并添加到属性文件
		clientPanel.add(new JLabel(HexSync.lang.getString("Settings.clientOnlyPath")));
		var clientOnlyField = new JTextField(Data.clientOnlyDirectory.get());
		clientPanel.add(clientOnlyField);
		var clientAutoStartBox = new JCheckBox(HexSync.lang.getString("Settings.autoStartClient"), Data.clientAutoStart.get());
		clientPanel.add(clientAutoStartBox);
		tabbedPane.addTab(HexSync.lang.getString("Settings.clientSettings"), clientPanel);
		// 其他选项卡
		var otherPanel = new JPanel(layout);
		otherPanel.add(new JLabel(HexSync.lang.getString("Settings.theme")));
		var themeBox = new JComboBox<>(Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(LookAndFeelInfo::getName)
			.toArray(String[]::new));
		themeBox.setSelectedItem(Data.theme.get());
		otherPanel.add(themeBox);
		var languageBox = new JComboBox<>(Locale.getAvailableLocales());
		languageBox.setSelectedItem(HexSync.lang.getLocale());
		otherPanel.add(new JLabel(HexSync.lang.getString("Settings.language")));
		otherPanel.add(languageBox);
		otherPanel.add(new JLabel());
		otherPanel.add(new JLabel());
		otherPanel.add(new JLabel());
		otherPanel.add(new JLabel());
		tabbedPane.addTab(HexSync.lang.getString("Settings.uiSettings"), otherPanel);
		// 按钮面板
		var buttonPanel = new JPanel(new GridLayout(1, 0));
		buttonPanel.add(new CButton(HexSync.lang.getString("Settings.save"), event -> {
			for (var input : new Object[][]{
				{HexSync.lang.getString("Settings.port"), serverPortField, 0},
				{HexSync.lang.getString("Settings.maxUploadRate"), rateField, 0},
				{HexSync.lang.getString("Settings.rateUnit"), rateUnitBox, 0},
				{HexSync.lang.getString("Settings.serverSyncPath"), serverSyncField, 0},
				{HexSync.lang.getString("Settings.port"), clientPortField, 1},
				{HexSync.lang.getString("Settings.remoteAddress"), remoteAddressField, 1},
				{HexSync.lang.getString("Settings.clientSyncPath"), clientSyncField, 1},
				{HexSync.lang.getString("Settings.clientOnlyPath"), clientOnlyField, 1}
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
				Log.warn(HexSync.lang.getString("Settings.maxUploadRate") + "格式错误: " + rateText);
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
				// 更新所有窗口的UI
				Arrays.stream(getWindows()).forEach(window -> {
					SwingUtilities.updateComponentTreeUI(window);
					window.setIconImage(GUI.icon.getImage());
				});
			}
			ConfigUtil.saveConfig(); // 保存配置
			dispose(); // 关闭对话框
		}));
		buttonPanel.add(new CButton(HexSync.lang.getString("Settings.cancel"), event -> dispose()));
		buttonPanel.add(new CButton(HexSync.lang.getString("Settings.about"),
			event -> new AboutJDialog(owner, HexSync.lang.getString("About.title"))));
		settingPanel.add(buttonPanel, BorderLayout.SOUTH);
		add(settingPanel);
		setMinimumSize(new Dimension(360, 300));
		ComponentUtil.setWindow(this);
	}
}
