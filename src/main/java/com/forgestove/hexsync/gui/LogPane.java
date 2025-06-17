package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.gui.common.*;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.event.*;
/**
 * 日志面板组件，扩展自 {@link JTextPane}
 * <p>
 * 用于显示应用程序日志信息，提供了右键菜单功能
 */
public class LogPane extends JTextPane {
	{
		setOpaque(false);
		setEditable(false);
		// 弹出菜单
		setComponentPopupMenu(new JPopupMenu() {{
			add(new CMenuItem(HexSync.get("GUI.copy"), Icons.copy, event -> {
				if (getSelectedText() == null) GUI.logPane.selectAll();
				GUI.logPane.copy();
			}));
			add(new CMenuItem(HexSync.get("GUI.clear"), Icons.clear, event -> GUI.logPane.setText("")));
			add(new CMenuItem(HexSync.get("GUI.refresh"), Icons.refresh, event -> System.gc()));
			add(new CMenuItem(HexSync.get("GUI.memory"), Icons.memory, event -> {
				var memBar = new JProgressBar(0, 100) {{
					setStringPainted(true);
					setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
				}};
				var timer = new Timer(500, e -> {
					var runtime = Runtime.getRuntime();
					var used = runtime.totalMemory() - runtime.freeMemory();
					var total = runtime.totalMemory();
					var percentage = (int) ((used * 100) / total);
					memBar.setValue(percentage);
					memBar.setString("%d%% (%dMB/%dMB)".formatted(percentage, used / 1024 / 1024, total / 1024 / 1024));
				}) {{start();}};
				new JDialog(GUI.getInstance(), HexSync.get("GUI.memoryUsage"), ModalityType.MODELESS) {{
					setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
					addWindowListener(new WindowAdapter() {
						public void windowClosing(WindowEvent windowEvent) {timer.stop();}
					});
					add(memBar);
					setSize(360, 90);
					setLocationRelativeTo(GUI.getInstance());
					setVisible(true);
				}};
			}));
			add(new CMenuItem(HexSync.get("GUI.openLog"), Icons.open, event -> {
				try {
					Desktop.getDesktop().open(Data.LOG_PATH.getParent().toFile());
				} catch (Exception error) {
					Log.error("无法打开日志: " + error);
				}
			}));
		}});
	}
}
