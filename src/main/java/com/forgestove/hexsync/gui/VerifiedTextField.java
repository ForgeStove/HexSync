package com.forgestove.hexsync.gui;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
public class VerifiedTextField extends UndoAbleTextField {
	private final Border originalBorder;
	private final Border errorBorder;
	private final InputValidator validator;
	private boolean isValid = true;
	private String errorMessage = "";
	public VerifiedTextField(String text, InputValidator validator) {
		super(text);
		this.validator = validator;
		this.originalBorder = getBorder();
		this.errorBorder = BorderFactory.createLineBorder(Color.RED, 1);
		// 初始化工具提示
		setToolTipText("");
		// 监听文本变化
		getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent event) {validateInput();}
			public void removeUpdate(DocumentEvent event) {validateInput();}
			public void changedUpdate(DocumentEvent event) {validateInput();}
		});
		// 添加焦点监听器以在失去焦点时验证
		addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent event) {}
			public void focusLost(FocusEvent event) {validateInput();}
		});
	}
	private void validateInput() {
		var text = getText();
		var validationResult = false;
		try {
			validationResult = validator.isValid(text);
		} catch (Exception error) {
			errorMessage = error.getMessage();
		}
		isValid = validationResult;
		if (isValid) {
			setBorder(originalBorder);
			setToolTipText(null);
			errorMessage = "";
			ToolTipManager.sharedInstance().setInitialDelay(ToolTipManager.sharedInstance().getInitialDelay());
			var exitEvent = new MouseEvent(this, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1, -1, 0, false);
			ToolTipManager.sharedInstance().mouseExited(exitEvent);
		} else {
			setBorder(errorBorder);
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
