package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
public class AboutJDialog {
	public static void initAboutJDialog(Window parent) {
		if (CComponent.checkJDialog("关于")) return;
		var aboutDialog = new JDialog(parent, "关于");
		var aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("""
			<span style="font-family:Microsoft YaHei;">%s<br>
			By: ForgeStove<br>
			GitHub: <a href="%s">%s</a><br>
			开源许可: <a href="%s/blob/main/LICENSE">MIT</a></span>
			""".formatted(HexSync.NAME, HexSync.GITHUB_URL, HexSync.GITHUB_URL, HexSync.GITHUB_URL));
		aboutTextPane.addHyperlinkListener(event -> {
			if (EventType.ACTIVATED.equals(event.getEventType())) try {
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
