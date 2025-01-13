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
import java.io.*;
import java.util.zip.CRC32;

import static ForgeStove.HexSync.Util.Log.*;
public class Checksum {
	// 计算文件校验码
	public static long calculateCRC(File file) {
		CRC32 crc = new CRC32();
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) crc.update(buffer, 0, bytesRead);
		} catch (IOException error) {
			log(SEVERE, "计算CRC时出错: " + error.getMessage());
		}
		return crc.getValue();
	}
}
