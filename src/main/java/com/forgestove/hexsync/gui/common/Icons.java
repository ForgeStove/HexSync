package com.forgestove.hexsync.gui.common;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;

import javax.swing.UIManager;
/**
 * 图标管理器，用于集中管理应用中使用的所有图标
 * 并在主题变更时统一更新图标颜色
 */
public class Icons {
	private static final ObjectOpenHashSet<FlatSVGIcon> iconSet = new ObjectOpenHashSet<>();
	public static final FlatSVGIcon icon = get("icon.svg");
	public static final FlatSVGIcon cog = get("cog.svg");
	public static final FlatSVGIcon exit = get("exit.svg");
	public static final FlatSVGIcon copy = get("copy.svg");
	public static final FlatSVGIcon clear = get("clear.svg");
	public static final FlatSVGIcon refresh = get("refresh.svg");
	public static final FlatSVGIcon memory = get("memory.svg");
	public static final FlatSVGIcon open = get("open.svg");
	/**
	 * 更新所有图标的颜色以适应当前主题
	 */
	public static void updateIconColors() {
		iconSet.forEach(icon -> icon.setColorFilter(new ColorFilter(color -> UIManager.getColor("Component.accentColor"))));
	}
	/**
	 * 注册一个SVG图标并添加到图标管理器中
	 * <p>
	 * 创建一个来自指定资源名称的SVG图标，并将其添加到管理器的图标集合中以便统一管理。
	 * <p>
	 * SVG图标的 {@code width} 和 {@code height}（或 {@code viewBox}）属性将用作图标大小。
	 * <p>
	 * 如果使用Java模块，包含图标的包必须在 {@code module-info.java} 中打开。
	 * <p>
	 * 此操作开销较小，因为图标仅在使用时才会加载。
	 *
	 * @param name SVG资源的名称（以'/'分隔的路径，例如 {@code "com/myapp/myicon.svg"}）
	 * @return 创建并注册的FlatSVGIcon实例
	 * @see FlatSVGIcon#FlatSVGIcon(String)
	 */
	public static @NotNull FlatSVGIcon get(String name) {
		var icon = new FlatSVGIcon(name);
		iconSet.add(icon);
		return icon;
	}
}
