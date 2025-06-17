package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.util.Converter;
import com.forgestove.hexsync.util.network.*;
import com.forgestove.hexsync.util.network.Rate.Unit;
import com.formdev.flatlaf.ui.FlatRoundBorder;
import org.jetbrains.annotations.*;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
/**
 * 设置对话框类
 * <p>
 * 提供应用程序的配置设置界面，包括服务器配置、客户端配置和UI主题设置。
 * 允许用户修改和保存各种应用程序参数。
 * </p>
 */
public class SettingJDialog extends JDialog {
	/**
	 * 创建一个不会阻止任何顶级窗口的设置对话框实例
	 *
	 * @param owner 对话框的所有者窗口
	 * @param title 对话框的标题
	 */
	public SettingJDialog(Window owner, String title) {
		super(owner, title, ModalityType.MODELESS);
		ConfigUtil.load();
		// 服务端选项
		var serverPort = new VerifiedTextField(Data.serverPort.get().toString(),
			input -> Converter.toOrThrow(input, Port::new).isSuccess());
		var rate = new VerifiedTextField(String.valueOf(Data.serverUploadRate.get().value),
			input -> Converter.toOrThrow(input, string -> new Rate(input, Data.serverUploadRate.get().unit)).isSuccess());
		var rateUnit = new JComboBox<>(Unit.values()) {{setSelectedItem(Data.serverUploadRate.get().unit);}};
		var serverSync = new UndoAbleTextField(Data.serverSyncPath.get().toString());
		var serverAuto = new JCheckBox(HexSync.get("Setting.autoStart"), Data.serverAuto.get());
		// 客户端选项
		var clientPort = new VerifiedTextField(Data.serverPort.get().toString(),
			input -> Converter.toOrThrow(input, Port::new).isSuccess());
		var remoteAddress = new VerifiedTextField(Data.remoteAddress.get(), input -> input != null && !input.trim().isEmpty());
		var clientSync = new UndoAbleTextField(Data.clientSyncPath.get().toString());
		var clientOnly = new UndoAbleTextField(Data.clientOnlyPath.get().toString());
		var clientAuto = new JCheckBox(HexSync.get("Setting.autoStart"), Data.clientAuto.get());
		// 其他选项
		var theme = new JComboBox<>(Arrays.stream(UIManager.getInstalledLookAndFeels())
			.map(LookAndFeelInfo::getName)
			.toArray(String[]::new)) {{setSelectedItem(Data.theme.get());}};
		var scriptPath = Data.script.get();
		var script = new UndoAbleTextField(scriptPath == null ? "" : scriptPath.toString());
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
					add(serverPort);
					add(new JLabel(HexSync.get("Setting.maxUploadRate")));
					add(rate);
					add(new JLabel(HexSync.get("Setting.rateUnit")));
					add(rateUnit);
					add(new JLabel(HexSync.get("Setting.serverSyncPath")));
					add(serverSync);
					add(serverAuto);
					add(new JLabel());
				}});
				// 客户端设置区域
				add(new JPanel(layout) {{
					setBorder(SettingJDialog.getBorder("Setting.client"));
					add(new JLabel(HexSync.get("Setting.port")));
					add(clientPort);
					add(new JLabel(HexSync.get("Setting.remoteAddress")));
					add(remoteAddress);
					add(new JLabel(HexSync.get("Setting.clientSyncPath")));
					add(clientSync);
					add(new JLabel(HexSync.get("Setting.clientOnlyPath")));
					add(clientOnly);
					add(clientAuto);
					add(new JLabel());
				}});
				// 其他设置区域
				add(new JPanel(layout) {{
					setBorder(SettingJDialog.getBorder("Setting.other"));
					add(new JLabel(HexSync.get("Setting.theme")));
					add(theme);
					add(new JLabel(HexSync.get("Setting.script")));
					add(script);
				}});
			}}, BorderLayout.CENTER);
			// 按钮面板
			add(new JPanel(new GridLayout(1, 0, 5, 0)) {{
				add(new CButton(HexSync.get("Setting.save"), event -> {
					if (!Arrays.stream(new VerifiedTextField[]{serverPort, clientPort, rate, remoteAddress})
						.allMatch(VerifiedTextField::isInputValid)) return;
					Data.serverPort.set(new Port(serverPort.getText()));
					Data.serverAuto.set(serverAuto.isSelected());
					Data.serverUploadRate.set(new Rate(rate.getText(), (Unit) Objects.requireNonNull(rateUnit.getSelectedItem())));
					Data.serverSyncPath.set(Path.of(serverSync.getText()));
					Data.clientPort.set(new Port(clientPort.getText()));
					Data.clientAuto.set(clientAuto.isSelected());
					Data.remoteAddress.set(remoteAddress.getText());
					Data.clientSyncPath.set(Path.of(clientSync.getText()));
					Data.clientOnlyPath.set(Path.of(clientOnly.getText()));
					var themeItem = (String) theme.getSelectedItem();
					if (!Data.theme.get().equals(themeItem)) {
						Data.theme.set(themeItem);
						Component.setTheme(themeItem);
					}
					Data.script.set(Path.of(script.getText()));
					ConfigUtil.save(); // 保存配置
					dispose(); // 关闭对话框
				}));
				add(new CButton(HexSync.get("Setting.cancel"), event -> dispose()));
				add(new CButton(HexSync.get("Setting.about"), event -> new AboutJDialog(SettingJDialog.this, HexSync.get("About.title"))));
				setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			}}, BorderLayout.SOUTH);
		}});
		Component.setWindow(this);
		requestFocusInWindow(); // 阻止聚焦到其他组件
	}
	@Contract("_ -> new")
	public static @NotNull CompoundBorder getBorder(String key) {
		return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(new FlatRoundBorder(), HexSync.get(key)),
			BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}
}
