package io.github.forgestove.hexsync.gui;
import io.github.forgestove.hexsync.HexSync;
import io.github.forgestove.hexsync.gui.common.Component;
import io.github.forgestove.hexsync.util.Log;

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
			} catch (Exception e) {
				Log.warn(HexSync.get("linkError") + ": " + e.getMessage());
			}
		});
		add(new JScrollPane(aboutTextPane));
		Component.setWindow(this);
	}
}
