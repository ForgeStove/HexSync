package com.forgestove.hexsync.client;
/**
 * 下载进度监听器接口
 * 用于监听和报告下载进度
 */
public interface ProgressListener {
	/**
	 * 当下载开始时调用
	 *
	 * @param totalFiles 需要下载的文件总数
	 */
	void onDownloadStart(int totalFiles);
	/**
	 * 当单个文件开始下载时调用
	 *
	 * @param fileName   当前正在下载的文件名
	 * @param fileIndex  当前文件索引（从1开始）
	 */
	void onFileDownloadStart(String fileName, int fileIndex);
	/**
	 * 当单个文件下载进度更新时调用
	 *
	 * @param fileName   当前正在下载的文件名
	 * @param fileIndex  当前文件索引（从1开始）
	 * @param progress   当前文件的下载进度（0-100）
	 * @param bytesRead  已下载的字节数
	 * @param totalBytes 总字节数
	 */
	void onFileDownloadProgress(String fileName, int fileIndex, int progress, long bytesRead, long totalBytes);
	/**
	 * 当单个文件下载完成时调用
	 *
	 * @param fileName   已下载的文件名
	 * @param fileIndex  当前文件索引（从1开始）
	 * @param success    下载是否成功
	 */
	void onFileDownloadComplete(String fileName, int fileIndex, boolean success);
	/**
	 * 当所有文件下载完成时调用
	 *
	 * @param successCount 成功下载的文件数
	 * @param totalFiles   总文件数
	 */
	void onDownloadComplete(int successCount, int totalFiles);
}
