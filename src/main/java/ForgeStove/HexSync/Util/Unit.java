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
package ForgeStove.HexSync.Util;
import static ForgeStove.HexSync.Util.Log.*;
import static java.lang.Math.multiplyExact;
public class Unit {
	// 速率转换为字节
	public static long convertToBytes(long value, String unit) {
		try {
			switch (unit) {
				case "B":
					return value;
				case "KB":
					return multiplyExact(value, 1024);
				case "MB":
					return multiplyExact(value, 1048576);
				case "GB":
					return multiplyExact(value, 1073741824);
				default:
					log(WARNING, "未知的最大上传速率单位: " + unit);
					return 0;
			}
		} catch (ArithmeticException error) {
			log(WARNING, "最大上传速率溢出，自动转化为无限制: " + error.getMessage());
			return 0;
		}
	}
}
