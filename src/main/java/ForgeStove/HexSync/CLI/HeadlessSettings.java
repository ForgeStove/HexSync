// Copyright (C) 2025 ForgeStove
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package ForgeStove.HexSync.CLI;
import ForgeStove.HexSync.Util.*;

import java.util.*;
import java.util.function.Consumer;

import static ForgeStove.HexSync.Client.Client.clientAutoStart;
import static ForgeStove.HexSync.HexSync.HEX_SYNC_NAME;
import static ForgeStove.HexSync.Server.Server.serverAutoStart;
import static ForgeStove.HexSync.Util.Config.*;
import static java.lang.System.*;
public class HeadlessSettings {
	// 无头模式设置
	public static void headlessSettings() {
		Map<String, Consumer<String[]>> map = new HashMap<>();
		map.put("SP", args -> Settings.canSetPort(args[1], true));
		map.put("SL", args -> Settings.setRate(args[1] + " " + args[2]));
		map.put("SD", args -> Settings.setDirectory(args[1], "服务端同步", value -> serverSyncDirectory = value));
		map.put("SS", args -> Settings.setAutoStart(args[1], true, value -> serverAutoStart = value));
		map.put("CP", args -> Settings.canSetPort(args[1], false));
		map.put(
				"SA", args -> {
					serverAddress = args[1];
					out.println("服务器地址已设置为: " + serverAddress);
				}
		);
		map.put("CD", args -> Settings.setDirectory(args[1], "客户端同步", value -> clientSyncDirectory = value));
		map.put("CO", args -> Settings.setDirectory(args[1], "仅客户端模组", value -> clientOnlyDirectory = value));
		map.put("CS", args -> Settings.setAutoStart(args[1], false, value -> clientAutoStart = value));
		map.put("SAVE", args -> saveConfig());
		map.put(
				"HELP", args -> Println.printlnStrings(new String[]{
						"SP [端口号]            |设置服务端端口",
						"SL [速率] [B/KB/MB/GB] |设置服务端最大上传速率",
						"SD [目录]              |设置服务端同步目录",
						"SS [Y/N]               |设置服务端自动启动",
						"CP [端口号]            |设置客户端端口",
						"SA [地址]              |设置服务器地址",
						"CD [目录]              |设置客户端同步目录",
						"CO [目录]              |设置客户端仅客户端目录",
						"CS [Y/N]               |设置客户端自动启动",
						"SAVE                   |保存并退出设置",
						"EXIT                   |退出设置而不保存",
						"HELP                   |帮助"
				})
		);
		out.println("进入设置模式,输入命令或输入HELP以获取帮助.");
		Scanner scanner = new Scanner(in);
		while (true) try {
			out.print(HEX_SYNC_NAME + "Settings>");
			String[] parts = scanner.nextLine().split("\\s+");
			if (parts.length == 0) continue;
			if (parts[0].equalsIgnoreCase("EXIT")) break;
			map.getOrDefault(parts[0].toUpperCase(), args -> err.println("无效命令,输入HELP以获取帮助.")).accept(parts);
			if (parts[0].equalsIgnoreCase("SAVE")) break;
		} catch (Exception error) {
			err.println("无效命令,输入HELP以获取帮助.");
		}
	}
}
