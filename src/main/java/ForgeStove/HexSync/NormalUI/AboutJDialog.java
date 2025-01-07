package ForgeStove.HexSync.NormalUI;
import ForgeStove.HexSync.Main;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.*;
import java.util.Objects;

import static ForgeStove.HexSync.Main.*;
import static ForgeStove.HexSync.NormalUI.ComponentFactory.*;
import static ForgeStove.HexSync.Util.Log.*;
public class AboutJDialog {
	// 关于
	public static void aboutJDialog(Window parent) {
		if (checkJDialog("关于")) return;
		JDialog aboutDialog = new JDialog(parent, "关于");
		JTextPane aboutTextPane = new JTextPane();
		aboutTextPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		aboutTextPane.setContentType("text/html");
		aboutTextPane.setEditable(false);
		aboutTextPane.setText("<span style=\"font-weight: bold;font-family: Arial;\">"
				+ HEX_SYNC_NAME
				+ "<br>By: ForgeStove<br>GitHub: <a href=\""
				+ GITHUB_URL
				+ "\">"
				+ GITHUB_URL
				+ "</a><br>开源许可: <a href=\"file:LICENSE\">GNU General Public License v3.0</a></span>");
		aboutTextPane.addHyperlinkListener(event -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) try {
				String url = event.getURL().toString();
				if (url.equals(GITHUB_URL)) {
					Desktop.getDesktop().browse(event.getURL().toURI());
				} else if (url.equals("file:LICENSE")) {
					if (checkJDialog("许可证")) return;
					try (
							BufferedReader reader =
									new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream(
									"LICENSE"))))
					) {
						StringBuilder licenseContent = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null)
							licenseContent.append(line).append(System.lineSeparator());
						JTextArea licenseTextArea = new JTextArea(licenseContent.toString());
						licenseTextArea.setEditable(false);
						licenseTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
						JDialog licenseJDialog = new JDialog(aboutDialog, "许可证");
						licenseJDialog.add(new JScrollPane(licenseTextArea));
						licenseJDialog.pack();
						setWindow(licenseJDialog);
					}
				}
			} catch (Exception error) {
				log(WARNING, "无法打开超链接: " + error.getMessage());
			}
		});
		aboutDialog.add(new JScrollPane(aboutTextPane));
		aboutDialog.pack();
		setWindow(aboutDialog);
	}
}
