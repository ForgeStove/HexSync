package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
public class AboutJDialog {
	// 关于
	public static void initAboutJDialog(Window parent) {
		if (CComponent.checkJDialog("关于")) return;
		var aboutDialog = new JDialog(parent, "关于");
		var aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("<span style=\"font-weight: bold;font-family: Arial;\">"
			+ HexSync.HEX_SYNC
			+ "<br>By: ForgeStove<br>GitHub: <a href=\""
			+ HexSync.GITHUB_URL
			+ "\">"
			+ HexSync.GITHUB_URL
			+ "</a><br>开源许可: <a href=\""
			+ HexSync.GITHUB_URL
			+ "/blob/main/LICENSE\">GNU General Public License v3.0</a></span>");
		aboutTextPane.addHyperlinkListener(event -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) try {
				Desktop.getDesktop().browse(event.getURL().toURI());
			} catch (Exception error) {
				Log.warn("无法打开超链接: " + error.getMessage());
			}
		});
		aboutDialog.add(new JScrollPane(aboutTextPane));
		aboutDialog.pack();
		CComponent.setWindow(aboutDialog);
	}
}
