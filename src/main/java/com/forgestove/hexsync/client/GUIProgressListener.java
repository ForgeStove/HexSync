package com.forgestove.hexsync.client;
import com.forgestove.hexsync.gui.GUI;
import it.unimi.dsi.fastutil.ints.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * GUI下载进度监听器实现
 * 用于将下载进度显示在GUI界面上
 */
public class GUIProgressListener implements ProgressListener {
	// 下载状态追踪
	private final Map<Integer, FileStatus> fileStatusMap = Collections.synchronizedMap(new Int2ObjectOpenHashMap<>());
	private final AtomicInteger totalFiles = new AtomicInteger(0);
	private final AtomicInteger completedFiles = new AtomicInteger(0);
	// 待下载和正在下载的文件队列
	private final List<Integer> pendingFiles = Collections.synchronizedList(new IntArrayList());
	private final Set<Integer> activeDownloads = Collections.synchronizedSet(new IntOpenHashSet());
	/**
	 * 格式化文件大小显示
	 *
	 * @param bytesRead  已下载的字节数
	 * @param totalBytes 总字节数
	 * @return 格式化的大小字符串
	 */
	private static String formatSize(long bytesRead, long totalBytes) {
		var readMB = bytesRead / (1024.0 * 1024.0);
		if (totalBytes <= 0) return String.format("%.2f MB", readMB);
		else return String.format("%.2f/%.2f MB", readMB, totalBytes / (1024.0 * 1024.0));
	}
	@Override
	public void onDownloadStart(int totalFiles) {
		// 初始化状态
		this.totalFiles.set(totalFiles);
		this.completedFiles.set(0);
		this.fileStatusMap.clear();
		this.pendingFiles.clear();
		this.activeDownloads.clear();
		// 显示初始进度
		GUI.progressPanel.updateProgress(0, "准备下载 " + totalFiles + " 个文件...");
	}
	@Override
	public void onFileDownloadStart(String fileName, int fileIndex, int totalFiles) {
		// 创建并存储文件状态
		var status = new FileStatus(fileName);
		fileStatusMap.put(fileIndex, status);
		// 将文件添加到活动下载列表
		activeDownloads.add(fileIndex);
		// 如果不是待下载队列中的文件，则添加到显示队列末尾
		if (!pendingFiles.contains(fileIndex)) pendingFiles.add(fileIndex);
		// 更新UI显示
		updateFileStatusDisplay();
		// 更新总进度
		updateOverallProgress();
	}
	@Override
	public void onFileDownloadProgress(String fileName, int fileIndex, int totalFiles, int progress, long bytesRead, long totalBytes) {
		// 更新文件状态
		var status = fileStatusMap.get(fileIndex);
		if (status != null) {
			status.progress = progress;
			status.bytesRead = bytesRead;
			status.totalBytes = totalBytes;
			// 确保文件在活动下载列表中
			activeDownloads.add(fileIndex);
			// 更新UI显示
			var statusText = status.getStatusText();
			GUI.progressPanel.updateFileStatus(fileIndex, statusText);
			// 更新总进度
			updateOverallProgress();
		}
	}
	@Override
	public void onFileDownloadComplete(String fileName, int fileIndex, int totalFiles, boolean success) {
		// 更新文件状态
		var status = fileStatusMap.get(fileIndex);
		if (status != null) {
			status.completed = true;
			// 从活动下载列表移除
			activeDownloads.remove(fileIndex);
			// 从待下载队列移除
			pendingFiles.remove((Integer) fileIndex);
			if (success) {
				status.progress = 100;
				completedFiles.incrementAndGet();
				// 立即从UI中移除该文件状态
				GUI.progressPanel.removeFileStatus(fileIndex);
			} else new Thread(() -> {
				// 保留失败文件的显示一小段时间后再移除
				try {
					// 显示失败状态
					GUI.progressPanel.updateFileStatus(fileIndex, "失败: " + fileName);
					// 短暂延迟后移除
					Thread.sleep(2000);
					GUI.progressPanel.removeFileStatus(fileIndex);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}).start();
			// 更新显示，可能需要显示下一个待下载的文件
			updateFileStatusDisplay();
			// 更新总进度
			updateOverallProgress();
		}
	}
	@Override
	public void onDownloadComplete(int successCount, int totalFiles) {
		// 显示最终完成状态
		var status = String.format("下载完成: %d/%d 个文件", successCount, totalFiles);
		GUI.progressPanel.updateProgress(100, status);
		// 延迟后清除状态面板
		new Thread(() -> {
			try {
				Thread.sleep(3000);
				GUI.progressPanel.reset();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}).start();
	}
	/**
	 * 更新文件状态显示
	 * 确保UI中显示当前活动的下载和即将下载的文件
	 */
	public synchronized void updateFileStatusDisplay() {
		// 当前显示的活跃下载数
		var displayedActiveCount = 0;
		// 最多可见的文件数量
		var maxVisibleFiles = 4;
		// 首先确保所有活跃下载都被显示
		synchronized (activeDownloads) {
			for (var fileIndex : activeDownloads) {
				var status = fileStatusMap.get(fileIndex);
				if (status != null && displayedActiveCount < maxVisibleFiles) {
					var statusText = status.getStatusText();
					GUI.progressPanel.updateFileStatus(fileIndex, statusText);
					displayedActiveCount++;
				}
			}
		}
		// 如果还有剩余显示槽位，显示即将下载的文件
		if (displayedActiveCount < maxVisibleFiles) {
			var slotsRemaining = maxVisibleFiles - displayedActiveCount;
			List<Integer> filesToDisplay;
			synchronized (pendingFiles) {
				filesToDisplay = pendingFiles.stream()
					.filter(fileIndex -> !activeDownloads.contains(fileIndex))
					.limit(slotsRemaining)
					.toList();
			}
			// 显示这些待下载文件
			filesToDisplay.forEach(fileIndex -> {
				var status = fileStatusMap.get(fileIndex);
				if (status != null) {
					var statusText = String.format("等待下载: %s", status.fileName);
					GUI.progressPanel.updateFileStatus(fileIndex, statusText);
				}
			});
		}
	}
	/**
	 * 更新总体下载进度
	 */
	public void updateOverallProgress() {
		var total = totalFiles.get();
		if (total <= 0) return;
		// 计算总进度
		var completed = completedFiles.get();
		var inProgressCount = 0;
		var progressSum = 0;
		for (var status : fileStatusMap.values())
			if (!status.completed) {
				inProgressCount++;
				progressSum += status.progress;
			}
		// 计算总体进度百分比
		int overallProgress;
		if (total == completed) overallProgress = 100;
		else {
			// 已完成文件贡献的进度
			var completedProgress = (completed * 100) / total;
			// 正在下载文件贡献的进度
			var inProgressPercentage = 0;
			if (inProgressCount > 0) {
				inProgressPercentage = progressSum / inProgressCount;
				inProgressPercentage = (inProgressPercentage * (total - completed)) / total;
			}
			overallProgress = completedProgress + inProgressPercentage;
		}
		// 更新总进度条
		var status = String.format("%d / %d ( %d %% )", completed, total, overallProgress);
		GUI.progressPanel.updateProgress(overallProgress, status);
	}
	/**
	 * 文件状态内部类
	 */
	public static class FileStatus {
		String fileName;
		int progress;
		long bytesRead;
		long totalBytes;
		boolean completed;
		@Contract(pure = true)
		public FileStatus(String fileName) {
			this.fileName = fileName;
			this.progress = 0;
			this.bytesRead = 0;
			this.totalBytes = -1;
			this.completed = false;
		}
		public @NotNull String getStatusText() {
			var sizeText = formatSize(bytesRead, totalBytes);
			return String.format("%s (%d%%) %s", fileName, progress, sizeText);
		}
	}
}
