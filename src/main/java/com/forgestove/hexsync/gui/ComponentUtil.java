package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.util.Log;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
public class ComponentUtil {
	static {
		Arrays.stream(FlatAllIJThemes.INFOS).toList().forEach(UIManager::installLookAndFeel);
		FlatMTGitHubDarkIJTheme.updateUI();
	}
	// 聚焦并全选输入框
	public static void selectAndFocus(@NotNull JTextField textField) {
		textField.requestFocus(); // 聚焦输入框
		textField.selectAll(); // 选中输入框
	}
	// 设置字体的通用方法
	public static void setFont(@NotNull Container container, Font font) {
		for (var component : container.getComponents()) {
			if (component instanceof Container) setFont((Container) component, font); // 递归设置子组件的字体
			if (font.equals(component.getFont())) continue;
			component.setFont(font); // 设置字体
		}
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
		window.setIconImage(GUI.icon.getImage());
		window.pack();
		window.setLocationRelativeTo(window.getOwner());
		window.setVisible(true);
	}
	public static void setTheme(String name) {
		GUI.icon.setColorFilter(new ColorFilter(color -> UIManager.getColor("Component.accentColor")));
		try {
			UIManager.setLookAndFeel(name);
		} catch (Exception error) {
			Log.error("设置主题 '" + name + "' 时出错: " + error.getMessage());
		}
	}
	public static String getName(String name) {
		for (var info : UIManager.getInstalledLookAndFeels())
			if (info.getName().equalsIgnoreCase(name)) return info.getName();
		Log.error("未找到名为 '" + name + "' 的主题");
		return UIManager.getSystemLookAndFeelClassName(); // 如果找不到，返回系统默认外观
	}
	public static String getClassName(String name) {
		for (var info : UIManager.getInstalledLookAndFeels())
			if (info.getName().equalsIgnoreCase(name)) return info.getClassName();
		Log.error("未找到名为 '" + name + "' 的主题");
		return UIManager.getSystemLookAndFeelClassName(); // 如果找不到，返回系统默认外观
	}
}
