package com.forgestove.hexsync.config;
import com.forgestove.hexsync.config.ConfigEntry.ValueEntry;
import com.forgestove.hexsync.util.Log;
import org.jetbrains.annotations.*;

import java.io.*;
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
		try (var bufferedReader = new BufferedReader(new FileReader(configFile))) {
			var configMap = createConfigMap();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (!line.matches("^[a-zA-Z].*")) continue; // 仅当首字符不是字母时跳过
				var parts = line.trim().split("=");
				if (parts.length != 2) {
					Log.warn("配置格式错误: " + line);
					continue;
				}
				var action = configMap.get(parts[0]);
				if (action != null) action.accept(parts[1]);
				else Log.warn("配置项错误: " + line);
			}
		} catch (IOException error) {
			Log.error("配置读取失败: " + error.getMessage());
		}
	}
	// 保存配置
	public static void saveConfig() {
		var joiner = new StringJoiner(System.lineSeparator());
		for (var config : getConfigEntries())
			joiner.add(config[0].toString().startsWith("#")
				? config[0].toString()
				: String.format("%s=%s", config[0], config.length > 1 ? config[1] : ""));
		var configFile = new File(Data.CONFIG_PATH);
		try (var bufferedWriter = new BufferedWriter(new FileWriter(configFile))) {
			bufferedWriter.write(joiner.toString());
			Log.info("配置已保存: %s%s", System.lineSeparator(), joiner);
		} catch (IOException error) {
			Log.error("配置保存失败: " + error.getMessage());
		}
	}
	// 创建配置映射
	public static @NotNull Map<String, Consumer<String>> createConfigMap() {
		Map<String, Consumer<String>> configMap = new HashMap<>();
		Data.CONFIG_ENTRIES.stream().filter(entry -> entry instanceof ValueEntry).forEach(entry -> {
			var valueEntry = (ValueEntry<?>) entry;
			configMap.put(valueEntry.key(), valueEntry.setter());
		});
		return configMap;
	}
	@Contract(value = " -> new", pure = true)
	public static Object @NotNull [][] getConfigEntries() {
		return Data.CONFIG_ENTRIES.stream().map(ConfigEntry::toObjectArray).toArray(Object[][]::new);
	}
}
