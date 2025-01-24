package HexSync.Util;
import java.io.*;
import java.util.zip.CRC32;

import static HexSync.Util.Log.*;
public class Checksum {
	// 计算文件校验码
	public static long calculateCRC(File file) {
		CRC32 crc = new CRC32();
		try (FileInputStream fileInputStream = new FileInputStream(file)) {
			byte[] buffer = new byte[16384];
			int bytesRead;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) crc.update(buffer, 0, bytesRead);
		} catch (IOException error) {
			log(SEVERE, "校验码计算错误: " + error.getMessage());
			return -1;
		}
		return crc.getValue();
	}
}
