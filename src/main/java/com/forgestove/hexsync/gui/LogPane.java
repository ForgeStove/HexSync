package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
public class LogPane extends JTextPane {
	{
		setOpaque(false);
		setEditable(false);
		// 弹出菜单
		setComponentPopupMenu(new JPopupMenu() {{
			add(new JMenuItem(HexSync.get("GUI.copy"), Icons.copy) {{
				addActionListener(event -> {
					if (getSelectedText() == null) selectAll();
					copy();
				});
			}});
			add(new JMenuItem(HexSync.get("GUI.clear"), Icons.clear) {{addActionListener(event -> GUI.logPane.setText(""));}});
			add(new JMenuItem(HexSync.get("GUI.refresh"), Icons.refresh) {{addActionListener(event -> System.gc());}});
			add(new JMenuItem(HexSync.get("GUI.memory"), Icons.memory) {{
				addActionListener(event -> {
					var memBar = new JProgressBar(0, 100) {{
						setStringPainted(true);
						setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
					}};
					var timer = new Timer(100, e -> {
						var runtime = Runtime.getRuntime();
						var used = runtime.totalMemory() - runtime.freeMemory();
						var total = runtime.totalMemory();
						var percentage = (int) ((used * 100) / total);
						memBar.setValue(percentage);
						memBar.setString("%d%% (%dMB/%dMB)".formatted(percentage, used / 1024 / 1024, total / 1024 / 1024));
					}) {{start();}};
					new JDialog(GUI.getInstance(), HexSync.get("GUI.memoryUsage"), Dialog.ModalityType.MODELESS) {{
						setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						addWindowListener(new WindowAdapter() {
							public void windowClosing(WindowEvent e) {timer.stop();}
						});
						add(memBar);
						setSize(360, 90);
						setLocationRelativeTo(GUI.getInstance());
						setVisible(true);
					}};
				});
			}});
			add(new JMenuItem(HexSync.get("GUI.openLog"), Icons.open) {{
				addActionListener(event -> {
					try {
						Desktop.getDesktop().open(Data.LOG_PATH.getParent().toFile());
					} catch (Exception error) {
						Log.error("无法打开日志: " + error);
					}
				});
			}});
		}});
	}
}
