package com.ForgeStove.HexSync.Client;
import java.net.HttpURLConnection;

import static com.ForgeStove.HexSync.Client.FileDownloader.*;
import static com.ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static com.ForgeStove.HexSync.Util.Config.*;
import static com.ForgeStove.HexSync.Util.Files.*;
import static com.ForgeStove.HexSync.Util.Log.*;
import static java.lang.System.exit;
public class Client {
	public static Thread clientThread; // 客户端线程
	public static HttpURLConnection HTTPURLConnection; // 存储客户端连接实例
	public static boolean errorDownload; // 客户端下载文件时是否发生错误，影响客户端是否自动关闭
	public static boolean clientAutoStart; // 客户端自动启动，默认不自动启动
	public static int clientPort = 65535; // 客户端端口，默认值65535
	// 启动客户端
	public static void runClient() {
		if (clientThread != null) return;
		clientThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Client正在启动...");
			initFiles(false);
			var requestMap = fetchFileCRCList();
			if (!requestMap.isEmpty()) {
				deleteFilesNotInMaps(requestMap, initMap(clientOnlyDirectory)); // 删除多余文件
				var clientMap = initMap(clientSyncDirectory); // 初始化客户端文件列表
				requestMap.entrySet().removeIf(entry -> clientMap.containsValue(entry.getValue())); // 循环并删除已存在文件
				downloadMissingFiles(requestMap);// 下载文件
				copyDirectory(clientOnlyDirectory, clientSyncDirectory);// 复制仅客户端模组文件夹中的文件到客户端同步文件夹
			}
			stopClient();
		});
		clientThread.start();
	}
	// 停止客户端
	public static void stopClient() {
		if (clientThread == null || HTTPURLConnection == null) return;
		HTTPURLConnection.disconnect();
		clientThread = null;
		log(INFO, HEX_SYNC_NAME + "Client已关闭");
		if (clientAutoStart && !errorDownload) exit(0);
	}
}
