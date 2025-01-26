package ForgeStove.HexSync.GUI;
import javax.swing.*;
import java.awt.*;

import static ForgeStove.HexSync.Client.Client.*;
import static ForgeStove.HexSync.GUI.AboutJDialog.aboutJDialog;
import static ForgeStove.HexSync.GUI.ComponentFactory.*;
import static ForgeStove.HexSync.GUI.GUI.*;
import static ForgeStove.HexSync.Server.Server.*;
import static ForgeStove.HexSync.Util.Config.*;
import static ForgeStove.HexSync.Util.Log.*;
import static ForgeStove.HexSync.Util.Settings.*;
import static javax.swing.BorderFactory.createEmptyBorder;
public class SettingsJDialog {
	// 打开设置对话框
	public SettingsJDialog() {
		if (checkJDialog("设置")) return;
		loadConfig();
		// 设置对话框
		JDialog settingsJDialog = new JDialog(frame, "设置");
		JPanel settingsPanel = new JPanel(new BorderLayout());
		settingsPanel.setBorder(createEmptyBorder(5, 5, 5, 5));
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
		JComboBox<String> serverUploadRateLimitUnitBox = new JComboBox<>(RATE_UNITS);
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
							{"上传速率单位", serverUploadRateLimitUnitBox, 0},
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
					if (!canSetPort(serverPortField.getText().trim(), true)) selectAndFocus(serverPortField);
					if (!canSetPort(clientPortField.getText().trim(), false)) selectAndFocus(clientPortField);
					// 检测最大上传速率
					String uploadRateLimitText = serverUploadRateLimitField.getText().trim();
					if (isInvalidLong(uploadRateLimitText) || Long.parseLong(uploadRateLimitText) < 0) {
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
}
