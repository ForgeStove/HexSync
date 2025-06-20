package com.forgestove.hexsync.util.network;
import org.jetbrains.annotations.NotNull;

import java.io.*;
/**
 * 进度监控输入流
 * 监控数据读取进度并通过回调报告
 */
public class ProgressInputStream extends FilterInputStream {
	public final long totalBytes;
	public final ProgressCallback callback;
	public long bytesRead;
	/**
	 * 创建一个进度监控输入流
	 *
	 * @param in         原始输入流
	 * @param totalBytes 总字节数
	 * @param callback   进度回调
	 */
	public ProgressInputStream(InputStream in, long totalBytes, ProgressCallback callback) {
		super(in);
		this.totalBytes = totalBytes;
		this.bytesRead = 0;
		this.callback = callback;
	}
	@Override
	public int read() throws IOException {
		var b = super.read();
		if (b != -1) updateProgress(1);
		return b;
	}
	@Override
	public int read(byte @NotNull [] b, int off, int len) throws IOException {
		var bytesRead = super.read(b, off, len);
		if (bytesRead != -1) updateProgress(bytesRead);
		return bytesRead;
	}
	@Override
	public long skip(long n) throws IOException {
		var skipped = super.skip(n);
		updateProgress(skipped);
		return skipped;
	}
	/**
	 * 更新进度并触发回调
	 *
	 * @param count 新读取的字节数
	 */
	private void updateProgress(long count) {
		if (count <= 0 || callback == null) return;
		this.bytesRead += count;
		callback.onProgressUpdate(this.bytesRead, this.totalBytes);
	}
	/**
	 * 进度回调接口
	 */
	public interface ProgressCallback {
		/**
		 * 当进度更新时调用
		 *
		 * @param bytesRead  已读取的字节数
		 * @param totalBytes 总字节数
		 */
		void onProgressUpdate(long bytesRead, long totalBytes);
	}
}
