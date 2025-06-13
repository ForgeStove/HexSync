package com.forgestove.hexsync.config;
import com.forgestove.hexsync.HexSync;
import com.forgestove.hexsync.config.ConfigEntry.*;
import com.forgestove.hexsync.util.*;
import com.forgestove.hexsync.util.Rate.Unit;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;

import java.nio.file.Path;
import java.util.List;
public class Data {
	public static final Path CONFIG_PATH = Path.of(HexSync.NAME, "config.properties");
	public static final Path LOG_PATH = Path.of(HexSync.NAME, "latest.log");
	public static final Config<Path> serverSyncPath = new Config<>(Path.of("mods"));
	public static final Config<Path> clientSyncPath = new Config<>(Path.of("mods"));
	public static final Config<Path> clientOnlyPath = new Config<>(Path.of("clientMods"));
	public static final Config<String> remoteAddress = new Config<>("localhost");
	public static final Config<Rate> serverUploadRate = new Config<>(new Rate(1, Unit.Mbps));
	public static final Config<Boolean> clientAuto = new Config<>(false);
	public static final Config<Boolean> serverAuto = new Config<>(false);
	public static final Config<Port> serverPort = new Config<>(new Port(Port.MAX_VALUE));
	public static final Config<Port> clientPort = new Config<>(new Port(Port.MAX_VALUE));
	public static final Config<String> theme = new Config<>(FlatMTGitHubDarkIJTheme.NAME);
	public static final List<ConfigEntry> CONFIG_ENTRIES = List.of( //
		new HeaderEntry("# Server"),
		ValueEntry.value("serverPort", serverPort, Port::new),
		ValueEntry.value("serverUploadRate", serverUploadRate, Rate::new),
		ValueEntry.value("serverSyncPath", serverSyncPath, Path::of),
		ValueEntry.value("serverAuto", serverAuto, Boolean::parseBoolean),
		new HeaderEntry("# Client"),
		ValueEntry.value("clientPort", clientPort, Port::new),
		ValueEntry.value("remoteAddress", remoteAddress, String::new),
		ValueEntry.value("clientSyncPath", clientSyncPath, Path::of),
		ValueEntry.value("clientOnlyPath", clientOnlyPath, Path::of),
		ValueEntry.value("clientAuto", clientAuto, Boolean::parseBoolean),
		new HeaderEntry("# UI"),
		ValueEntry.value("theme", theme, String::new));
}
