package com.forgestove.hexsync.gui;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.event.ActionListener;
public class CButton extends JButton {
	{
		setMinimumSize(new Dimension(96, 64));
	}
	public CButton(String text, ActionListener actionListener) {
		super(text);
		addActionListener(actionListener);
	}
	public CButton(String text, ActionListener actionListener, Icon icon) {
		super(text, icon);
		addActionListener(actionListener);
	}
}
