package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
import java.awt.Dialog.ModalityType;
public class AboutJDialog {
	public static void about(Frame parent, String title) {
		var aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("""
			<span style="font-family:Microsoft YaHei;">%s<br>
			By: ForgeStove<br>
			License: <a href="%s">%s</a><br>
			Repository: <a href="%s">%s</a></span>
			""".formatted(HexSync.NAME, HexSync.LICENSE_URL, HexSync.LICENSE, HexSync.GITHUB_URL, HexSync.GITHUB_URL));
		aboutTextPane.addHyperlinkListener(event -> {
			if (EventType.ACTIVATED.equals(event.getEventType())) try {
				Desktop.getDesktop().browse(event.getURL().toURI());
			} catch (Exception error) {
				Log.warn("无法打开超链接: " + error.getMessage());
			}
		});
		var aboutJDialog = new JDialog(parent, title, ModalityType.MODELESS);
		aboutJDialog.add(new JScrollPane(aboutTextPane));
		aboutJDialog.setMinimumSize(new Dimension(400, 150));
		ComponentUtil.setWindow(aboutJDialog);
	}
}
