package com.training.db;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// 内存模拟数据库（教学用）：
// - 用于管理 用户、课程、选课、成绩、日志 等数据，全部存储在内存结构中（Map/List）。
// - 支持选课的容量限制、时间冲突检测、候补队列、退课转正、简单推荐与统计。
// - 线程安全：课程维度使用 ReentrantLock 保证并发操作一致性。
/**
 * 内存数据库（教学版）
 * - 职责：提供用户、课程、时间片、选课、成绩等实体与基础操作
 * - 并发与线程安全：通过 `ConcurrentHashMap` 等结构与课程级锁保证并发安全（选课/退课）
 * - 日志与统计：提供简单日志记录与系统统计，供管理员查询
 * - 推荐：根据课程被选“热度”与个人已选过滤进行推荐
 * - 种子数据：`seed()` 初始化示例用户/课程/选课/成绩，便于演示
 * - 说明：默认作为 DAO 层的内存实现后端；可替换为持久化实现时逐步迁移
 */
public final class Db {
    // 用户实体：包含账号、密码、角色（student/admin）与邮箱
    public static class User {
        public String id;
        public String username;
        public String password;
        public String role; // student/admin
        public String email;
        public User(String id,String username,String password,String role,String email){
            this.id=id;this.username=username;this.password=password;this.role=role;this.email=email;
        }
    }
    // 上课时间段：day 表示星期（Mon/Tue…），start/end 表示开始/结束小时（end 为排他）
    public static class TimeSlot {
        public String day; // Mon/Tue...
        public int start; // 8..20
        public int end;   // exclusive
        public String date; // 可选：具体日期，格式如 2025/10/25
        public TimeSlot(String day,int start,int end){this.day=day;this.start=start;this.end=end;this.date=null;}
    }
    // 课程实体：包含学分、容量、已选人数与时间安排
    public static class Course {
        public String id;
        public String name;
        public int credit;
        public List<TimeSlot> times=new ArrayList<>();
        public int capacity;
        public int enrolled;
        public Course(String id,String name,int credit,int capacity,List<TimeSlot> times){
            this.id=id;this.name=name;this.credit=credit;this.capacity=capacity;this.times=times;this.enrolled=0;
        }
    }
    // 选课记录：status 可为 enrolled（已选）或 waitlist（候补）或 conflict（冲突）
    public static class Enrollment {
        public String userId;
        public String courseId;
        public String status; // enrolled/waitlist
        public Enrollment(String userId,String courseId,String status){this.userId=userId;this.courseId=courseId;this.status=status;}
    }
    // 成绩记录：courseId 与分数，同时冗余存储课程名以应对课程删除场景
    public static class Grade {public String userId;public String courseId;public double score; public String courseName;}

    // 基础数据存储结构
    private static final Map<String,User> users=new ConcurrentHashMap<>();
    private static final Map<String,Course> courses=new ConcurrentHashMap<>();
    private static final Map<String,List<Enrollment>> enrollmentsByUser=new ConcurrentHashMap<>();
    private static final Map<String,List<String>> waitlistByCourse=new ConcurrentHashMap<>();
    private static final Map<String,List<Grade>> gradesByUser=new ConcurrentHashMap<>();
    private static final List<String> logs=Collections.synchronizedList(new ArrayList<>());
    private static final Map<String,ReentrantLock> courseLocks=new ConcurrentHashMap<>();

    private static String uuid(){return UUID.randomUUID().toString();}

    // 简单日志记录：便于管理员查看系统操作
    /** 记录系统操作日志（附时间戳）
     * @param text 日志文本，例如 "enroll:uid:courseId"
     */
    public static void log(String text){
        logs.add(new Date()+" | "+text);
    }
    /** 返回当前日志列表的副本
     * @return 日志字符串列表（不可修改原始内部集合）
     */
    public static java.util.List<String> getLogs(){
        return new ArrayList<>(logs);
    }

    // 用户相关操作
    /** 新增用户
     * @param username 用户名
     * @param password 密码（教学示例明文，不做复杂校验）
     * @param role 角色：student/admin
     * @param email 邮箱
     * @return 创建的用户对象
     * 边界与异常：
     * - 未强制用户名唯一；存在重复时以第一个匹配为准（findUserByName）
     * - 不校验邮箱格式与角色合法性（由上层调用方负责）
     */
    public static User addUser(String username,String password,String role,String email){
        String id=uuid(); User u=new User(id,username,password,role,email); users.put(id,u); log("addUser:"+username); return u;
    }
    /** 按用户名查找用户
     * @param username 用户名（区分大小写）
     * @return 匹配用户或 null
     * 边界：若存在重名用户，返回内部 Map 遍历遇到的第一个
     */
    public static User findUserByName(String username){
        for(User u: users.values()) if(u.username.equals(username)) return u; return null;
    }
    /** 重置用户密码
     * @param username 用户名
     * @param newPwd 新密码
     * @return 是否成功（用户存在）
     * 边界：不校验密码复杂度；不存在用户返回 false
     */
    public static boolean resetPassword(String username,String newPwd){
        User u=findUserByName(username); if(u==null) return false; u.password=newPwd; log("resetPassword:"+username); return true;
    }
    /** 用户认证
     * @param username 用户名
     * @param password 密码
     * @return 验证成功返回用户，否则 null
     * 边界：不做登录失败计数与锁定；大小写敏感；未强制唯一用户名时取第一个匹配
     */
    public static User auth(String username,String password){
        User u=findUserByName(username); if(u!=null && u.password.equals(password)) return u; return null;
    }

    // 课程相关操作
    /** 新增课程
     * @param name 课程名
     * @param credit 学分
     * @param capacity 容量
     * @param times 上课时间片
     * @return 创建的课程
     * 边界：不校验学分/容量取值范围；不校验时间片重叠与合法性（由选课冲突检测处理）
     */
    public static Course addCourse(String name,int credit,int capacity,List<TimeSlot> times){
        String id=uuid(); Course c=new Course(id,name,credit,capacity,times); courses.put(id,c); courseLocks.put(id,new ReentrantLock()); log("addCourse:"+name); return c;
    }
    /** 更新课程（字段为 null 表示不修改）
     * @param id 课程ID
     * @param name 课程名（可选）
     * @param credit 学分（可选）
     * @param capacity 容量（可选）
     * @param times 时间片列表（可选）
     * @return 是否更新成功
     * 边界：不校验负数/过大数值；更新时间片可能导致后续选课冲突，但本方法不进行校验
     */
    public static boolean updateCourse(String id,String name,Integer credit,Integer capacity,List<TimeSlot> times){
        Course c=courses.get(id); if(c==null) return false; if(name!=null) c.name=name; if(credit!=null) c.credit=credit; if(capacity!=null) c.capacity=capacity; if(times!=null) c.times=times; log("updateCourse:"+id); return true;
    }
    /** 删除课程
     * @param id 课程ID
     * @return 是否删除成功
     * 边界：级联清理候补队列、课程锁、用户选课与成绩中与该课程相关的数据
     */
    public static boolean deleteCourse(String id){
        ReentrantLock lock = courseLocks.get(id);
        if(lock != null) lock.lock();
        try {
            Course c=courses.remove(id); if(c==null) return false;
            // 清理候补队列
            waitlistByCourse.remove(id);
            // 清理用户选课记录（所有用户）
            for(Map.Entry<String,List<Enrollment>> entry : enrollmentsByUser.entrySet()){
                List<Enrollment> es = entry.getValue();
                Iterator<Enrollment> it = es.iterator();
                while(it.hasNext()){
                    Enrollment e = it.next();
                    if(id.equals(e.courseId)) it.remove();
                }
            }
            // 清理成绩记录（所有用户）
            for(Map.Entry<String,List<Grade>> entry : gradesByUser.entrySet()){
                List<Grade> gs = entry.getValue();
                Iterator<Grade> it = gs.iterator();
                while(it.hasNext()){
                    Grade g = it.next();
                    if(id.equals(g.courseId)) it.remove();
                }
            }
            log("deleteCourse:"+id);
            return true;
        } finally {
            if(lock != null) lock.unlock();
            courseLocks.remove(id);
        }
    }
    /** 列出所有课程
     * @return 课程列表快照
     */
    public static List<Course> listCourses(){return new ArrayList<>(courses.values());}
    /** 按条件过滤课程
     * @param minCredit 最小学分（可选，null 表示不限）
     * @param maxCredit 最大学分（可选，null 表示不限）
     * @param day 星期（如 Mon/Tue…，可选，大小写敏感，需与 TimeSlot.day 完全匹配）
     * @return 满足条件的课程列表（约束互相矛盾时返回空列表，不抛异常）
     */
    public static List<Course> filterCourses(Integer minCredit,Integer maxCredit,String day){
        List<Course> out=new ArrayList<>();
        for(Course c: courses.values()){
            boolean ok=true;
            if(minCredit!=null && c.credit<minCredit) ok=false;
            if(maxCredit!=null && c.credit>maxCredit) ok=false;
            if(day!=null){ boolean has=false; for(TimeSlot t: c.times){ if(t.day.equals(day)) { has=true; break; } } if(!has) ok=false; }
            if(ok) out.add(c);
        }
        return out;
    }

    // 冲突检测：判断用户已有课程与新课程是否存在时间重叠
    private static boolean conflict(String userId, Course newCourse){
        List<Enrollment> list=enrollmentsByUser.getOrDefault(userId, new ArrayList<>());
        for(Enrollment e: list){ if(!"enrolled".equals(e.status)) continue; Course c=courses.get(e.courseId); if(c==null) continue; for(TimeSlot t1:c.times) for(TimeSlot t2:newCourse.times){ if(t1.day.equals(t2.day) && !(t1.end<=t2.start || t2.end<=t1.start)) return true; } }
        return false;
    }
    // 选课：
    /** 学生选课（并发安全）
     * - 冲突返回 status=conflict；课程不存在返回 null
     * - 未满直接 enrolled；满员加入候补队列 waitlist（FIFO）
     * - 若已存在该课程的选课/候补记录，直接返回该记录（避免重复）
     * @param userId 学生ID（未校验用户是否存在）
     * @param courseId 课程ID
     * @return 选课记录（enrolled/waitlist/conflict），课程不存在返回 null
     * 边界与并发：
     * - 不去重：重复选课会产生多条记录并占容量；应用层需避免重复请求
     * - 课程级锁保证 enrolled/候补与转正的一致性
     */
    public static Enrollment enroll(String userId,String courseId){
        // 避免重复：若用户已有该课程的记录，直接返回
        List<Enrollment> existing = enrollmentsByUser.getOrDefault(userId, new ArrayList<>());
        for(Enrollment e: existing){ if(e.courseId.equals(courseId)) { log("duplicate_enroll:"+userId+":"+courseId); return e; } }
        Course c=courses.get(courseId); if(c==null) return null; courseLocks.putIfAbsent(courseId,new ReentrantLock()); ReentrantLock lock=courseLocks.get(courseId); lock.lock(); try{
            if(conflict(userId,c)) { log("conflict:"+userId+":"+courseId); return new Enrollment(userId,courseId,"conflict"); }
            if(c.enrolled<c.capacity){ c.enrolled++; Enrollment e=new Enrollment(userId,courseId,"enrolled"); enrollmentsByUser.computeIfAbsent(userId,k->new ArrayList<>()).add(e); log("enroll:"+userId+":"+courseId); return e; }
            List<String> wl=waitlistByCourse.computeIfAbsent(courseId,k->new ArrayList<>()); wl.add(userId); Enrollment e=new Enrollment(userId,courseId,"waitlist"); enrollmentsByUser.computeIfAbsent(userId,k->new ArrayList<>()).add(e); log("waitlist:"+userId+":"+courseId); return e;
        } finally { lock.unlock(); }
    }
    // 退课：
    /** 学生退课（并发安全）
     * - 退课成功且存在候补队列时，自动将队首转为已选（仅当退掉的是已选记录）
     * @param userId 学生ID
     * @param courseId 课程ID
     * @return 是否成功退课
     * 边界与并发：
     * - 若用户存在该课程的多条记录，只删除第一条匹配
     * - 仅当删除的是已选记录时减少课程已选数与触发候补转正
     */
    public static boolean drop(String userId,String courseId){
        List<Enrollment> list=enrollmentsByUser.getOrDefault(userId, new ArrayList<>()); boolean removed=false; String removedStatus=null; Iterator<Enrollment> it=list.iterator(); while(it.hasNext()){Enrollment e=it.next(); if(e.courseId.equals(courseId)){removed=true; removedStatus=e.status; it.remove(); break;}}
        Course c=courses.get(courseId); if(c!=null && removed){ courseLocks.putIfAbsent(courseId,new ReentrantLock()); ReentrantLock lock=courseLocks.get(courseId); lock.lock(); try{ if("enrolled".equals(removedStatus) && c.enrolled>0) c.enrolled--; if("enrolled".equals(removedStatus)){ List<String> wl=waitlistByCourse.getOrDefault(courseId,new ArrayList<>()); if(!wl.isEmpty()){ String next=wl.remove(0); List<Enrollment> en=enrollmentsByUser.getOrDefault(next,new ArrayList<>()); for(Enrollment e: en){ if(e.courseId.equals(courseId) && "waitlist".equals(e.status)){ e.status="enrolled"; c.enrolled++; log("promote:"+next+":"+courseId); break; } } } } } finally { lock.unlock(); } }
        return removed;
    }
    /** 我的选课列表
     * @param userId 学生ID
     * @return 该用户的选课记录列表
     */
    public static List<Enrollment> listUserEnrollments(String userId){ return new ArrayList<>(enrollmentsByUser.getOrDefault(userId,new ArrayList<>())); }

    // 课表：将选课转换为日历事件（简化格式）
    /** 课表事件生成
     * 将用户的已选课程映射为简化的日历事件
     * @param userId 学生ID
     * @return 事件列表（title/day/start/end）；仅包含 status=enrolled 的记录
     * 边界：同一课程的多个时间片会生成多条事件；重复选课会导致重复事件
     */
    public static List<Map<String,Object>> calendar(String userId){
        List<Map<String,Object>> events=new ArrayList<>();
        for(Enrollment e: listUserEnrollments(userId)){ if(!"enrolled".equals(e.status)) continue; Course c=courses.get(e.courseId); if(c==null) continue; for(TimeSlot t: c.times){ Map<String,Object> ev=new HashMap<>(); ev.put("title", c.name); ev.put("day", t.day); ev.put("start", t.start); ev.put("end", t.end); events.add(ev); } }
        return events;
    }

    // 成绩：在写入成绩时冗余记录课程名
    /** 设置成绩（冗余记录课程名）
     * @param userId 学生ID
     * @param courseId 课程ID
     * @param score 分数（越界将截断到 0-100）
     */
    public static void setGrade(String userId,String courseId,double score){
        double s = score; if(s < 0) s = 0; if(s > 100) s = 100;
        Grade g=new Grade(); g.userId=userId; g.courseId=courseId; g.score=s; Course c=courses.get(courseId); g.courseName=(c!=null? c.name: null); gradesByUser.computeIfAbsent(userId,k->new ArrayList<>()).add(g); log("grade:"+userId+":"+courseId+":"+s);
    }
    /** 查询学生成绩
     * @param userId 学生ID
     * @return 成绩列表（可能为空）
     * 边界：返回列表为当前快照；未保证排序稳定性
     */
    public static List<Grade> getGrades(String userId){ return new ArrayList<>(gradesByUser.getOrDefault(userId,new ArrayList<>())); }

    // 简单推荐：按照课程被选次数（热度）排序，过滤掉已经选过的课程
    /** 课程推荐
     * 排除该用户已选与候补课程，按课程“热度”（被选次数）降序；热度相同按课程名升序、ID 升序稳定化
     * @param userId 学生ID
     * @return 推荐课程列表
     */
    public static List<Course> recommend(String userId){
        Map<String,Integer> popularity=new HashMap<>(); for(List<Enrollment> list: enrollmentsByUser.values()) for(Enrollment e:list){ popularity.put(e.courseId, popularity.getOrDefault(e.courseId,0)+1); }
        Set<String> taken=new HashSet<>(); for(Enrollment e: enrollmentsByUser.getOrDefault(userId,new ArrayList<>())) taken.add(e.courseId);
        List<Course> out=new ArrayList<>(); for(Course c: courses.values()) if(!taken.contains(c.id)) out.add(c);
        out.sort((a,b)->{
            int pb = popularity.getOrDefault(b.id,0);
            int pa = popularity.getOrDefault(a.id,0);
            int cmp = Integer.compare(pb, pa);
            if(cmp != 0) return cmp;
            cmp = a.name.compareTo(b.name);
            if(cmp != 0) return cmp;
            return a.id.compareTo(b.id);
        });
        return out;
    }

    // 系统统计：用户数量、课程数量、选课总数
    /** 系统统计
     * @return Map：users/courses/enrollments
     * 边界：选课总数包含已选与候补记录；不区分状态
     */
    public static Map<String,Object> stats(){
        Map<String,Object> s=new HashMap<>(); s.put("users", users.size()); s.put("courses", courses.size()); int totalEnroll=0; for(List<Enrollment> l: enrollmentsByUser.values()) totalEnroll+=l.size(); s.put("enrollments", totalEnroll); return s;
    }

    // 初始化示例数据：默认创建管理员与两个学生，以及两门课程
    /** 初始化示例数据
     * 创建基础用户/课程并做部分选课与成绩
     * 边界：仅在用户表为空时执行，避免重复注入数据
     */
    public static void seed(){
        if(users.isEmpty()){
            addUser("admin","123456","admin","admin@example.com"); User u1=addUser("alice","123456","student","alice@example.com"); User u2=addUser("bob","123456","student","bob@example.com"); List<TimeSlot> t1=Arrays.asList(new TimeSlot("Mon",10,12),new TimeSlot("Wed",10,12)); List<TimeSlot> t2=Arrays.asList(new TimeSlot("Tue",14,16),new TimeSlot("Thu",14,16)); Course c1=addCourse("数据结构",3,2,t1); Course c2=addCourse("数据库原理",4,2,t2); enroll(u1.id,c1.id); enroll(u2.id,c1.id); enroll(u1.id,c2.id); setGrade(u1.id,c1.id,88.0);
        }
    }
}