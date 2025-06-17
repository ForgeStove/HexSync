package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.gui.common.Component;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
public class AboutJDialog extends JDialog {
	public static final String LICENSE = "MIT";
	public static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync";
	public static final String LICENSE_URL = GITHUB_URL + "/blob/main/LICENSE";
	public AboutJDialog(Window parent, String title) {
		super(parent, title, ModalityType.MODELESS);
		var aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("""
			<span style="font-family:Microsoft YaHei;"><b>%s<br>
			%s: <a href="%s">%s</a><br>
			Github: <a href="%s">%s</a><br></span>
			""".formatted(HexSync.NAME, HexSync.get("license"), LICENSE_URL, LICENSE, GITHUB_URL, GITHUB_URL));
		aboutTextPane.addHyperlinkListener(event -> {
			if (EventType.ACTIVATED.equals(event.getEventType())) try {
				Desktop.getDesktop().browse(event.getURL().toURI());
			} catch (Exception error) {
				Log.warn(HexSync.get("linkError") + ": " + error.getMessage());
			}
		});
		add(new JScrollPane(aboutTextPane));
		Component.setWindow(this);
	}
}
