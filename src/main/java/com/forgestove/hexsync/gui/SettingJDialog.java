package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.util.network.*;
import com.forgestove.hexsync.util.network.Rate.Unit;
import com.formdev.flatlaf.ui.FlatRoundBorder;
import org.jetbrains.annotations.*;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.*;
public class SettingJDialog extends JDialog {
	// 打开设置对话框
	public SettingJDialog(Window owner, String title) {
		super(owner, title, ModalityType.MODELESS);
		ConfigUtil.load();
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
		//
		serverPortField.setInputVerifier(new InputVerifier() {
			@Override
			public boolean verify(JComponent input) {
				var textField = (JTextField) input;
				var text = textField.getText();
				try {
					new Port(text);
					textField.setToolTipText(null);
					return true;
				} catch (Exception error) {
					textField.setToolTipText(error.getMessage());
					return false;
				}
			}
			@Override
			public boolean shouldYieldFocus(JComponent source, JComponent target) {
				if (verify(source)) return true;
				if (!(source instanceof JTextField textField)) return false;
				ToolTipManager.sharedInstance().setInitialDelay(0);
				var tip = textField.getToolTipText();
				if (tip != null) {
					var toolTip = textField.createToolTip();
					toolTip.setTipText(tip);
					toolTip.setVisible(true);
				}
				if (target instanceof JTextField) target.getInputVerifier();
				return false;
			}
		});
		// 基础面板
		add(new JPanel(new BorderLayout()) {{
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			add(new JPanel() {{
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				var layout = new GridLayout(0, 2);
				// 服务端设置区域
				add(new JPanel(layout) {{
					setBorder(SettingJDialog.getBorder("Setting.server"));
					add(new JLabel(HexSync.get("Setting.port")));
					add(serverPortField);
					add(new JLabel(HexSync.get("Setting.maxUploadRate")));
					add(rateField);
					add(new JLabel(HexSync.get("Setting.rateUnit")));
					add(rateUnitBox);
					add(new JLabel(HexSync.get("Setting.serverSyncPath")));
					add(serverSyncField);
					add(serverAutoBox);
					add(new JLabel());
				}});
				// 客户端设置区域
				add(new JPanel(layout) {{
					setBorder(SettingJDialog.getBorder("Setting.client"));
					add(new JLabel(HexSync.get("Setting.port")));
					add(clientPortField);
					add(new JLabel(HexSync.get("Setting.remoteAddress")));
					add(remoteAddressField);
					add(new JLabel(HexSync.get("Setting.clientSyncPath")));
					add(clientSyncField);
					add(new JLabel(HexSync.get("Setting.clientOnlyPath")));
					add(clientOnlyField);
					add(clientAutoBox);
					add(new JLabel());
				}});
				// 界面设置区域
				add(new JPanel(layout) {{
					setBorder(SettingJDialog.getBorder("Setting.ui"));
					add(new JLabel(HexSync.get("Setting.theme")));
					add(themeBox);
				}});
			}}, BorderLayout.CENTER);
			// 按钮面板
			add(new JPanel(new GridLayout(1, 0, 5, 0)) {{
				add(new CButton(HexSync.get("Setting.save"), event -> {
					if (!validateAllFields(serverPortField, clientPortField)) return;
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
					ConfigUtil.save(); // 保存配置
					dispose(); // 关闭对话框
				}));
				add(new CButton(HexSync.get("Setting.cancel"), event -> dispose()));
				add(new CButton(HexSync.get("Setting.about"), event -> new AboutJDialog(SettingJDialog.this, HexSync.get("About.title"))));
				setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			}}, BorderLayout.SOUTH);
		}});
		Component.setWindow(this);
	}
	@Contract("_ -> new")
	public static @NotNull CompoundBorder getBorder(String key) {
		return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(new FlatRoundBorder(), HexSync.get(key)),
			BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}
	public boolean validateAllFields(JTextField... fields) {
		for (var field : fields) {
			var verifier = field.getInputVerifier();
			if (verifier != null && !verifier.verify(field)) {
				// 验证失败，聚焦到失败的字段
				field.requestFocusInWindow();
				// 手动触发显示提示
				ToolTipManager.sharedInstance()
					.mouseMoved(new MouseEvent(field, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 5, 5, 0, false));
				return false;
			}
		}
		return true;
	}
}
