package com.forgestove.hexsync.cli;
import com.forgestove.hexsync.cli.Setting.*;
import com.forgestove.hexsync.config.*;
import com.forgestove.hexsync.util.network.*;
import com.forgestove.hexsync.util.network.Rate.Unit;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.nio.file.Path;
@Command(
	name = "set", description = "设置相关参数", subcommands = {
	ServerSetting.class, ClientSetting.class, Save.class
}
)
public class Setting implements Runnable {
	public void run() {new CommandLine(this).usage(System.out);}
	@Command(
		name = "server", description = "服务端设置", subcommands = {
		ServerSetting.ServerPort.class, ServerSetting.RateLimit.class, ServerSetting.ServerDirectory.class, ServerSetting.ServerAuto.class
	}
	)
	static class ServerSetting implements Runnable {
		public void run() {new CommandLine(this).usage(System.out);}
		@Command(name = "port", description = "端口")
		static class ServerPort implements Runnable {
			@Parameters(description = "端口") Port port;
			public void run() {Data.serverPort.set(port);}
		}
		@Command(name = "rate", description = "最大上传速率")
		static class RateLimit implements Runnable {
			@Parameters(description = "数字") long value;
			@Parameters(description = "单位") Unit unit;
			public void run() {Data.serverUploadRate.set(new Rate(value, unit));}
		}
		@Command(name = "dir", description = "服务端同步目录")
		static class ServerDirectory implements Runnable {
			@Parameters(description = "目录") Path dir;
			public void run() {Data.serverSyncPath.set(dir);}
		}
		@Command(name = "auto", description = "服务端自动启动")
		static class ServerAuto implements Runnable {
			@Parameters(description = "true/false") boolean auto;
			public void run() {Data.serverAuto.set(auto);}
		}
	}
	@Command(
		name = "client", description = "客户端设置", subcommands = {
		ClientSetting.ClientPort.class,
		ClientSetting.RemoteAddress.class,
		ClientSetting.ClientSyncDirectory.class,
		ClientSetting.ClientOnlyDirectory.class,
		ClientSetting.ClientAutoStart.class
	}
	)
	static class ClientSetting implements Runnable {
		public void run() {new CommandLine(this).usage(System.out);}
		@Command(name = "port", description = "端口")
		static class ClientPort implements Runnable {
			@Parameters(description = "端口") Port port;
			public void run() {Data.clientPort.set(port);}
		}
		@Command(name = "address", description = "远程地址")
		static class RemoteAddress implements Runnable {
			@Parameters(description = "地址") String address;
			public void run() {Data.remoteAddress.set(address);}
		}
		@Command(name = "syncDir", description = "客户端同步目录")
		static class ClientSyncDirectory implements Runnable {
			@Parameters(description = "目录") Path dir;
			public void run() {Data.clientSyncPath.set(dir);}
		}
		@Command(name = "onlyDir", description = "客户端模组目录")
		static class ClientOnlyDirectory implements Runnable {
			@Parameters(description = "目录") Path dir;
			public void run() {Data.clientOnlyPath.set(dir);}
		}
		@Command(name = "auto", description = "客户端自动启动")
		static class ClientAutoStart implements Runnable {
			@Parameters(description = "true/false") boolean auto;
			public void run() {Data.clientAuto.set(auto);}
		}
	}
	@Command(name = "save", description = "保存设置")
	static class Save implements Runnable {
		public void run() {ConfigUtil.save();}
	}
}
