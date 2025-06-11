package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.cli.Setting.*;
import com.forgestove.hexsync.client.Client;
import com.forgestove.hexsync.config.Config;
import com.forgestove.hexsync.server.Server;
import com.forgestove.hexsync.util.SettingUtil;
import picocli.CommandLine;
import picocli.CommandLine.*;
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
public class Setting implements Runnable {
	public void run() {new CommandLine(this).usage(System.out);}
	@Command(name = "sp", description = "设置服务端端口")
	static class ServerPort implements Runnable {
		@Parameters(description = "端口") String port;
		public void run() {SettingUtil.canSetPort(port, true);}
	}
	@Command(name = "sl", description = "设置限速")
	static class ServerLimit implements Runnable {
		@Parameters(description = "数字") String number;
		@Parameters(description = "单位") String unit;
		public void run() {SettingUtil.setRate(number + " " + unit);}
	}
	@Command(name = "sd", description = "设置服务端同步目录")
	static class ServerDirectory implements Runnable {
		@Parameters(description = "目录") String dir;
		public void run() {SettingUtil.setDirectory(dir, "服务端同步", value -> Config.serverSyncDirectory = value);}
	}
	@Command(name = "ss", description = "设置服务端自动启动")
	static class ServerAutoStart implements Runnable {
		@Parameters(description = "true/false") String auto;
		public void run() {SettingUtil.setAutoStart(auto, true, value -> Server.serverAutoStart = value);}
	}
	@Command(name = "cp", description = "设置客户端端口")
	static class ClientPort implements Runnable {
		@Parameters(description = "端口") String port;
		public void run() {SettingUtil.canSetPort(port, false);}
	}
	@Command(name = "ra", description = "设置服务器地址")
	static class RemoteAddress implements Runnable {
		@Parameters(description = "地址") String address;
		public void run() {
			Config.remoteAddress = address;
			System.out.println("服务器地址已设置为: " + Config.remoteAddress);
		}
	}
	@Command(name = "cd", description = "设置客户端同步目录")
	static class ClientSyncDirectory implements Runnable {
		@Parameters(description = "目录") String dir;
		public void run() {SettingUtil.setDirectory(dir, "客户端同步", value -> Config.clientSyncDirectory = value);}
	}
	@Command(name = "co", description = "设置仅客户端模组目录")
	static class ClientOnlyDirectory implements Runnable {
		@Parameters(description = "目录") String dir;
		public void run() {SettingUtil.setDirectory(dir, "仅客户端模组", value -> Config.clientOnlyDirectory = value);}
	}
	@Command(name = "cs", description = "设置客户端自动启动")
	static class ClientAutoStart implements Runnable {
		@Parameters(description = "true/false") String auto;
		public void run() {SettingUtil.setAutoStart(auto, false, value -> Client.clientAutoStart = value);}
	}
	@Command(name = "save", description = "保存设置")
	static class Save implements Runnable {
		public void run() {Config.saveConfig();}
	}
}

