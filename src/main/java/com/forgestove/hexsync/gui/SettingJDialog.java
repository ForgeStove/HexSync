package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.*;
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
		// 服务端选项
		var serverPortField = new JTextField(String.valueOf(Data.serverPort.get().getValue()));
		var rateField = new JTextField(String.valueOf(Data.serverUploadRate.get().value));
		var rateUnitBox = new JComboBox<>(Unit.values()) {{setSelectedItem(Data.serverUploadRate.get().unit);}};
		var serverSyncField = new JTextField(Data.serverSyncPath.get().toString());
		var serverAutoBox = new JCheckBox(HexSync.get("Setting.autoStart"), Data.serverAuto.get());
		// 客户端选项
		var clientPortField = new JTextField(String.valueOf(Data.clientPort.get().getValue()));
		var remoteAddressField = new JTextField(Data.remoteAddress.get());
		var clientSyncField = new JTextField(Data.clientSyncPath.get().toString());
		var clientOnlyField = new JTextField(Data.clientOnlyPath.get().toString());
		var clientAutoBox = new JCheckBox(HexSync.get("Setting.autoStart"), Data.clientAuto.get());
		// 界面选项
		var themeBox = new JComboBox<>(Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(LookAndFeelInfo::getName)
			.toArray(String[]::new)) {{setSelectedItem(Data.theme.get());}};
		// 基础面板
		add(new JPanel(new BorderLayout()) {{
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			// 选项卡
			add(new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT) {{
				addChangeListener(event -> SwingUtilities.invokeLater(SettingJDialog.this::pack));
				// 设置布局
				var layout = new GridLayout(0, 2);
				// 服务端选项卡
				addTab(HexSync.get("Setting.serverSettings"), new JPanel(layout) {{
					setFocusable(false);
					add(new JLabel(HexSync.get("Setting.port")));
					add(serverPortField);
					add(new JLabel(HexSync.get("Setting.maxUploadRate")));
					add(rateField);
					add(new JLabel(HexSync.get("Setting.rateUnit")));
					add(rateUnitBox);
					add(new JLabel(HexSync.get("Setting.serverSyncPath")));
					add(serverSyncField);
					add(serverAutoBox);
				}});
				// 客户端选项卡
				addTab(HexSync.get("Setting.clientSettings"), new JPanel(layout) {{
					setFocusable(false);
					add(new JLabel(HexSync.get("Setting.port")));
					add(clientPortField);
					add(new JLabel(HexSync.get("Setting.remoteAddress")));
					add(remoteAddressField);
					add(new JLabel(HexSync.get("Setting.clientSyncPath")));
					add(clientSyncField);
					add(new JLabel(HexSync.get("Setting.clientOnlyPath")));
					add(clientOnlyField);
					add(clientAutoBox);
				}});
				// 界面选项卡
				addTab(HexSync.get("Setting.uiSettings"), new JPanel(layout) {{
					setFocusable(false);
					add(new JLabel(HexSync.get("Setting.theme")));
					add(themeBox);
				}});
			}}, BorderLayout.CENTER);
			// 按钮面板
			add(new JPanel(new GridLayout(1, 0)) {{
				add(new CButton(HexSync.get("Setting.save"), event2 -> {
					Data.serverPort.set(new Port(serverPortField.getText()));
					Data.serverAuto.set(serverAutoBox.isSelected());
					Data.serverUploadRate.set(new Rate(rateField.getText(), (Unit) Objects.requireNonNull(rateUnitBox.getSelectedItem())));
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
				}));
				add(new CButton(HexSync.get("Setting.cancel"), event1 -> dispose()));
				add(new CButton(HexSync.get("Setting.about"), event -> new AboutJDialog(owner, HexSync.get("About.title"))));
				setPreferredSize(new Dimension(0, 40));
			}}, BorderLayout.SOUTH);
		}});
		Component.setWindow(this);
	}
}
