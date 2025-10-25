package com.training.web;

import lw.web.restful.SimpleRestful;
import lw.web.lwWebException;
import com.training.dao.DaoFactory;
import com.training.db.Db.Course;
import com.training.db.Db.TimeSlot;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

// 课程管理接口：管理员发布/修改/删除课程，学生可查询与筛选课程
// 时间段采用 day/start/end 三要素，支持冲突检测（在选课逻辑中）
/**
 * 课程管理接口 Servlet
 * - 职责：课程创建/更新/删除、查询与过滤
 * - 支持 action：
 *   - create：管理员发布课程
 *   - update：管理员修改课程（支持部分字段）
 *   - delete：管理员删除课程
 *   - list：列出所有课程（明确 JSON 字段）
 *   - filter：按学分区间与星期过滤
 * - 会话与权限：仅管理员可进行 create/update/delete；查询无需登录
 * - 依赖：通过 `DaoFactory.course()` 访问数据层；时间段 `TimeSlot(day,start,end,date?)`
 */
public class CourseServlet extends SimpleRestful {
    // 将课程对象转换为明确的JSON结构，避免默认反射序列化遗漏字段
    /** 将课程对象转换为明确 JSON 结构
     * @param c 课程对象
     * @return 包含 id/name/credit/capacity/enrolled/times 的 JSON
     */
    private JSONObject toJson(Course c){
        JSONObject j=new JSONObject();
        j.put("id", c.id);
        j.put("name", c.name);
        j.put("credit", c.credit);
        j.put("capacity", c.capacity);
        j.put("enrolled", c.enrolled);
        JSONArray times=new JSONArray();
        for(TimeSlot t: c.times){
            JSONObject jt=new JSONObject();
            jt.put("day", t.day);
            jt.put("start", t.start);
            jt.put("end", t.end);
            if(t.date!=null) jt.put("date", t.date);
            times.put(jt);
        }
        j.put("times", times);
        return j;
    }

    @Override
    /** 处理课程管理请求
     * 支持 action：
     * - create：发布课程（管理员）
     * - update：修改课程（管理员）
     * - delete：删除课程（管理员）
     * - list：列出所有课程（明确字段）
     * - filter：按学分/星期过滤（明确字段）
     * 权限：create/update/delete 需管理员；查询无需登录
     * @param req HTTP 请求
     * @param resp HTTP 响应
     * @param jreq 请求体 JSON（含 action）
     * @return 根据 action 返回 JSON/数组
     * @throws lwWebException 缺少参数/无权限/不存在
     */
    protected Object execute(HttpServletRequest req, HttpServletResponse resp, JSONObject jreq) throws Exception {
        String action=jreq!=null? jreq.optString("action", ""): "";
        if(action==null || action.isEmpty()) throw new lwWebException(400, "缺少action");
        HttpSession s=req.getSession(false);
        String role=s!=null? (String)s.getAttribute("role"): null;
        switch(action){
            case "create":{
                // 仅管理员可发布课程；提交 name/credit/capacity 与 times（数组）
                if(!"admin".equals(role)) throw new lwWebException(403, "仅管理员可发布课程");
                String name=jreq.getString("name");
                int credit=jreq.getInt("credit");
                int capacity=jreq.getInt("capacity");
                JSONArray ts=jreq.getJSONArray("times");
                List<TimeSlot> times=new ArrayList<>();
                for(int i=0;i<ts.length();i++){ JSONObject t=ts.getJSONObject(i); TimeSlot slot=new TimeSlot(t.getString("day"), t.getInt("start"), t.getInt("end")); if(t.has("date")) slot.date=t.getString("date"); times.add(slot); }
                Course c=DaoFactory.course().addCourse(name,credit,capacity,times);
                return new JSONObject().put("id", c.id);
            }
            case "update":{
                // 仅管理员可修改课程；支持部分字段更新
                if(!"admin".equals(role)) throw new lwWebException(403, "仅管理员可修改课程");
                String id=jreq.getString("id");
                String name=jreq.optString("name",null);
                Integer credit=jreq.has("credit")? jreq.getInt("credit"): null;
                Integer capacity=jreq.has("capacity")? jreq.getInt("capacity"): null;
                List<TimeSlot> times=null;
                if(jreq.has("times")){ JSONArray ts=jreq.getJSONArray("times"); times=new ArrayList<>(); for(int i=0;i<ts.length();i++){ JSONObject t=ts.getJSONObject(i); TimeSlot slot=new TimeSlot(t.getString("day"), t.getInt("start"), t.getInt("end")); if(t.has("date")) slot.date=t.getString("date"); times.add(slot); } }
                boolean ok=DaoFactory.course().updateCourse(id,name,credit,capacity,times);
                if(!ok) throw new lwWebException(404, "课程不存在");
                return new JSONObject().put("ok", true);
            }
            case "delete":{
                // 仅管理员可删除课程
                if(!"admin".equals(role)) throw new lwWebException(403, "仅管理员可删除课程");
                String id=jreq.getString("id"); boolean ok=DaoFactory.course().deleteCourse(id); if(!ok) throw new lwWebException(404, "课程不存在"); return new JSONObject().put("ok", true);
            }
            case "list":{
                // 列出所有课程（明确JSON字段，避免前端出现undefined）
                List<Course> list=DaoFactory.course().listCourses();
                JSONArray arr=new JSONArray();
                for(Course c: list){ arr.put(toJson(c)); }
                return arr;
            }
            case "filter":{
                // 基于学分区间和星期过滤课程（明确JSON字段，避免前端出现undefined）
                Integer min=jreq.has("minCredit")? jreq.getInt("minCredit"): null;
                Integer max=jreq.has("maxCredit")? jreq.getInt("maxCredit"): null;
                String day=jreq.optString("day", null);
                List<Course> list=DaoFactory.course().filterCourses(min,max,day);
                JSONArray arr=new JSONArray();
                for(Course c: list){ arr.put(toJson(c)); }
                return arr;
            }
            default: throw new lwWebException(400, "未知action:"+action);
        }
    }
}