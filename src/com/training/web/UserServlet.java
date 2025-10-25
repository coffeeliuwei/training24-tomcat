package com.training.web;

import lw.web.restful.SimpleRestful;
import lw.web.lwFormData;
import lw.web.lwWebException;
import com.training.dao.DaoFactory;
import com.training.db.Db.User;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

// 用户相关接口：注册、登录、注销、密码重置
// 使用会话(HttpSession)保存登录状态；所有响应均由基础类输出统一JSON格式
/**
 * 用户接口 Servlet
 * - 职责：注册、登录、注销、密码重置；启动时进行示例数据种子初始化
 * - 支持 action：
 *   - register：注册用户，返回 id/username/role
 *   - login：登录并写入 Session（uid/username/role），返回基本用户信息
 *   - logout：注销，销毁 Session
 *   - reset：按用户名重置密码，返回 ok
 * - 会话与安全：使用 HttpSession 维护登录状态；登录失败返回 401
 * - 依赖：通过 `DaoFactory.user()` 与 `DaoFactory.admin().seed()` 与数据层交互
 */
public class UserServlet extends SimpleRestful {
    /** 初始化示例数据
     * 在容器启动时调用，便于演示与联调
     */
    @Override
    public void init(){ DaoFactory.admin().seed(); }

    /** 处理用户相关请求
     * 支持 action：
     * - register：注册用户（username/password 必填；role 默认 student）
     * - login：登录并写入 Session（uid/username/role）
     * - logout：注销会话
     * - reset：按用户名重置密码
     * 会话：通过 HttpSession 管理登录状态
     * @param req HTTP 请求
     * @param resp HTTP 响应
     * @param jreq 请求体 JSON（含 action）
     * @return 根据 action 返回 JSON 对象
     * @throws lwWebException 参数错误/认证失败/资源不存在
     */
    @Override
    protected Object execute(HttpServletRequest req, HttpServletResponse resp, JSONObject jreq) throws Exception {
        // 解析查询字符串（GET 部分），演示 lwFormData.parse 的用法
        String qs=req.getQueryString();
        lwFormData q=lwFormData.parse(qs, charset);
        String mode=q.getOrDefault("mode", ""); // 示意：可用于开启某些调试开关
        
        // 读取请求体中的 action，用于区分具体的业务操作
        String action=jreq!=null? jreq.optString("action", ""): "";
        if(action==null || action.isEmpty()) throw new lwWebException(400, "缺少action");
        switch(action){
            case "register":{
                // 注册：username/password 必填，role 默认 student，email 可选
                String username=jreq.getString("username");
                String password=jreq.getString("password");
                String email=jreq.optString("email","");
                String role=jreq.optString("role","student");
                if(DaoFactory.user().findByName(username)!=null) throw new lwWebException(409, "用户已存在");
                User u=DaoFactory.user().addUser(username,password,role,email);
                return new JSONObject().put("id",u.id).put("username",u.username).put("role",u.role);
            }
            case "login":{
                // 登录：校验用户名和密码，成功后将 uid/username/role 写入会话
                String username=jreq.getString("username");
                String password=jreq.getString("password");
                User u=DaoFactory.user().auth(username,password);
                if(u==null) throw new lwWebException(401, "用户名或密码错误");
                HttpSession s=req.getSession(true);
                s.setAttribute("uid", u.id);
                s.setAttribute("username", u.username);
                s.setAttribute("role", u.role);
                return new JSONObject().put("id",u.id).put("username",u.username).put("role",u.role);
            }
            case "logout":{
                // 注销：销毁会话对象
                HttpSession s=req.getSession(false); if(s!=null) s.invalidate();
                return new JSONObject().put("ok", true);
            }
            case "reset":{
                // 密码重置：按用户名更新密码
                String username=jreq.getString("username");
                String newPwd=jreq.getString("newPwd");
                boolean ok=DaoFactory.user().resetPassword(username,newPwd);
                if(!ok) throw new lwWebException(404, "用户不存在");
                return new JSONObject().put("ok", true);
            }
            default: throw new lwWebException(400, "未知action:"+action);
        }
    }
}