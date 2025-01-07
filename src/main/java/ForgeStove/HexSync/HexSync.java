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
package ForgeStove.HexSync;
import static ForgeStove.HexSync.Client.*;
import static ForgeStove.HexSync.Config.loadConfig;
import static ForgeStove.HexSync.HeadlessUI.headlessUI;
import static ForgeStove.HexSync.Log.initLog;
import static ForgeStove.HexSync.NormalUI.*;
import static ForgeStove.HexSync.Server.*;
public class HexSync {
	public static final String HEX_SYNC_NAME = "HexSync"; // 程序名称
	public static final String GITHUB_URL = "https://github.com/ForgeStove/HexSync"; // 项目GitHub地址
	public static void main(String[] args) {
		initLog();
		loadConfig();
		if (serverAutoStart) startServer();
		if (clientAutoStart) startClient();
		if (HEADLESS) headlessUI();
		else normalUI();
	}
}