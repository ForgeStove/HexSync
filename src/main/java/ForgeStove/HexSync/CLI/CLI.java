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
import ForgeStove.HexSync.Client.Client;
import ForgeStove.HexSync.Server.Server;
import ForgeStove.HexSync.Util.Println;

import java.util.*;

import static ForgeStove.HexSync.HexSync.*;
import static ForgeStove.HexSync.Util.Log.*;
import static java.lang.System.*;
public class CLI {
	// 命令行界面
	public static void initCLI() {
		out.println("欢迎使用" + HEX_SYNC_NAME + "!");
		out.println("输入HELP以获取帮助.");
		Map<String, Runnable> map = new HashMap<>();
		map.put("RS", Server::runServer);
		map.put("RC", Client::runClient);
		map.put("SS", Server::stopServer);
		map.put("SC", Client::stopClient);
		map.put("SET", HeadlessSettings::headlessSettings);
		map.put("GITHUB", () -> out.println(GITHUB_URL));
		map.put(
				"HELP", () -> Println.printlnStrings(new String[]{
						"RS     |启动服务端",
						"RC     |启动客户端",
						"SS     |停止服务端",
						"SC     |停止客户端",
						"SET    |设置",
						"GITHUB |仓库",
						"EXIT   |退出",
						"HELP   |帮助"
				})
		);
		map.put("EXIT", () -> exit(0));
		Scanner scanner = new Scanner(in);
		while (true) try {
			out.print(HEX_SYNC_NAME + ">");
			String line = scanner.nextLine().toUpperCase();
			if (line.isEmpty()) continue;
			map.getOrDefault(line, () -> err.println("无效命令,输入HELP以获取帮助.")).run();
		} catch (Exception error) {
			log(SEVERE, "命令处理时出错: " + error.getMessage());
			break;
		}
	}
}
