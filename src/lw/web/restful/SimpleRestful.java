package lw.web.restful;

// 简易REST基础类：统一处理请求读取、异常捕获、JSON响应格式化
// 面向初学者说明：
// - 所有业务Servlet继承本类，仅需实现 execute 方法即可。
// - 本类负责：将请求体读取为JSON、调用业务逻辑、捕获异常并输出统一的JSON响应。
// - 响应格式固定为：{"error":0, "reason":"ok", "data":...}；当出现错误时 error!=0，reason为错误原因。

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import lw.web.lwFormData;
import lw.web.lwWebException;

public abstract class SimpleRestful extends HttpServlet{
    // 默认字符集，用于请求体读取与响应输出
	protected String charset="UTF-8";
	// 读取请求体的最大字节数，防止过大请求占用内存（约19KB）
	protected int max_req_size=1024*19;
	// 是否使用美化的JSON格式（缩进）输出，便于调试与学习
	protected boolean JsonFormat=true;
	
	// 业务入口：子类实现具体逻辑，传入请求对象、响应对象以及解析好的JSON请求
	protected abstract Object execute(HttpServletRequest req,HttpServletResponse resp,JSONObject jreq) throws Exception;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 统一走 POST 逻辑，便于代码复用
		doPost(req, resp);
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 构造一个最终的响应JSON对象
		JSONObject jresp=new JSONObject();
		
		try {
			// 读取请求体文本（例如：{"action":"login",...}），并尝试解析为JSON
			String reqText=lwFormData.readAsText(req.getInputStream(), charset, max_req_size);
			JSONObject jreq=null;
			if (reqText.length()>0) {
				jreq=new JSONObject(reqText);
			}
			// 调用子类的业务实现，拿到返回数据（可以是任意对象/集合/JSON）
			Object data=execute(req, resp, jreq);
			
			// 正常响应：error=0，reason=ok，并返回 data
			jresp.put("error", 0);
			jresp.put("reason", "ok");
			if (data!=null) {
				jresp.put("data", data);
			}
			
			resp.setCharacterEncoding("UTF-8");
			resp.setContentType("application/json");
			PrintWriter writer=resp.getWriter();
			String jsonstr=JsonFormat?jresp.toString(2):jresp.toString();
			writer.write(jsonstr);
			writer.close();
			
		}catch (lwWebException e) {
			// 业务异常：例如参数缺失、权限不足等，使用自定义错误码 e.error
			String reason=e.getMessage();
			jresp.put("error", e.error);
			jresp.put("reason", reason);
			resp.setCharacterEncoding("UTF-8");
			resp.setContentType("application/json");
			PrintWriter writer=resp.getWriter();
			String jsonstr=JsonFormat?jresp.toString(2):jresp.toString();
			writer.write(jsonstr);
			writer.close();
		} 
		catch (Exception e) {
			// 未知异常：例如运行时错误，统一返回 error=-1，reason为异常类型或消息
			String reason=e.getMessage();
			if (reason==null) {
				reason=e.getClass().getName();
				jresp.put("error", -1);
				jresp.put("reason", reason);
			}
			resp.setCharacterEncoding("UTF-8");
			resp.setContentType("application/json");
			PrintWriter writer=resp.getWriter();
			String jsonstr=JsonFormat?jresp.toString(2):jresp.toString();
			writer.write(jsonstr);
			writer.close();
		}
		
		
		
	}

}