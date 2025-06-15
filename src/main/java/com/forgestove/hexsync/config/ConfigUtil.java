package com.forgestove.hexsync.config;
import com.forgestove.hexsync.config.ConfigEntry.ValueEntry;
import com.forgestove.hexsync.util.*;

import java.util.stream.Collectors;
public class ConfigUtil {
	// 加载配置
	public static void load() {
		var configFile = Data.CONFIG_PATH.toFile();
		if (!configFile.isFile()) {
			save();
			return;
		}
		var configMap = Data.CONFIG_ENTRIES.stream()
			.filter(entry -> entry instanceof ValueEntry<?>)
			.map(entry -> (ValueEntry<?>) entry)
			.collect(Collectors.toMap(ValueEntry::key, ValueEntry::setter));
		FileUtil.readLine(configFile, line -> {
			if (!line.matches("^[a-zA-Z].*")) return;
			var parts = line.trim().split("=");
			if (parts.length != 2) return;
			var action = configMap.get(parts[0]);
			if (action != null) action.accept(parts[1]);
			else Log.warn("配置项错误: " + line);
		});
	}
	// 保存配置
	public static void save() {
		var configContent = Data.CONFIG_ENTRIES.stream()
			.map(ConfigEntry::toObjectArray)
			.map(config -> config[0].toString().startsWith("#")
				? config[0].toString()
				: "%s=%s".formatted(config[0], config.length > 1 ? config[1] : ""))
			.collect(Collectors.joining(System.lineSeparator()));
		FileUtil.writeFile(Data.CONFIG_PATH.toFile(), configContent);
		Log.info("配置已保存: %n%s".formatted(configContent));
	}
}
