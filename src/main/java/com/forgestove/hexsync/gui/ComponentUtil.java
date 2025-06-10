package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.util.Log;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
public class ComponentUtil {
	// 聚焦并全选输入框
	public static void selectAndFocus(@NotNull JTextField textField) {
		textField.requestFocus(); // 聚焦输入框
		textField.selectAll(); // 选中输入框
	}
	// 设置字体的通用方法
	public static void setFont(@NotNull Container container, Font font) {
		for (var component : container.getComponents()) {
			if (component instanceof Container) setFont((Container) component, font); // 递归设置子组件的字体
			component.setFont(font); // 设置字体
		}
	}
	// 检测是否有同名窗口并显示
	public static boolean checkJDialog(String title) {
		for (var window : Window.getWindows()) {
			if (!(window instanceof JDialog dialog)) continue;
			if (!dialog.getTitle().equals(title)) continue;
			dialog.setVisible(true);
			dialog.toFront();
			return true;
		}
		return false;
	}
	// 基础复选框框架
	public static @NotNull JCheckBox newJCheckBox(@NotNull JPanel panel, String text, boolean selected) {
		var checkBox = new JCheckBox(text);
		checkBox.setFocusPainted(false);
		checkBox.setSelected(selected);
		panel.add(checkBox);
		return checkBox;
	}
	// 基础按钮框架
	public static void newJButton(@NotNull JPanel panel, String text, ActionListener actionListener) {
		var button = new JButton("<html>" + text);
		button.setFocusPainted(false);
		button.setMinimumSize(new Dimension(96, 32));
		button.addActionListener(actionListener);
		panel.add(button);
	}
	// 设置窗口属性
	public static void setWindow(@NotNull Window window) {
		setFont(window, new Font("Microsoft YaHei", Font.PLAIN, 14));
		window.setIconImage(GUI.icon);
		window.pack();
		window.setLocationRelativeTo(window.getOwner());
		window.setVisible(true);
	}
	// 设置主题
	public static void setTheme(LookAndFeel lookAndFeel) {
		try {
			UIManager.setLookAndFeel(lookAndFeel);
		} catch (UnsupportedLookAndFeelException error) {
			Log.error("设置主题时出错: " + error.getMessage());
		}
	}
}
