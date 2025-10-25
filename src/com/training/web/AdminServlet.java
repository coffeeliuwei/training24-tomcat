package com.training.web;

import lw.web.restful.SimpleRestful;
import lw.web.lwWebException;
import com.training.dao.DaoFactory;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// 管理员接口：查看系统统计与日志
// 需管理员权限（role=admin），否则返回403
public class AdminServlet extends SimpleRestful {
    @Override
    protected Object execute(HttpServletRequest req, HttpServletResponse resp, org.json.JSONObject jreq) throws Exception {
        HttpSession s=req.getSession(false); if(s==null) throw new lwWebException(401, "未登录");
        String role=(String)s.getAttribute("role"); if(!"admin".equals(role)) throw new lwWebException(403, "需要管理员权限");
        String action=jreq!=null? jreq.optString("action", ""): "";
        switch(action){
            case "stats": return DaoFactory.admin().stats();   // 返回用户数量、课程数量、选课总数
            case "logs_query": return DaoFactory.admin().getLogs(); // 返回系统操作日志（简单字符串列表）
            default: throw new lwWebException(400, "未知action:"+action);
        }
    }
}