package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.cli.CLI.*;
import com.forgestove.hexsync.cli.CLI.Run.*;
import com.forgestove.hexsync.cli.CLI.Stop.*;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.*;
import org.jetbrains.annotations.Contract;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;

import java.util.Scanner;
@Command(name = HexSync.NAME, subcommands = {Run.class, Stop.class, Setting.class, GC.class, Memory.class, Help.class, Exit.class})
public class CLI implements Runnable {
	@Contract(pure = true)
	private CLI() {}
	public static void start() {new CLI().run();}
	@SuppressWarnings("InfiniteLoopStatement")
	public void run() {
		System.out.printf("======================%n%s CLI%n输入 help 获取帮助，输入 exit 退出。%n======================%n", HexSync.NAME);
		new Help().run();
		var cmd = new CommandLine(this).setColorScheme(CommandLine.Help.defaultColorScheme(Ansi.ON))
			.setAbbreviatedOptionsAllowed(true)
			.setAbbreviatedSubcommandsAllowed(true)
			.setCaseInsensitiveEnumValuesAllowed(true);
		var scanner = new Scanner(System.in);
		while (true) {
			var line = scanner.nextLine().trim();
			if (line.isEmpty()) continue;
			cmd.execute(line.split("\\s+"));
		}
	}
	@Command(name = "run", description = "运行实例", subcommands = {RunServer.class, RunClient.class})
	static class Run implements Runnable {
		public void run() {new CommandLine(this).usage(System.out);}
		@Command(name = "server", description = "启动服务端")
		static class RunServer implements Runnable {
			public void run() {Server.start();}
		}
		@Command(name = "client", description = "启动客户端")
		static class RunClient implements Runnable {
			public void run() {Client.start();}
		}
	}
	@Command(name = "stop", description = "关闭实例", subcommands = {StopServer.class, StopClient.class})
	static class Stop implements Runnable {
		public void run() {new CommandLine(this).usage(System.out);}
		@Command(name = "server", description = "关闭服务端")
		static class StopServer implements Runnable {
			public void run() {Server.stop();}
		}
		@Command(name = "client", description = "关闭客户端")
		static class StopClient implements Runnable {
			public void run() {Client.stop();}
		}
	}
	@Command(name = "gc", description = "运行垃圾回收")
	static class GC implements Runnable {
		public void run() {System.gc();}
	}
	@Command(name = "memory", description = "显示内存使用情况")
	static class Memory implements Runnable {
		public void run() {Log.info(new MemoryInfo().info);}
	}
	@Command(name = "help", description = "显示帮助信息")
	static class Help implements Runnable {
		public void run() {new CommandLine(new CLI()).usage(System.out);}
	}
	@Command(name = "exit", description = "退出程序")
	static class Exit implements Runnable {
		public void run() {
			Log.info(HexSync.NAME + " 正在退出...");
			System.exit(0);
		}
	}
}
