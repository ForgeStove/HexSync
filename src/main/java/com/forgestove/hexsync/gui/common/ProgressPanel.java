package com.forgestove.hexsync.gui.common;
import javax.swing.*;
import java.awt.*;
import java.util.*;
/**
 * 进度显示面板，包含进度条和状态文本
 */
public class ProgressPanel extends JPanel {
	public final static int maxVisibleFiles = 4;
	private final JProgressBar progressBar;
	private final JPanel statusPanel;
	private final Map<Integer, JLabel> fileStatusLabels;
	/**
	 * 创建一个新的进度显示面板
	 */
	public ProgressPanel() {
		setLayout(new BorderLayout(5, 5));
		// 总进度条
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		add(progressBar, BorderLayout.NORTH);
		// 创建文件状态面板 - 使用网格布局显示多行
		statusPanel = new JPanel(new GridLayout(maxVisibleFiles, 1, 0, 2));
		add(statusPanel, BorderLayout.CENTER);
		// 存储文件状态标签的映射
		fileStatusLabels = new HashMap<>();
		// 默认隐藏
		setVisible(false);
	}
	/**
	 * 更新总体进度条
	 *
	 * @param progress 进度值 (0-100)
	 * @param status   总体状态文本(可选)
	 */
	public void updateProgress(int progress, String status) {
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(progress);
			if (status != null) progressBar.setString(status);
			setVisible(true);
		});
	}
	/**
	 * 更新特定文件的状态
	 *
	 * @param fileIndex 文件索引
	 * @param status    文件状态文本
	 */
	public void updateFileStatus(int fileIndex, String status) {
		SwingUtilities.invokeLater(() -> {
			var statusLabel = fileStatusLabels.get(fileIndex);
			// 如果标签不存在且数量未超过最大值，创建新标签
			if (statusLabel == null && fileStatusLabels.size() < maxVisibleFiles) {
				statusLabel = new JLabel(status);
				fileStatusLabels.put(fileIndex, statusLabel);
				statusPanel.add(statusLabel);
				statusPanel.revalidate();
			}
			// 如果标签存在，更新文本
			else if (statusLabel != null) statusLabel.setText(status);
			setVisible(true);
		});
	}
	/**
	 * 移除特定文件的状态显示
	 *
	 * @param fileIndex 文件索引
	 */
	public void removeFileStatus(int fileIndex) {
		SwingUtilities.invokeLater(() -> {
			var statusLabel = fileStatusLabels.remove(fileIndex);
			if (statusLabel == null) return;
			statusPanel.remove(statusLabel);
			statusPanel.revalidate();
			statusPanel.repaint();
		});
	}
	/**
	 * 重置进度条并隐藏
	 */
	public void reset() {
		SwingUtilities.invokeLater(() -> {
			progressBar.setValue(0);
			progressBar.setString("");
			fileStatusLabels.clear();
			statusPanel.removeAll();
			statusPanel.revalidate();
			statusPanel.repaint();
			setVisible(false);
		});
	}
}
