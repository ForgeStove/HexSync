package ForgeStove.HexSync.NormalUI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

import static ForgeStove.HexSync.NormalUI.NormalUI.*;
public class ComponentFactory {
	// 聚焦并全选输入框
	public static void selectAndFocus(JTextField textField) {
		textField.requestFocus(); // 聚焦输入框
		textField.selectAll(); // 选中输入框
	}
	// 设置字体的通用方法
	public static void setFont(Container container, Font font) {
		for (Component component : container.getComponents()) {
			if (component instanceof Container) setFont((Container) component, font); // 递归设置子组件的字体
			component.setFont(font); // 设置字体
		}
	}
	// 检测是否有同名窗口并显示
	public static boolean checkJDialog(String title) {
		for (Window window : Window.getWindows()) {
			if (!(window instanceof JDialog)) continue;
			JDialog dialog = (JDialog) window;
			if (!dialog.getTitle().equals(title)) continue;
			dialog.setVisible(true);
			dialog.toFront();
			return true;
		}
		return false;
	}
	// 基础复选框框架
	public static JCheckBox newJCheckBox(JPanel panel, String text, boolean selected) {
		JCheckBox checkBox = new JCheckBox(text);
		checkBox.setFocusPainted(false);
		checkBox.setSelected(selected);
		panel.add(checkBox);
		return checkBox;
	}
	// 基础按钮框架
	public static void newJButton(JPanel panel, String text, ActionListener actionListener) {
		JButton button = new JButton("<html>" + text);
		button.setFocusPainted(false);
		button.setPreferredSize(new Dimension(0, screenLength / 55));
		button.addActionListener(actionListener);
		panel.add(button);
	}
	// 设置窗口属性
	public static void setWindow(Window window) {
		setFont(window, new Font("Arial", Font.PLAIN, 14));
		window.setIconImage(icon);
		window.setLocationRelativeTo(null);
		window.setVisible(true);
	}
}
