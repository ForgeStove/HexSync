package io.github.forgestove.hexsync.client;
import io.github.forgestove.hexsync.gui.GUI;
import io.github.forgestove.hexsync.gui.common.ProgressPanel;
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
	public final Map<Integer, FileStatus> fileStatusMap = Collections.synchronizedMap(new Int2ObjectOpenHashMap<>());
	public final AtomicInteger totalFiles = new AtomicInteger(0);
	public final AtomicInteger completedFiles = new AtomicInteger(0);
	// 待下载和正在下载的文件队列
	public final List<Integer> pendingFiles = Collections.synchronizedList(new IntArrayList());
	public final Set<Integer> activeDownloads = Collections.synchronizedSet(new IntOpenHashSet());
	@Override
	public void onDownloadStart(int totalFiles) {
		// 初始化状态
		this.totalFiles.set(totalFiles);
		completedFiles.set(0);
		fileStatusMap.clear();
		pendingFiles.clear();
		activeDownloads.clear();
		// 显示初始进度
		GUI.progressPanel.updateProgress(0, "准备下载 " + totalFiles + " 个文件...");
	}
	@Override
	public void onFileDownloadStart(String fileName, int fileIndex) {
		// 创建并存储文件状态
		fileStatusMap.put(fileIndex, new FileStatus(fileName));
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
	public void onFileDownloadProgress(int fileIndex, int progress, long bytesRead, long totalBytes) {
		// 更新文件状态
		var status = fileStatusMap.get(fileIndex);
		if (status == null) return;
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
	@Override
	public void onFileDownloadComplete(String fileName, int fileIndex, boolean success) {
		// 更新文件状态
		var status = fileStatusMap.get(fileIndex);
		if (status == null) return;
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
				Thread.sleep(3000);
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
	@Override
	public void onDownloadComplete(int successCount, int totalFiles) {
		// 显示最终完成状态
		GUI.progressPanel.updateProgress(100, String.format("下载完成: %d/%d 个文件", successCount, totalFiles));
		// 延迟后清除状态面板
		new Thread(() -> {
			try {
				Thread.sleep(5000);
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
		var maxVisibleFiles = ProgressPanel.maxVisibleFiles;
		// 首先确保所有活跃下载都被显示
		synchronized (activeDownloads) {
			for (var fileIndex : activeDownloads) {
				var status = fileStatusMap.get(fileIndex);
				if (status == null || displayedActiveCount >= maxVisibleFiles) continue;
				GUI.progressPanel.updateFileStatus(fileIndex, status.getStatusText());
				displayedActiveCount++;
			}
		}
		// 如果还有剩余显示槽位，显示即将下载的文件
		if (displayedActiveCount >= maxVisibleFiles) return;
		var slotsRemaining = maxVisibleFiles - displayedActiveCount;
		synchronized (pendingFiles) {
			pendingFiles.stream().filter(fileIndex -> !activeDownloads.contains(fileIndex)).limit(slotsRemaining).forEach(fileIndex -> {
				var status = fileStatusMap.get(fileIndex);
				if (status == null) return;
				var statusText = String.format("等待下载: %s", status.fileName);
				GUI.progressPanel.updateFileStatus(fileIndex, statusText);
			});
		}
	}
	/**
	 * 更新总体下载进度
	 * 采用平等权重方式：每个文件占相同比重，但考虑每个文件的下载进度
	 */
	public synchronized void updateOverallProgress() {
		var total = totalFiles.get();
		if (total <= 0) return;
		// 计算总进度：每个文件权重相同，但考虑文件下载进度
		double totalProgress;
		// 同步访问文件状态数据
		synchronized (fileStatusMap) {
			totalProgress = fileStatusMap.values().stream().mapToDouble(status -> status.progress).sum();
		}
		// 计算平均进度，使用总文件数作为除数
		var newProgress = (int) Math.round(totalProgress / total);
		var status = String.format("%d / %d ( %d %% )", completedFiles.get(), total, newProgress);
		GUI.progressPanel.updateProgress(newProgress, status);
	}
	/**
	 * 文件状态内部类
	 */
	public static class FileStatus {
		public String fileName;
		public int progress;
		public long bytesRead;
		public long totalBytes;
		@Contract(pure = true)
		public FileStatus(String fileName) {
			this.fileName = fileName;
			progress = 0;
			bytesRead = 0;
			totalBytes = -1;
		}
		/**
		 * 格式化文件大小显示
		 *
		 * @param bytesRead  已下载的字节数
		 * @param totalBytes 总字节数
		 * @return 格式化的大小字符串
		 */
		public static @NotNull String formatSize(long bytesRead, long totalBytes) {
			var readMB = bytesRead / (1024.0 * 1024.0);
			if (totalBytes <= 0) return String.format("%.2f MB", readMB);
			else return String.format("%.2f/%.2f MB", readMB, totalBytes / (1024.0 * 1024.0));
		}
		public @NotNull String getStatusText() {
			return String.format("%s (%d%%) %s", fileName, progress, formatSize(bytesRead, totalBytes));
		}
	}
}
