package com.forgestove.hexsync.gui;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import org.jetbrains.annotations.NotNull;
public class SVGIcon {
	public static final FlatSVGIcon //
		icon = getIcon("icon.svg"), // 程序图标
		cog = getIcon("cog.svg"), // 设置图标
		exit = getIcon("exit.svg"); // 退出图标
	public static @NotNull FlatSVGIcon getIcon(String name) {
		return SVGIconManager.getInstance().getIcon(name);
	}
}
