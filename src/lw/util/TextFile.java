package lw.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

// 简单文本文件读写工具
// 适用于导出报告（如CSV）与读取小型配置文件；为保护内存，read做了文件大小限制
public class TextFile {
	
	// 读取整个文件为字符串，指定字符集
	public static String read(File f,String charset) throws Exception {
		FileInputStream fStream=new FileInputStream(f);
		
		int filesize=(int)f.length();
		if (filesize>1024*512) 
			throw new Exception("文件过大"+filesize);
		
		byte[] buffer=new byte[filesize];
		fStream.read(buffer);
		return new String(buffer, charset);
	}
	
	// 写入文本到文件（覆盖写），指定字符集
	public static void write(File f,String text,String charset) throws Exception {
			FileOutputStream fStream=new FileOutputStream(f);
			try {
				fStream.write(text.getBytes(charset));
			} catch (Exception e) {
				fStream.close();
			}
			
		
	}

}