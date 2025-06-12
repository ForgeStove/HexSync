package com.forgestove.hexsync.gui;
import com.forgestove.hexsync.gui.SVGIconManager.IconType;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
public class SVGIcon {
	public static final FlatSVGIcon //
		icon = getIcon("icon.svg"), // 程序图标
		cog = getIcon("cog.svg"); // 设置图标
	public static @NotNull FlatSVGIcon getIcon(String name) {
		return SVGIconManager.getInstance().getIcon("/svg/" + name);
	}
	/**
	 * 根据类型获取图标
	 *
	 * @param name SVG资源名称
	 * @param type 图标类型
	 * @return 创建的图标
	 */
	public static @NotNull FlatSVGIcon getIcon(String name, IconType type) {
		return SVGIconManager.getInstance().getIcon(name, type);
	}
	/**
	 * 获取自定义颜色的图标
	 *
	 * @param name  SVG资源名称
	 * @param color 自定义颜色
	 * @return 创建的图标
	 */
	public static @NotNull FlatSVGIcon getIcon(String name, Color color) {
		return SVGIconManager.getInstance().getIconWithColor(name, color);
	}
}
