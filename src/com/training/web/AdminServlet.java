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
/**
 * 管理员接口 Servlet
 * - 职责：提供系统统计与操作日志查询入口
 * - 支持 action：
 *   - stats：返回用户数、课程数、选课总数（Map<String,Object>）
 *   - logs_query：返回系统操作日志（List<String>）
 * - 认证与权限：需已登录且 Session 中 `role=admin`，否则 401/403
 * - 依赖：通过 `DaoFactory.admin()` 访问数据层，避免直接依赖 `Db`
 */
public class AdminServlet extends SimpleRestful {
    /** 处理管理员端请求
     * 支持 action：
     * - stats：系统统计（users/courses/enrollments）
     * - logs_query：操作日志列表
     * 会话：需登录且 role=admin
     * @param req HTTP 请求
     * @param resp HTTP 响应
     * @param jreq 请求体 JSON（含 action）
     * @return Map 或 List，根据 action 返回
     * @throws lwWebException 未登录/无权限/未知 action
     */
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