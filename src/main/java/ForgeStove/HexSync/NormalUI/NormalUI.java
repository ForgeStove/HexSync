// Copyright (C) 2025 ForgeStove
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package ForgeStove.HexSync.NormalUI;
import ForgeStove.HexSync.Main;

import javax.swing.*;
import java.awt.*;

import static ForgeStove.HexSync.Client.Client.*;
import static ForgeStove.HexSync.Main.HEX_SYNC_NAME;
import static ForgeStove.HexSync.NormalUI.ComponentFactory.*;
import static ForgeStove.HexSync.NormalUI.SettingsJDialog.settingsJDialog;
import static ForgeStove.HexSync.Server.Server.*;
import static ForgeStove.HexSync.Util.Log.*;
import static java.lang.System.exit;
public class NormalUI {
	public static Image icon; // 程序图标
	public static JFrame frame; // 主窗口
	public static JTextPane textPane; // 日志面板
	public static int screenLength; // 屏幕长边
	// 有头模式UI
	public static void normalUI() {
		SwingUtilities.invokeLater(() -> {
			try {
				icon = Toolkit.getDefaultToolkit().getImage(Main.class.getClassLoader().getResource("icon.png"));
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
				screenLength = Math.max(size.width, size.height);
				// 添加按钮，状态面板和托盘图标
				JPanel panel = new JPanel();
				panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				panel.setLayout(new BorderLayout(5, 5));
				textPane = new JTextPane();
				textPane.setEditable(false);
				textPane.setOpaque(false);
				panel.add(new JScrollPane(textPane), BorderLayout.CENTER);
				frame = new JFrame(HEX_SYNC_NAME); // 主窗口
				frame.setAlwaysOnTop(true);
				JPanel buttonPanel = new JPanel(new GridLayout(2, 3));
				newJButton(buttonPanel, "设置", event -> settingsJDialog());
				newJButton(buttonPanel, "启动服务端", event -> startServer());
				newJButton(buttonPanel, "启动客户端", event -> startClient());
				newJButton(buttonPanel, "停止服务端", event -> stopServer());
				newJButton(buttonPanel, "停止客户端", event -> stopClient());
				newJButton(buttonPanel, "退出", event -> exit(0));
				panel.add(buttonPanel, BorderLayout.SOUTH);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.add(panel);
				frame.setSize(new Dimension(screenLength / 3, screenLength / 4));
				setWindow(frame);
			} catch (Exception error) {
				log(SEVERE, "初始化UI时出错:" + error.getMessage());
			}
		});
	}
}
