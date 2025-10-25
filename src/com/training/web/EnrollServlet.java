package com.training.web;

import lw.web.restful.SimpleRestful;
import lw.web.lwWebException;
import com.training.dao.DaoFactory;
import com.training.db.Db.Enrollment;
import com.training.db.Db.Course;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

// 学生选课接口：选课/退课/查看我的选课
// 内部包含容量校验、时间冲突检测、候补队列/转正逻辑（见 Db.enroll/drop）
/**
 * 选课接口 Servlet
 * - 职责：学生选课、退课、查询我的选课
 * - 支持 action：
 *   - enroll：选课（返回 status=enrolled/waitlist/conflict）
 *   - drop：退课（返回 ok；可触发候补转正）
 *   - mylist：我的选课列表（包含课程名称）
 * - 会话与权限：需登录并从 Session 读取 `uid`
 * - 依赖：通过 `DaoFactory.enrollment()` 与 `DaoFactory.course()` 访问数据层
 */
public class EnrollServlet extends SimpleRestful {
    /** 处理选课相关请求
     * 支持 action：
     * - enroll：选课（返回 status=enrolled/waitlist/conflict）
     * - drop：退课（返回 ok；可触发候补转正）
     * - mylist：我的选课列表（包含课程名称）
     * 会话：需登录（从 Session 读取 uid）
     * @param req HTTP 请求
     * @param resp HTTP 响应
     * @param jreq 请求体 JSON（含 action）
     * @return 根据 action 返回 JSON/数组
     * @throws lwWebException 未登录/参数错误/不存在
     */
    @Override
    protected Object execute(HttpServletRequest req, HttpServletResponse resp, JSONObject jreq) throws Exception {
        // 选课需要登录，读取会话中的 uid
        HttpSession s=req.getSession(false); if(s==null) throw new lwWebException(401, "未登录");
        String uid=(String)s.getAttribute("uid"); if(uid==null) throw new lwWebException(401, "未登录");
        String action=jreq!=null? jreq.optString("action", ""): "";
        switch(action){
            case "enroll":{
                // 选课：返回 status=enrolled 或 waitlist 或 conflict
                String courseId=jreq.getString("courseId");
                Enrollment e=DaoFactory.enrollment().enroll(uid, courseId);
                if(e==null) throw new lwWebException(404, "课程不存在");
                return new JSONObject().put("status", e.status);
            }
            case "drop":{
                // 退课：若课程有人候补，会自动将候补队列首位转为已选
                String courseId=jreq.getString("courseId"); boolean ok=DaoFactory.enrollment().drop(uid,courseId); return new JSONObject().put("ok", ok);
            }
            case "mylist":{
                // 我的选课列表：显式构造成 JSON 数组，附带课程名称，避免前端再查
                List<Enrollment> list=DaoFactory.enrollment().listUserEnrollments(uid);
                List<Course> all = DaoFactory.course().listCourses();
                JSONArray arr=new JSONArray();
                for(Enrollment e: list){
                    JSONObject je=new JSONObject();
                    je.put("userId", e.userId);
                    je.put("courseId", e.courseId);
                    je.put("status", e.status);
                    String name=null; for(Course c: all){ if(c.id.equals(e.courseId)){ name=c.name; break; } }
                    if(name!=null) je.put("name", name);
                    arr.put(je);
                }
                return arr;
            }
            default: throw new lwWebException(400, "未知action:"+action);
        }
    }
}