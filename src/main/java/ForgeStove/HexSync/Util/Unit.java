package ForgeStove.HexSync.Util;
import static ForgeStove.HexSync.Util.Log.*;
import static java.lang.Math.multiplyExact;
public class Unit {
	// 速率转换为字节
	public static long convertToBytes(long value, String unit) {
		try {
			switch (unit) {
				case "Bps":
					return value;
				case "KBps":
					return multiplyExact(value, 1024);
				case "MBps":
					return multiplyExact(value, 1048576);
				case "GBps":
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
