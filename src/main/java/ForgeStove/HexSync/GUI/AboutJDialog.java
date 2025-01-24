// Copyright (C) 2025 ForgeStove
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package ForgeStove.HexSync.GUI;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

import static ForgeStove.HexSync.GUI.ComponentFactory.*;
import static ForgeStove.HexSync.HexSync.*;
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
				+ "</a><br>开源许可: <a href=\""
				+ GITHUB_URL
				+ "/blob/main/LICENSE\">GNU General Public License v3.0</a></span>");
		aboutTextPane.addHyperlinkListener(event -> {
			if (HyperlinkEvent.EventType.ACTIVATED.equals(event.getEventType())) try {
				Desktop.getDesktop().browse(event.getURL().toURI());
			} catch (Exception error) {
				log(WARNING, "无法打开超链接: " + error.getMessage());
			}
		});
		aboutDialog.add(new JScrollPane(aboutTextPane));
		aboutDialog.pack();
		setWindow(aboutDialog);
	}
}
