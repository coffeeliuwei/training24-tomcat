package com.training.web;

import lw.web.restful.SimpleRestful;
import lw.web.lwWebException;
import lw.util.TextFile;
import com.training.dao.DaoFactory;
import com.training.db.Db.Course;
import com.training.db.Db.Enrollment;
import com.training.db.Db.TimeSlot;
import org.json.JSONObject;
import org.json.JSONArray;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.*;

// 学生视图接口：课表、成绩、课程推荐、成绩导出
// 导出成绩为CSV到 WebRoot/exports 目录，前端可直接下载
/**
 * 学生端综合接口 Servlet
 * - 职责：课表查询、成绩查询、成绩导出
 * - 支持 action：
 *   - schedule：我的课表
 *   - grades_query：查询成绩列表
 *   - grades_export：导出成绩为 CSV
 * - 会话与权限：需登录并从 Session 读取 `uid`
 * - 依赖：通过 `DaoFactory.grade()`、`DaoFactory.enrollment()`、`DaoFactory.course()` 访问数据层
 */
public class StudentServlet extends SimpleRestful {
    // 将课程对象转换为明确 JSON 结构
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
    /** 处理学生端请求
     * 支持 action：
     * - calendar：返回我的课表事件（title/day/start/end）
     * - grades：查询成绩列表（含未评分返回 null）
     * - set_grade：设定成绩（仅管理员可为他人设定）
     * - recommend：课程推荐（明确字段）
     * - grades_export：导出成绩为 CSV 文件
     * 会话：需登录（从 Session 读取 uid/role）
     * @param req HTTP 请求
     * @param resp HTTP 响应
     * @param jreq 请求体 JSON（含 action）
     * @return 根据 action 返回 JSON/数组/Map
     * @throws lwWebException 未登录/无权限/参数错误
     */
    protected Object execute(HttpServletRequest req, HttpServletResponse resp, JSONObject jreq) throws Exception {
        HttpSession s=req.getSession(false); if(s==null) throw new lwWebException(401, "未登录");
        String uid=(String)s.getAttribute("uid"); if(uid==null) throw new lwWebException(401, "未登录");
        String action=jreq!=null? jreq.optString("action", ""): "";
        switch(action){
            case "calendar": return DaoFactory.enrollment().calendar(uid); // 返回简化日历事件：title/day/start/end
            case "grades": {
                // 合并“我的选课”和“我的成绩”，即使未打分也返回课程名称，score 为空
                java.util.List<Enrollment> ens = DaoFactory.enrollment().listUserEnrollments(uid);
                java.util.List<com.training.db.Db.Grade> grades = DaoFactory.grade().getGrades(uid);
                java.util.Map<String, com.training.db.Db.Grade> gmap = new java.util.HashMap<>();
                for(com.training.db.Db.Grade g: grades){ gmap.put(g.courseId, g); }
                // 当前课程ID到名称的映射
                java.util.Map<String,String> id2name = new java.util.HashMap<>();
                for(Course c : DaoFactory.course().listCourses()){ id2name.put(c.id, c.name); }
                // 组合所有课程ID（选课 + 已有成绩）
                java.util.Set<String> ids = new java.util.HashSet<>();
                for(Enrollment e: ens){ if("enrolled".equals(e.status)) ids.add(e.courseId); }
                for(com.training.db.Db.Grade g: grades){ ids.add(g.courseId); }
                JSONArray arr = new JSONArray();
                for(String cid: ids){
                    JSONObject j = new JSONObject();
                    j.put("courseId", cid);
                    com.training.db.Db.Grade g = gmap.get(cid);
                    // 优先使用成绩中冗余课程名；否则用当前课程表映射；再回退为ID
                    String nm = (g!=null && g.courseName!=null)? g.courseName: id2name.getOrDefault(cid, cid);
                    j.put("name", nm);
                    // 统一返回 score 字段：已评分为具体分数；未评分为 null
                    j.put("score", g!=null ? g.score : org.json.JSONObject.NULL);
                    arr.put(j);
                }
                return arr;
            }
            case "set_grade": {
                // 设定成绩：默认为当前登录用户；若传 userId 需管理员权限
                String role=s!=null? (String)s.getAttribute("role"): null;
                String targetUserId = uid;
                if(jreq.has("userId")){
                    if(!"admin".equals(role)) throw new lwWebException(403, "仅管理员可为他人设定成绩");
                    targetUserId = jreq.getString("userId");
                }
                String courseId = jreq.getString("courseId");
                double score = jreq.getDouble("score");
                DaoFactory.grade().setGrade(targetUserId, courseId, score);
                return new JSONObject().put("ok", true);
            }
            case "recommend":{
                // 返回明确字段的推荐课程，避免前端出现 undefined
                java.util.List<Course> list=DaoFactory.course().recommend(uid);
                JSONArray arr=new JSONArray();
                for(Course c: list){ arr.put(toJson(c)); }
                return arr;
            }
            case "grades_export":{
                // 将成绩导出为CSV文件，并返回下载路径
                java.util.List<com.training.db.Db.Grade> list=DaoFactory.grade().getGrades(uid);
                StringBuilder sb=new StringBuilder(); sb.append("courseId,score\n");
                for(com.training.db.Db.Grade g: list){ sb.append(g.courseId).append(",").append(g.score).append("\n"); }
                String real=req.getServletContext().getRealPath("/exports");
                File dir=new File(real); if(!dir.exists()) dir.mkdirs();
                File f=new File(dir, "grades_"+uid+".csv");
                TextFile.write(f, sb.toString(), charset);
                Map<String,Object> r=new HashMap<>(); r.put("file", "/exports/"+f.getName()); return r;
            }
            default: throw new lwWebException(400, "未知action:"+action);
        }
    }
}