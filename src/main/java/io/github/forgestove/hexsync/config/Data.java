package io.github.forgestove.hexsync.config;
import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme;
import io.github.forgestove.hexsync.HexSync;
import io.github.forgestove.hexsync.config.ConfigEntry.*;
import io.github.forgestove.hexsync.util.network.*;
import io.github.forgestove.hexsync.util.network.Rate.Unit;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.nio.file.Path;
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
	public static final Config<Path> script = new Config<>(null);
	public static final ObjectList<ConfigEntry> CONFIG_ENTRIES = ObjectList.of( //
		new Header("# Server"),
		Value.of("serverPort", serverPort, Port::new),
		Value.of("serverUploadRate", serverUploadRate, Rate::new),
		Value.of("serverSyncPath", serverSyncPath, Path::of),
		Value.of("serverAuto", serverAuto, Boolean::parseBoolean),
		new Header("# Client"),
		Value.of("clientPort", clientPort, Port::new),
		Value.of("remoteAddress", remoteAddress, String::new),
		Value.of("clientSyncPath", clientSyncPath, Path::of),
		Value.of("clientOnlyPath", clientOnlyPath, Path::of),
		Value.of("clientAuto", clientAuto, Boolean::parseBoolean),
		new Header("# Other"),
		Value.of("theme", theme, String::new),
		Value.of("script", script, Path::of));
}
