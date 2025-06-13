package com.forgestove.hexsync.util;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.Data;
import com.forgestove.hexsync.util.object.Port;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.function.Consumer;
public class SettingUtil {
	// 字符串转端口
	public static void setPort(Port port, boolean isServer) {
		var side = isServer ? "服务端" : "客户端";
		if (isServer) Data.serverPort.set(port);
		else Data.clientPort.set(port);
		if (HexSync.HEADLESS) System.out.printf("%s端口已设置为: %d%n", side, port.getValue());
	}
	// 设置文件夹路径
	public static void setDirectory(Path directory, String log, @NotNull Consumer<Path> consumer) {
		consumer.accept(directory);
		System.out.printf("%s文件夹路径已设置为: %s%n", log, directory);
	}
}
