package io.github.forgestove.hexsync.gui.common;
import javax.swing.*;
import java.awt.event.ActionListener;
public class CMenuItem extends JMenuItem {
	public CMenuItem(String text, Icon icon, ActionListener actionListener) {
		super(text, icon);
		addActionListener(actionListener);
	}
}
