package com.forgestove.hexsync.gui.common;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.ToolTipManager;
import javax.swing.event.*;
import java.awt.Point;
import java.awt.event.MouseEvent;
public class VerifiedTextField extends UndoableTextField {
	private final InputValidator validator;
	private boolean isValid = true;
	private String errorMessage;
	public VerifiedTextField(String text, InputValidator validator) {
		super(text);
		this.validator = validator;
		// 初始化工具提示
		setToolTipText(null);
		// 监听文本变化
		getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent event) {validateInput();}
			public void removeUpdate(DocumentEvent event) {validateInput();}
			public void changedUpdate(DocumentEvent event) {validateInput();}
		});
	}
	private void validateInput() {
		var text = getText();
		var validationResult = false;
		try {
			validationResult = validator.isValid(text);
		} catch (Exception e) {
			errorMessage = e.getMessage();
		}
		isValid = validationResult;
		if (isValid) {
			putClientProperty(FlatClientProperties.OUTLINE, null);
			setToolTipText(null);
			errorMessage = null;
			ToolTipManager.sharedInstance().setInitialDelay(ToolTipManager.sharedInstance().getInitialDelay());
			var exitEvent = new MouseEvent(this, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1, -1, 0, false);
			ToolTipManager.sharedInstance().mouseExited(exitEvent);
		} else {
			putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
			setToolTipText(errorMessage);
			// 如果当前有焦点，立即显示工具提示
			if (hasFocus()) {
				var point = new Point(10, getHeight());
				ToolTipManager.sharedInstance().setInitialDelay(0);
				var mouseEvent = new MouseEvent(this, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
				ToolTipManager.sharedInstance().mouseMoved(mouseEvent);
			}
		}
		repaint();
	}
	public boolean isInputValid() {
		return isValid;
	}
	@Override
	public Point getToolTipLocation(MouseEvent event) {
		if (!isValid) return new Point(0, getHeight());
		return super.getToolTipLocation(event);
	}
	public interface InputValidator {
		boolean isValid(String text);
	}
}
