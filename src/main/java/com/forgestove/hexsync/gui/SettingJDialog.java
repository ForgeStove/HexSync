package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.util.*;
import com.forgestove.hexsync.util.Rate.Unit;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
public class SettingJDialog extends JDialog {
	// 打开设置对话框
	public SettingJDialog(Window owner, String title) {
		super(owner, title, ModalityType.MODELESS);
		ConfigUtil.loadConfig();
		// 设置布局
		var layout = new GridLayout(0, 2);
		// 服务端选项卡
		var serverPanel = new JPanel(layout);
		var serverPortField = new JTextField(String.valueOf(Data.serverPort));
		var rateField = new JTextField(String.valueOf(Data.serverUploadRate.get().value));
		var rateUnitBox = new JComboBox<>(Unit.values()) {{setSelectedItem(Data.serverUploadRate.get().unit);}};
		var serverSyncField = new JTextField(Data.serverSyncPath.get().toString());
		var serverAutoStartBox = new JCheckBox(HexSync.get("Settings.autoStartServer"), Data.serverAuto.get());
		serverPanel.add(new JLabel(HexSync.get("Settings.port")));
		serverPanel.add(serverPortField);
		serverPanel.add(new JLabel(HexSync.get("Settings.maxUploadRate")));
		serverPanel.add(rateField);
		serverPanel.add(new JLabel(HexSync.get("Settings.rateUnit")));
		serverPanel.add(rateUnitBox);
		serverPanel.add(new JLabel(HexSync.get("Settings.serverSyncPath")));
		serverPanel.add(serverSyncField);
		serverPanel.add(serverAutoStartBox);
		// 客户端选项卡
		var clientPortField = new JTextField(String.valueOf(Data.clientPort));
		var remoteAddressField = new JTextField(Data.remoteAddress.get());
		var clientSyncField = new JTextField(Data.clientSyncPath.get().toString());
		var clientOnlyField = new JTextField(Data.clientOnlyPath.get().toString());
		var clientAutoStartBox = new JCheckBox(HexSync.get("Settings.autoStartClient"), Data.clientAuto.get());
		var clientPanel = new JPanel(layout);
		clientPanel.add(new JLabel(HexSync.get("Settings.port")));
		clientPanel.add(clientPortField);
		clientPanel.add(new JLabel(HexSync.get("Settings.remoteAddress")));
		clientPanel.add(remoteAddressField);
		clientPanel.add(new JLabel(HexSync.get("Settings.clientSyncPath")));
		clientPanel.add(clientSyncField);
		clientPanel.add(new JLabel(HexSync.get("Settings.clientOnlyPath")));
		clientPanel.add(clientOnlyField);
		clientPanel.add(clientAutoStartBox);
		// 其他选项卡
		var otherPanel = new JPanel(layout);
		var themeBox = new JComboBox<>(Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(LookAndFeelInfo::getName)
			.toArray(String[]::new)) {{setSelectedItem(Data.theme.get());}};
		otherPanel.add(new JLabel(HexSync.get("Settings.theme")));
		otherPanel.add(themeBox);
		// 选项卡
		var tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT) {{
			addChangeListener(event -> SwingUtilities.invokeLater(SettingJDialog.this::pack));
			addTab(HexSync.get("Settings.serverSettings"), serverPanel);
			addTab(HexSync.get("Settings.clientSettings"), clientPanel);
			addTab(HexSync.get("Settings.uiSettings"), otherPanel);
		}};
		// 按钮
		var save = new CButton(HexSync.get("Settings.save"), event -> {
			for (var input : new Object[][]{
				{HexSync.get("Settings.port"), serverPortField, 0},
				{HexSync.get("Settings.maxUploadRate"), rateField, 0},
				{HexSync.get("Settings.rateUnit"), rateUnitBox, 0},
				{HexSync.get("Settings.serverSyncPath"), serverSyncField, 0},
				{HexSync.get("Settings.port"), clientPortField, 1},
				{HexSync.get("Settings.remoteAddress"), remoteAddressField, 1},
				{HexSync.get("Settings.clientSyncPath"), clientSyncField, 1},
				{HexSync.get("Settings.clientOnlyPath"), clientOnlyField, 1}
			})
				if (input[1] instanceof JTextField textField && textField.getText().trim().isEmpty()) {
					tabbedPane.setSelectedIndex((int) input[2]); // 跳转到对应的选项卡
					Component.selectAndFocus(textField);
					Log.warn(HexSync.get("Error.invalidFormat") + input[0]);
					return;
				}
			// 检测输入框是否为数字且在合法范围内并尝试转换
			SettingUtil.setPort(new Port(serverPortField.getText().trim()), true);
			SettingUtil.setPort(new Port(clientPortField.getText().trim()), false);
			// 检测最大上传速率
			var rateText = rateField.getText().trim();
			if (SettingUtil.isInvalidLong(rateText) || Long.parseLong(rateText) < 0) {
				Log.warn("%s%s%s".formatted(HexSync.get("Error.invalidFormat"), HexSync.get("Settings.maxUploadRate"), rateText));
				tabbedPane.setSelectedIndex(0);
				Component.selectAndFocus(rateField);
				return;
			}
			Data.serverAuto.set(serverAutoStartBox.isSelected());
			Data.serverUploadRate.set(new Rate(rateText, (Unit) Objects.requireNonNull(rateUnitBox.getSelectedItem())));
			Data.serverSyncPath.set(Path.of(serverSyncField.getText()));
			Data.clientAuto.set(clientAutoStartBox.isSelected());
			Data.remoteAddress.set(remoteAddressField.getText());
			Data.clientSyncPath.set(Path.of(clientSyncField.getText()));
			Data.clientOnlyPath.set(Path.of(clientOnlyField.getText()));
			var themeItem = (String) themeBox.getSelectedItem();
			if (!Data.theme.get().equals(themeItem)) {
				Data.theme.set(themeItem);
				Component.setTheme(themeItem);
			}
			ConfigUtil.saveConfig(); // 保存配置
			dispose(); // 关闭对话框
		});
		var cancel = new CButton(HexSync.get("Settings.cancel"), event -> dispose());
		var about = new CButton(HexSync.get("Settings.about"), event -> new AboutJDialog(owner, HexSync.get("About.title")));
		// 按钮面板
		add(new JPanel(new BorderLayout()) {{
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			add(tabbedPane, BorderLayout.CENTER);
			add(new JPanel(new GridLayout(1, 0)) {{
				add(save);
				add(cancel);
				add(about);
				setPreferredSize(new Dimension(0, 40));
			}}, BorderLayout.SOUTH);
		}});
		Component.setWindow(this);
	}
}
