package com.forgestove.hexsync.config;
import com.forgestove.hexsync.config.ConfigEntry.ValueEntry;
import com.forgestove.hexsync.util.*;
import org.jetbrains.annotations.*;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
public class ConfigUtil {
	// 加载配置
	public static void loadConfig() {
		var configFile = new File(Data.CONFIG_PATH);
		if (!configFile.isFile()) {
			saveConfig();
			return;
		}
		var configMap = createConfigMap();
		FileUtil.readLines(configFile, line -> {
			if (!line.matches("^[a-zA-Z].*")) return;
			var parts = line.trim().split("=");
			if (parts.length != 2) return;
			var action = configMap.get(parts[0]);
			if (action != null) action.accept(parts[1]);
			else Log.warn("配置项错误: " + line);
		});
	}
	// 保存配置
	public static void saveConfig() {
		var joiner = new StringJoiner(System.lineSeparator());
		var entries = getConfigEntries();
		for (var config : entries)
			joiner.add(config[0].toString().startsWith("#")
				? config[0].toString()
				: String.format("%s=%s", config[0], config.length > 1 ? config[1] : ""));
		FileUtil.writeFile(new File(Data.CONFIG_PATH), joiner.toString());
		Log.info("配置已保存: %n%s", joiner);
	}
	// 创建配置映射
	public static @NotNull Map<String, Consumer<String>> createConfigMap() {
		var configMap = new HashMap<String, Consumer<String>>();
		for (var entry : Data.CONFIG_ENTRIES)
			if (entry instanceof ValueEntry<?> valueEntry) configMap.put(valueEntry.key(), valueEntry.setter());
		return configMap;
	}
	@Contract(value = " -> new", pure = true)
	public static Object @NotNull [][] getConfigEntries() {
		return Data.CONFIG_ENTRIES.stream().map(ConfigEntry::toObjectArray).toArray(Object[][]::new);
	}
}
