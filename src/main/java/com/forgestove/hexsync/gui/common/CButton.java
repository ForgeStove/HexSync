package com.forgestove.hexsync.gui.common;
import javax.swing.*;
import java.awt.event.ActionListener;
public class CButton extends JButton {
	public CButton(String text, ActionListener actionListener) {
		super(text);
		addActionListener(actionListener);
	}
	public CButton(String text, ActionListener actionListener, Icon icon) {
		super(text, icon);
		addActionListener(actionListener);
	}
}
