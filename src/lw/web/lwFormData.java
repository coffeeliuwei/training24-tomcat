package lw.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;



public class lwFormData extends  HashMap<String, String>{
	
	// 轻量级表单/查询字符串解析工具
	// 适合初学者理解 GET 的 "key=value&key2=value2" 结构，以及 POST 文本读取
	public lwFormData() {
		// 无参构造：创建一个空的键值映射
	}
	// 解析 GET 查询字符串，例如：mode=admin&token=111111
	public static lwFormData parse(String query,String charset) {
		lwFormData result=new lwFormData();
		if (query==null) return result;
		
		String[] ppp=query.split("&");
		for (String p : ppp) {
			// 将 "key=value" 形式切分为键和值
			String[] kv=p.split("=");
			String key=kv[0];
			String value="";
			if (key.length()>1) {
				value=kv[1];
			}
			// URL 解码：处理中文或空格等编码字符（如 %E4%B8%AD%E6%96%87）
			if (charset!=null&& value.indexOf("%")>=0) {
				try {
					value=URLDecoder.decode(value, charset);
				} catch (UnsupportedEncodingException e) {
					System.out.println("表单解析错误"+key+","+value+e.getMessage());
				}
			}
			result.put(key, value);
		}
		return result;
	}
	
	// 便捷获取整数：当键不存在或转换失败时返回默认值
	public int  getInt(String key,int defValue) {
		try {
			return Integer.valueOf(this.get(key));
		} catch (Exception e) {
			return defValue;
		}
	}
	
	// 从输入流读取文本（用于POST请求体），限制最大读取字节避免内存过度占用
	public static String readAsText(InputStream stream,String charset ,int maxsize) throws IOException {
		ByteArrayOutputStream cache=new ByteArrayOutputStream(maxsize);
		byte[] data=new byte[1024];
		while (true) {
			int n=stream.read(data);
			if (n<0) break; // 读取结束
			if (n==0) continue; // 无数据继续读取
			cache.write(data,0,n);
			if (cache.size()>maxsize) 
				break; // 保护：超过上限立即停止
		}
		return cache.toString(charset);
		
	}

}