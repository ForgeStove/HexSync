package com.forgestove.hexsync.gui;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.*;
/**
 * SVG图标管理器，用于集中管理应用中使用的所有SVG图标
 * 并在主题变更时统一更新图标颜色
 */
public class SVGIconManager {
	private static final SVGIconManager INSTANCE = new SVGIconManager(); // 单例模式
	private final Map<IconType, List<FlatSVGIcon>> iconsByType = new EnumMap<>(IconType.class); // 按类型存储图标
	private final Map<FlatSVGIcon, Color> customColors = new HashMap<>(); // 自定义颜色映射
	private SVGIconManager() {
		for (var type : IconType.values()) iconsByType.put(type, new ArrayList<>()); // 初始化所有类型的列表
	}
	public static SVGIconManager getInstance() {
		return INSTANCE;
	}
	/**
	 * 创建并注册SVG图标
	 *
	 * @param name SVG资源名称
	 * @return 创建的图标
	 */
	public FlatSVGIcon getIcon(String name) {
		return getIcon(name, IconType.DEFAULT);
	}
	/**
	 * 创建并注册指定类型的SVG图标
	 *
	 * @param name SVG资源名称
	 * @param type 图标类型
	 * @return 创建的图标
	 */
	public FlatSVGIcon getIcon(String name, IconType type) {
		var icon = new FlatSVGIcon(name);
		iconsByType.get(type).add(icon);
		return icon;
	}
	/**
	 * 创建并注册自定义颜色的SVG图标
	 *
	 * @param name  SVG资源名称
	 * @param color 自定义颜色
	 * @return 创建的图标
	 */
	public FlatSVGIcon getIconWithColor(String name, Color color) {
		var icon = new FlatSVGIcon(name);
		iconsByType.get(IconType.CUSTOM).add(icon);
		customColors.put(icon, color);
		icon.setColorFilter(new ColorFilter(c -> color));
		return icon;
	}
	/**
	 * 设置图标颜色
	 *
	 * @param icon  图标
	 * @param color 颜色
	 */
	public void setIconColor(FlatSVGIcon icon, Color color) {
		customColors.put(icon, color);
		icon.setColorFilter(new ColorFilter(c -> color));
	}
	/**
	 * 更新所有图标的颜色以适应当前主题
	 */
	public void updateIconColors() {
		// 更新默认图标
		updateIconsOfType(IconType.DEFAULT, UIManager.getColor("Component.accentColor"));
		// 更新操作图标
		updateIconsOfType(IconType.ACTION, UIManager.getColor("Button.foreground"));
		// 更新状态图标
		updateIconsOfType(IconType.STATUS, UIManager.getColor("Label.foreground"));
		// 更新导航图标
		updateIconsOfType(IconType.NAVIGATION, UIManager.getColor("Menu.foreground"));
		// 更新自定义颜色图标
		for (var icon : iconsByType.get(IconType.CUSTOM))
			icon.setColorFilter(new ColorFilter(c -> customColors.getOrDefault(icon, UIManager.getColor("Component.accentColor"))));
	}
	/**
	 * 更新指定类型的所有图标颜色
	 *
	 * @param type  图标类型
	 * @param color 颜色
	 */
	private void updateIconsOfType(IconType type, Color color) {
		for (var icon : iconsByType.get(type)) icon.setColorFilter(new ColorFilter(c -> color));
	}
	/**
	 * 获取所有已注册的图标
	 *
	 * @return 所有图标的列表
	 */
	public List<FlatSVGIcon> getAllIcons() {
		List<FlatSVGIcon> allIcons = new ArrayList<>();
		for (var icons : iconsByType.values()) allIcons.addAll(icons);
		return Collections.unmodifiableList(allIcons);
	}
	// 图标分类，用于对不同类型的图标应用不同的颜色策略
	public enum IconType {
		DEFAULT,        // 默认使用accent颜色
		ACTION,         // 操作按钮图标
		STATUS,         // 状态指示图标
		NAVIGATION,     // 导航图标
		CUSTOM          // 自定义颜色图标
	}
}
