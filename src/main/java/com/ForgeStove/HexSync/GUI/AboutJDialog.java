package com.ForgeStove.HexSync.GUI;
import javax.swing.*;
import java.awt.Window;

import static com.ForgeStove.HexSync.GUI.ComponentFactory.*;
import static com.ForgeStove.HexSync.HexSync.*;
import static com.ForgeStove.HexSync.Util.Log.*;
import static java.awt.Desktop.getDesktop;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.event.HyperlinkEvent.EventType.ACTIVATED;
public class AboutJDialog {
	// 关于
	public static void initAboutJDialog(Window parent) {
		if (checkJDialog("关于")) return;
		var aboutDialog = new JDialog(parent, "关于");
		var aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(createEmptyBorder(5, 5, 5, 5));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("<span style=\"font-weight: bold;font-family: Arial;\">"
				+ HEX_SYNC_NAME
				+ "<br>By: ForgeStove<br>GitHub: <a href=\""
				+ GITHUB_URL
				+ "\">"
				+ GITHUB_URL
				+ "</a><br>开源许可: <a href=\""
				+ GITHUB_URL
				+ "/blob/main/LICENSE\">GNU General Public License v3.0</a></span>");
		aboutTextPane.addHyperlinkListener(event -> {
			if (ACTIVATED.equals(event.getEventType())) try {
				getDesktop().browse(event.getURL().toURI());
			} catch (Exception error) {
				log(WARNING, "无法打开超链接: " + error.getMessage());
			}
		});
		aboutDialog.add(new JScrollPane(aboutTextPane));
		aboutDialog.pack();
		setWindow(aboutDialog);
	}
}
