package com.forgestove.hexsync.client;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.*;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
public class Client {
	public static final AtomicReference<Thread> clientThread = new AtomicReference<>();
	public static boolean errorDownload;
	// 启动客户端
	public static void runClient() {
		if (clientThread.get() != null) return;
		var thread = new Thread(() -> {
			Log.info(HexSync.NAME + "Client正在启动...");
			FileUtil.initFiles(false);
			var requestMap = Downloader.fetchFileSHA1List();
			if (!requestMap.isEmpty()) {
				FileUtil.deleteFilesNotInMaps(requestMap, FileUtil.initMap(Data.clientOnlyDirectory.get()));
				var clientSHA1Set = new HashSet<>(FileUtil.initMap(Data.clientSyncDirectory.get()).values());
				requestMap.entrySet().removeIf(entry -> clientSHA1Set.contains(entry.getValue()));
				Downloader.downloadMissingFiles(requestMap);
				FileUtil.copyDirectory(Data.clientOnlyDirectory.get(), Data.clientSyncDirectory.get());
			}
			stopClient();
		});
		if (clientThread.compareAndSet(null, thread)) thread.start();
	}
	// 停止客户端
	public static void stopClient() {
		if (clientThread.getAndSet(null) == null) return;
		Log.info(HexSync.NAME + "Client已关闭");
		if (Data.clientAutoStart.get() && !errorDownload) System.exit(0);
	}
}
