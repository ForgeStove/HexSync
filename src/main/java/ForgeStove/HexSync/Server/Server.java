package ForgeStove.HexSync.Server;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static ForgeStove.HexSync.Server.RequestHandler.requestHandler;
import static ForgeStove.HexSync.Util.Config.*;
import static ForgeStove.HexSync.Util.Files.initFiles;
import static ForgeStove.HexSync.Util.Log.*;
import static ForgeStove.HexSync.Util.Unit.convertToBytes;
import static com.sun.net.httpserver.HttpServer.create;
import static java.util.concurrent.Executors.newFixedThreadPool;
public class Server {
	public static final AtomicLong AVAILABLE_TOKENS = new AtomicLong(0); // 当前可用令牌数量
	public static Thread serverThread; // 服务器线程
	public static HttpServer HTTPServer; // 存储服务器实例
	public static Map<String, Long> serverMap; // 存储服务端文件名和对应的校验码数据
	public static long serverUploadRateLimit = 1; // 上传速率限制值，默认限速1MB
	public static long maxUploadRateInBytes; // 上传速率限制值对应的字节数
	public static boolean serverAutoStart; // 服务端自动启动，默认不自动启动
	public static int serverPort = 65535; // 服务端端口，默认值65535
	// 启动服务端
	public static void runServer() {
		if (serverThread != null) return;
		serverThread = new Thread(() -> {
			log(INFO, HEX_SYNC_NAME + "Server正在启动...");
			initFiles(true);
			if (serverMap.isEmpty()) {
				log(WARNING, serverSyncDirectory + "没有文件,无法启动服务器");
				stopServer();
				return;
			}
			try {
				ExecutorService executorService = newFixedThreadPool(8);
				maxUploadRateInBytes = convertToBytes(serverUploadRateLimit, serverUploadRateLimitUnit);
				HTTPServer = create(new InetSocketAddress(serverPort), 0);
				HTTPServer.setExecutor(executorService);
				HTTPServer.createContext("/", exchange -> executorService.submit(() -> requestHandler(exchange)));
				HTTPServer.start();
			} catch (IOException error) {
				log(SEVERE, HEX_SYNC_NAME + "Server无法启动: " + error.getMessage());
				return;
			}
			log(INFO, HEX_SYNC_NAME + "Server正在运行...端口号为: " + serverPort);
		});
		serverThread.start();
	}
	// 停止服务端
	public static void stopServer() {
		if (serverMap != null) serverMap.clear();
		if (serverThread != null) serverThread = null;
		if (HTTPServer != null) HTTPServer.stop(0);
		log(INFO, HEX_SYNC_NAME + "Server已关闭");
	}
}
