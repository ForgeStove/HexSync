package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.util.Log;
import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.themes.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.Window;
import java.util.Arrays;
public class Component {
	static {
		FlatLightLaf.installLafInfo();
		FlatDarkLaf.installLafInfo();
		FlatMacLightLaf.installLafInfo();
		FlatMacDarkLaf.installLafInfo();
		FlatIntelliJLaf.installLafInfo();
		FlatDarculaLaf.installLafInfo();
		Arrays.stream(FlatAllIJThemes.INFOS).toList().forEach(UIManager::installLookAndFeel);
	}
	// 聚焦并全选输入框
	public static void selectAndFocus(@NotNull JTextField textField) {
		textField.requestFocus(); // 聚焦输入框
		textField.selectAll(); // 选中输入框
	}
	// 设置窗口属性
	public static void setWindow(@NotNull Window window) {
		window.setIconImage(IconManager.icon.getImage());
		window.pack();
		window.setLocationRelativeTo(window.getOwner());
		window.setVisible(true);
	}
	/**
	 * 设置应用程序的UI主题
	 *
	 * @param name 主题名称（区分大小写）
	 */
	public static void setTheme(String name) {
		try {
			UIManager.setLookAndFeel(getClassName(name));
		} catch (Exception error) {
			Log.error("设置主题 %s 时出错: %s".formatted(name, error.getMessage()));
		}
		IconManager.updateIconColors();
		for (var window : Window.getWindows()) {
			window.setIconImage(IconManager.icon.getImage());
			SwingUtilities.updateComponentTreeUI(window);
		}
	}
	/**
	 * 从主题名称获取对应的主题类名
	 *
	 * @param name 要查找的主题名称
	 * @return 主题对应的类名；如果未找到指定主题，则返回系统默认主题类名
	 */
	public static @NotNull String getClassName(String name) {
		var className = Arrays.stream(UIManager.getInstalledLookAndFeels())
			.filter(info -> info.getName().equalsIgnoreCase(name))
			.map(LookAndFeelInfo::getClassName)
			.findFirst()
			.orElse(null);
		if (className != null) return className;
		Log.error("未找到名为 %s 的主题".formatted(name));
		return UIManager.getSystemLookAndFeelClassName();
	}
}
