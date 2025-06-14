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
		@Parameters(description = "端口") Port port;
		public void run() {Data.serverPort.set(port);}
	}
	@Command(name = "sl", description = "设置限速")
	static class ServerLimit implements Runnable {
		@Parameters(description = "数字") long value;
		@Parameters(description = "单位") Unit unit;
		public void run() {Data.serverUploadRate.set(new Rate(value, unit));}
	}
	@Command(name = "sd", description = "设置服务端同步目录")
	static class ServerDirectory implements Runnable {
		@Parameters(description = "目录") Path dir;
		public void run() {Data.serverSyncPath.set(dir);}
	}
	@Command(name = "ss", description = "设置服务端自动启动")
	static class ServerAutoStart implements Runnable {
		@Parameters(description = "true/false") boolean auto;
		public void run() {Data.serverAuto.set(auto);}
	}
	@Command(name = "cp", description = "设置客户端端口")
	static class ClientPort implements Runnable {
		@Parameters(description = "端口") Port port;
		public void run() {Data.clientPort.set(port);}
	}
	@Command(name = "ra", description = "设置远程地址")
	static class RemoteAddress implements Runnable {
		@Parameters(description = "地址") String address;
		public void run() {Data.remoteAddress.set(address);}
	}
	@Command(name = "cd", description = "设置客户端同步目录")
	static class ClientSyncDirectory implements Runnable {
		@Parameters(description = "目录") Path dir;
		public void run() {Data.clientSyncPath.set(dir);}
	}
	@Command(name = "co", description = "设置仅客户端模组目录")
	static class ClientOnlyDirectory implements Runnable {
		@Parameters(description = "目录") Path dir;
		public void run() {Data.clientOnlyPath.set(dir);}
	}
	@Command(name = "cs", description = "设置客户端自动启动")
	static class ClientAutoStart implements Runnable {
		@Parameters(description = "true/false") boolean auto;
		public void run() {Data.clientAuto.set(auto);}
	}
	@Command(name = "save", description = "保存设置")
	static class Save implements Runnable {
		public void run() {ConfigUtil.save();}
	}
}
