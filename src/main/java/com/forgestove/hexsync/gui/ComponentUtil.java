package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.util.Log;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.util.Arrays;
public class ComponentUtil {
	static {
		Arrays.stream(FlatAllIJThemes.INFOS).toList().forEach(UIManager::installLookAndFeel);
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
	// 从主题名称获取主题类名
	public static @NotNull String getClassName(String name) {
		var className = Arrays.stream(UIManager.getInstalledLookAndFeels())
			.filter(info -> info.getName().equalsIgnoreCase(name))
			.map(LookAndFeelInfo::getClassName)
			.findFirst()
			.orElse(null);
		if (className != null) return className;
		Log.error("未找到名为 %s 的主题", name);
		return UIManager.getSystemLookAndFeelClassName(); // 如果找不到，返回系统默认外观
	}
}
