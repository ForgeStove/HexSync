package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.util.*;
import com.forgestove.hexsync.util.network.*;
import com.forgestove.hexsync.util.network.Rate.Unit;

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
		var serverPortField = new JTextField(String.valueOf(Data.serverPort.get().getValue()));
		var rateField = new JTextField(String.valueOf(Data.serverUploadRate.get().value));
		var rateUnitBox = new JComboBox<>(Unit.values()) {{setSelectedItem(Data.serverUploadRate.get().unit);}};
		var serverSyncField = new JTextField(Data.serverSyncPath.get().toString());
		var serverAutoBox = new JCheckBox(HexSync.get("Settings.autoStart"), Data.serverAuto.get());
		var serverPanel = new JPanel(layout) {{
			setFocusable(false);
			add(new JLabel(HexSync.get("Settings.port")));
			add(serverPortField);
			add(new JLabel(HexSync.get("Settings.maxUploadRate")));
			add(rateField);
			add(new JLabel(HexSync.get("Settings.rateUnit")));
			add(rateUnitBox);
			add(new JLabel(HexSync.get("Settings.serverSyncPath")));
			add(serverSyncField);
			add(serverAutoBox);
		}};
		// 客户端选项卡
		var clientPortField = new JTextField(String.valueOf(Data.clientPort.get().getValue()));
		var remoteAddressField = new JTextField(Data.remoteAddress.get());
		var clientSyncField = new JTextField(Data.clientSyncPath.get().toString());
		var clientOnlyField = new JTextField(Data.clientOnlyPath.get().toString());
		var clientAutoBox = new JCheckBox(HexSync.get("Settings.autoStart"), Data.clientAuto.get());
		var clientPanel = new JPanel(layout) {{
			setFocusable(false);
			add(new JLabel(HexSync.get("Settings.port")));
			add(clientPortField);
			add(new JLabel(HexSync.get("Settings.remoteAddress")));
			add(remoteAddressField);
			add(new JLabel(HexSync.get("Settings.clientSyncPath")));
			add(clientSyncField);
			add(new JLabel(HexSync.get("Settings.clientOnlyPath")));
			add(clientOnlyField);
			add(clientAutoBox);
		}};
		// 界面选项卡
		var themeBox = new JComboBox<>(Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(LookAndFeelInfo::getName)
			.toArray(String[]::new)) {{setSelectedItem(Data.theme.get());}};
		var uiPanel = new JPanel(layout);
		uiPanel.setFocusable(false);
		uiPanel.add(new JLabel(HexSync.get("Settings.theme")));
		uiPanel.add(themeBox);
		// 选项卡
		var tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT) {{
			addChangeListener(event -> SwingUtilities.invokeLater(SettingJDialog.this::pack));
			addTab(HexSync.get("Settings.serverSettings"), serverPanel);
			addTab(HexSync.get("Settings.clientSettings"), clientPanel);
			addTab(HexSync.get("Settings.uiSettings"), uiPanel);
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
				if (input[1] instanceof JTextField textField && textField.getText().isEmpty()) {
					tabbedPane.setSelectedIndex((int) input[2]); // 跳转到对应的选项卡
					Component.selectAndFocus(textField);
					Log.warn(HexSync.get("Error.invalidFormat") + input[0]);
					return;
				}
			// 检测输入框是否为数字且在合法范围内并尝试转换
			var serverPortResult = TypeConverter.tryConvertWithResult(serverPortField.getText(), Port::new);
			if (!serverPortResult.isSuccess) {
				Log.warn("%s%s%s".formatted(HexSync.get("Error.invalidFormat"), HexSync.get("Settings.port"), serverPortField.getText()));
				tabbedPane.setSelectedIndex(0);
				Component.selectAndFocus(serverPortField);
				return;
			}
			// 检测最大上传速率
			var rateText = rateField.getText();
			var rateResult = TypeConverter.tryToLong(rateText);
			if (!rateResult.isSuccess || rateResult.value < 0) {
				Log.warn("%s%s%s".formatted(HexSync.get("Error.invalidFormat"), HexSync.get("Settings.maxUploadRate"), rateText));
				tabbedPane.setSelectedIndex(0);
				Component.selectAndFocus(rateField);
				return;
			}
			Data.serverPort.set(new Port(serverPortField.getText()));
			Data.serverAuto.set(serverAutoBox.isSelected());
			Data.serverUploadRate.set(new Rate(rateText, (Unit) Objects.requireNonNull(rateUnitBox.getSelectedItem())));
			Data.serverSyncPath.set(Path.of(serverSyncField.getText()));
			Data.clientPort.set(new Port(clientPortField.getText()));
			Data.clientAuto.set(clientAutoBox.isSelected());
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
