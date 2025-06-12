package com.forgestove.hexsync.gui;
import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.event.ActionListener;
public class CButton extends JButton {
	public CButton(String text, ActionListener actionListener) {
		super(text);
		setMinimumSize(new Dimension(96, 64));
		addActionListener(actionListener);
	}
}
