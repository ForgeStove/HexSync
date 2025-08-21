package io.github.forgestove.hexsync.config;
import io.github.forgestove.hexsync.config.ConfigEntry.Value;
import io.github.forgestove.hexsync.util.*;

import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * 配置工具类
 * <p>
 * 提供加载和保存配置文件的功能
 * </p>
 */
public class ConfigUtil {
	/**
	 * 加载配置
	 * <p>
	 * 从配置文件中加载配置项。如果配置文件不存在，则创建默认配置文件。
	 * 配置文件中的每一行应当是"键=值"的格式。
	 * </p>
	 */
	public static void load() {
		var configFile = Data.CONFIG_PATH.toFile();
		if (!configFile.isFile()) {
			save();
			return;
		}
		var configMap = Data.CONFIG_ENTRIES.stream().filter(Value.class::isInstance).map(Value.class::cast)
			.collect(Collectors.toMap(Value::key, Function.identity()));
		FileUtil.readLine(
			configFile, line -> {
				if (!line.matches("^[a-zA-Z].*")) return;
				var parts = line.trim().split("=");
				if (parts.length != 2) return;
				var action = configMap.get(parts[0]);
				if (action != null) action.accept(parts[1]);
				else Log.warn("配置项错误: " + line);
			}
		);
	}
	/**
	 * 保存配置
	 * <p>
	 * 将当前配置保存到配置文件中。
	 * 配置项会以{@code 键=值}的格式写入，以{@code #}开头的配置项会作为注释保存。
	 * </p>
	 */
	public static void save() {
		var configContent = Data.CONFIG_ENTRIES.stream().map(ConfigEntry::toString).collect(Collectors.joining(System.lineSeparator()));
		FileUtil.writeFile(Data.CONFIG_PATH.toFile(), configContent);
		Log.info("配置已保存: " + System.lineSeparator() + configContent);
	}
}
