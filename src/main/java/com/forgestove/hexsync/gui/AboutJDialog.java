package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.util.Log;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent.EventType;
import java.awt.*;
public class AboutJDialog extends JDialog {
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
			""".formatted(HexSync.NAME, HexSync.get("About.license"),
			HexSync.LICENSE_URL,
			HexSync.LICENSE,
			HexSync.GITHUB_URL,
			HexSync.GITHUB_URL));
		aboutTextPane.addHyperlinkListener(event -> {
			if (EventType.ACTIVATED.equals(event.getEventType())) try {
				Desktop.getDesktop().browse(event.getURL().toURI());
			} catch (Exception error) {
				Log.warn(HexSync.get("About.linkError") + ": " + error.getMessage());
			}
		});
		add(new JScrollPane(aboutTextPane));
		ComponentUtil.setWindow(this);
	}
}
