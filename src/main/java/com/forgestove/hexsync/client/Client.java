package com.forgestove.hexsync.client;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.util.*;
public class Client {
	public static Thread clientThread; // 客户端线程
	public static boolean errorDownload;
	public static boolean clientAutoStart;
	public static int clientPort = 65535;
	// 启动客户端
	public static void runClient() {
		if (clientThread != null) return;
		clientThread = new Thread(() -> {
			Log.info(HexSync.NAME + "Client正在启动...");
			FileUtil.initFiles(false);
			var requestMap = Downloader.fetchFileSHA1List();
			if (!requestMap.isEmpty()) {
				FileUtil.deleteFilesNotInMaps(requestMap, FileUtil.initMap(Config.clientOnlyDirectory));
				var clientMap = FileUtil.initMap(Config.clientSyncDirectory);
				requestMap.entrySet().removeIf(entry -> clientMap.containsValue(entry.getValue()));
				Downloader.downloadMissingFiles(requestMap);
				FileUtil.copyDirectory(Config.clientOnlyDirectory, Config.clientSyncDirectory);
			}
			stopClient();
		});
		clientThread.start();
	}
	// 停止客户端
	public static void stopClient() {
		if (clientThread == null) return;
		clientThread = null;
		Log.info(HexSync.NAME + "Client已关闭");
		if (clientAutoStart && !errorDownload) System.exit(0);
	}
}
