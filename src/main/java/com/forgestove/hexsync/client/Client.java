package com.forgestove.hexsync.client;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.util.*;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
public class Client {
	public static final AtomicReference<Thread> clientThread = new AtomicReference<>();
	public static boolean errorDownload;
	public static boolean clientAutoStart;
	public static int clientPort = 65535;
	// 启动客户端
	public static void runClient() {
		if (clientThread.get() != null) return;
		var thread = new Thread(() -> {
			Log.info(HexSync.NAME + "Client正在启动...");
			FileUtil.initFiles(false);
			var requestMap = Downloader.fetchFileSHA1List();
			if (!requestMap.isEmpty()) {
				FileUtil.deleteFilesNotInMaps(requestMap, FileUtil.initMap(Config.clientOnlyDirectory));
				var clientMap = FileUtil.initMap(Config.clientSyncDirectory);
				var clientSHA1Set = new HashSet<>(clientMap.values());
				requestMap.entrySet().removeIf(entry -> clientSHA1Set.contains(entry.getValue()));
				Downloader.downloadMissingFiles(requestMap);
				FileUtil.copyDirectory(Config.clientOnlyDirectory, Config.clientSyncDirectory);
			}
			stopClient();
		});
		if (clientThread.compareAndSet(null, thread)) thread.start();
	}
	// 停止客户端
	public static void stopClient() {
		var thread = clientThread.getAndSet(null);
		if (thread == null) return;
		Log.info(HexSync.NAME + "Client已关闭");
		if (clientAutoStart && !errorDownload) System.exit(0);
	}
}
