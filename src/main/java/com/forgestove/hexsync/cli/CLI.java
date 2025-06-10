package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.cli.CLI.*;
import com.forgestove.hexsync.cli.CLI.Run.*;
import com.forgestove.hexsync.cli.CLI.Stop.*;
import com.forgestove.hexsync.cli.Setting.*;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.Log;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Scanner;
@Command(name = HexSync.NAME, subcommands = {Run.class, Stop.class, Help.class, Set.class, Exit.class})
public class CLI implements Runnable {
	public void run() {
		System.out.printf("欢迎使用%s!%n输入 help 获取帮助，输入 exit 退出。%n", HexSync.NAME);
		new Help().run();
		var cmd = new CommandLine(new CLI());
		cmd.setAbbreviatedOptionsAllowed(true);
		cmd.setAbbreviatedSubcommandsAllowed(true);
		cmd.setCaseInsensitiveEnumValuesAllowed(true);
		var scanner = new Scanner(System.in);
		while (true) {
			var line = scanner.nextLine().trim();
			if (line.isEmpty()) continue;
			try {
				cmd.execute(line.split("\\s+"));
			} catch (Exception error) {
				Log.error("命令执行出错: " + error.getMessage());
				System.exit(1);
			}
		}
	}
	@Command(name = "run", description = "运行实例", subcommands = {RunServer.class, RunClient.class})
	static class Run implements Runnable {
		public void run() {new CommandLine(new Run()).usage(System.out);}
		@Command(name = "server", description = "启动服务端")
		static class RunServer implements Runnable {
			public void run() {Server.runServer();}
		}
		@Command(name = "client", description = "启动客户端")
		static class RunClient implements Runnable {
			public void run() {Client.runClient();}
		}
	}
	@Command(name = "stop", description = "关闭实例", subcommands = {StopServer.class, StopClient.class})
	static class Stop implements Runnable {
		public void run() {new CommandLine(new Stop()).usage(System.out);}
		@Command(name = "server", description = "关闭服务端")
		static class StopServer implements Runnable {
			public void run() {Server.stopServer();}
		}
		@Command(name = "client", description = "关闭客户端")
		static class StopClient implements Runnable {
			public void run() {Client.stopClient();}
		}
	}
	@Command(
		name = "set", description = "设置相关参数", subcommands = {
		ServerPort.class,
		ServerLimit.class,
		ServerDirectory.class,
		ServerAutoStart.class,
		ClientPort.class,
		RemoteAddress.class,
		ClientSyncDirectory.class,
		ClientOnlyDirectory.class,
		ClientAutoStart.class,
		Save.class
	}
	)
	static class Set implements Runnable {
		public void run() {new CommandLine(new Setting()).usage(System.out);}
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
