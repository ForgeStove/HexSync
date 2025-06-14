package com.forgestove.hexsync.util;
import org.jetbrains.annotations.NotNull;

import java.io.*;
/**
 * 一个将数据同时写入多个输出流的工具类。<p>
 * 该类继承自{@link OutputStream}，允许将相同的数据同时写入多个目标输出流。<p>
 * 这在需要将输出同时发送到多个目标（如文件和控制台）的场景中非常有用。
 */
public class TeeOutputStream extends OutputStream {
	private final OutputStream[] streams;
	/**
	 * 创建一个新的TeeOutputStream
	 *
	 * @param streams 要写入的输出流数组
	 */
	public TeeOutputStream(OutputStream... streams) {this.streams = streams;}
	public void write(int b) throws IOException {for (var stream : streams) stream.write(b);}
	public void write(byte @NotNull [] b) throws IOException {for (var stream : streams) stream.write(b);}
	public void write(byte @NotNull [] b, int off, int len) throws IOException {for (var stream : streams) stream.write(b, off, len);}
	public void flush() throws IOException {for (var stream : streams) stream.flush();}
	public void close() throws IOException {for (var stream : streams) stream.close();}
}
