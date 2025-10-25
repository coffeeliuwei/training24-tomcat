package com.training.db;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

// 内存模拟数据库（教学用）：
// - 用于管理 用户、课程、选课、成绩、日志 等数据，全部存储在内存结构中（Map/List）。
// - 支持选课的容量限制、时间冲突检测、候补队列、退课转正、简单推荐与统计。
// - 线程安全：课程维度使用 ReentrantLock 保证并发操作一致性。
public class Db {
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
    public static void log(String text){
        logs.add(new Date()+" | "+text);
    }
    public static List<String> getLogs(){return new ArrayList<>(logs);}    

    // 用户相关操作
    public static User addUser(String username,String password,String role,String email){
        String id=uuid();
        User u=new User(id,username,password,role,email);
        users.put(id,u); log("addUser:"+username);
        return u;
    }
    public static User findUserByName(String username){
        for(User u: users.values()) if(u.username.equals(username)) return u; return null;
    }
    public static boolean resetPassword(String username,String newPwd){
        User u=findUserByName(username); if(u==null) return false; u.password=newPwd; log("resetPassword:"+username); return true;
    }
    public static User auth(String username,String password){
        User u=findUserByName(username); if(u!=null && u.password.equals(password)) return u; return null;
    }

    // 课程相关操作
    public static Course addCourse(String name,int credit,int capacity,List<TimeSlot> times){
        String id=uuid(); Course c=new Course(id,name,credit,capacity,times); courses.put(id,c); courseLocks.put(id,new ReentrantLock()); log("addCourse:"+name); return c;
    }
    public static boolean updateCourse(String id,String name,Integer credit,Integer capacity,List<TimeSlot> times){
        Course c=courses.get(id); if(c==null) return false; if(name!=null) c.name=name; if(credit!=null) c.credit=credit; if(capacity!=null) c.capacity=capacity; if(times!=null) c.times=times; log("updateCourse:"+id); return true;
    }
    public static boolean deleteCourse(String id){Course c=courses.remove(id); if(c==null) return false; log("deleteCourse:"+id); return true;}
    public static List<Course> listCourses(){return new ArrayList<>(courses.values());}
    public static List<Course> filterCourses(Integer minCredit,Integer maxCredit,String day){
        // 课程过滤：按学分区间与星期筛选
        List<Course> out=new ArrayList<>();
        for(Course c: courses.values()){
            boolean ok=true;
            if(minCredit!=null && c.credit<minCredit) ok=false;
            if(maxCredit!=null && c.credit>maxCredit) ok=false;
            if(day!=null){boolean has=false; for(TimeSlot t: c.times){if(t.day.equals(day)) {has=true;break;}} if(!has) ok=false;}
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
    // - 若冲突则返回 status=conflict
    // - 若未满员则直接选课（enrolled），并增加课程的已选人数
    // - 若已满员则加入候补队列（waitlist），等待他人退课后被自动转正
    public static Enrollment enroll(String userId,String courseId){
        Course c=courses.get(courseId); if(c==null) return null; courseLocks.putIfAbsent(courseId,new ReentrantLock()); ReentrantLock lock=courseLocks.get(courseId); lock.lock(); try{
            if(conflict(userId,c)) {log("conflict:"+userId+":"+courseId); return new Enrollment(userId,courseId,"conflict");}
            if(c.enrolled<c.capacity){ c.enrolled++; Enrollment e=new Enrollment(userId,courseId,"enrolled"); enrollmentsByUser.computeIfAbsent(userId,k->new ArrayList<>()).add(e); log("enroll:"+userId+":"+courseId); return e; }
            // 已满：加入候补队列
            List<String> wl=waitlistByCourse.computeIfAbsent(courseId,k->new ArrayList<>()); wl.add(userId); Enrollment e=new Enrollment(userId,courseId,"waitlist"); enrollmentsByUser.computeIfAbsent(userId,k->new ArrayList<>()).add(e); log("waitlist:"+userId+":"+courseId); return e;
        } finally {lock.unlock();}
    }
    // 退课：
    // - 若成功退课并存在候补队列，则自动将队首同学转为已选（同时增加已选人数）
    public static boolean drop(String userId,String courseId){
        List<Enrollment> list=enrollmentsByUser.getOrDefault(userId, new ArrayList<>()); boolean removed=false; Iterator<Enrollment> it=list.iterator(); while(it.hasNext()){Enrollment e=it.next(); if(e.courseId.equals(courseId)){removed=true; it.remove(); break;}}
        Course c=courses.get(courseId); if(c!=null && removed){courseLocks.get(courseId).lock(); try{ if(c.enrolled>0) c.enrolled--; // promote first from waitlist
            List<String> wl=waitlistByCourse.getOrDefault(courseId,new ArrayList<>()); if(!wl.isEmpty()){ String next=wl.remove(0); // update that user's enrollment status
                List<Enrollment> en=enrollmentsByUser.getOrDefault(next,new ArrayList<>()); for(Enrollment e: en){ if(e.courseId.equals(courseId) && "waitlist".equals(e.status)){ e.status="enrolled"; c.enrolled++; log("promote:"+next+":"+courseId); break; } }
            }
        } finally {courseLocks.get(courseId).unlock();}}
        return removed;
    }
    // 我的选课列表
    public static List<Enrollment> listUserEnrollments(String userId){ return new ArrayList<>(enrollmentsByUser.getOrDefault(userId,new ArrayList<>())); }

    // 课表：将选课转换为日历事件（简化格式）
    public static List<Map<String,Object>> calendar(String userId){
        List<Map<String,Object>> events=new ArrayList<>();
        for(Enrollment e: listUserEnrollments(userId)){ if(!"enrolled".equals(e.status)) continue; Course c=courses.get(e.courseId); if(c==null) continue; for(TimeSlot t: c.times){ Map<String,Object> ev=new HashMap<>(); ev.put("title", c.name); ev.put("day", t.day); ev.put("start", t.start); ev.put("end", t.end); events.add(ev);} }
        return events;
    }

    // 成绩：在写入成绩时冗余记录课程名
    public static void setGrade(String userId,String courseId,double score){ Grade g=new Grade(); g.userId=userId; g.courseId=courseId; g.score=score; Course c=courses.get(courseId); g.courseName = (c!=null? c.name: null); gradesByUser.computeIfAbsent(userId,k->new ArrayList<>()).add(g); log("grade:"+userId+":"+courseId+":"+score); }
    public static List<Grade> getGrades(String userId){ return new ArrayList<>(gradesByUser.getOrDefault(userId,new ArrayList<>())); }

    // 简单推荐：按照课程被选次数（热度）排序，过滤掉已经选过的课程
    public static List<Course> recommend(String userId){
        Map<String,Integer> popularity=new HashMap<>();
        for(List<Enrollment> list: enrollmentsByUser.values()) for(Enrollment e:list){ popularity.put(e.courseId, popularity.getOrDefault(e.courseId,0)+1);}        
        Set<String> taken=new HashSet<>(); for(Enrollment e: enrollmentsByUser.getOrDefault(userId,new ArrayList<>())) taken.add(e.courseId);
        List<Course> out=new ArrayList<>();
        for(Course c: courses.values()) if(!taken.contains(c.id)) out.add(c);
        out.sort((a,b)-> popularity.getOrDefault(b.id,0)-popularity.getOrDefault(a.id,0));
        return out;
    }

    // 系统统计：用户数量、课程数量、选课总数
    public static Map<String,Object> stats(){ Map<String,Object> s=new HashMap<>(); s.put("users", users.size()); s.put("courses", courses.size()); int totalEnroll=0; for(List<Enrollment> l: enrollmentsByUser.values()) totalEnroll+=l.size(); s.put("enrollments", totalEnroll); return s; }

    // 初始化示例数据：默认创建管理员与两个学生，以及两门课程
    public static void seed(){
        if(users.isEmpty()){
            addUser("admin","123456","admin","admin@example.com");
            User u1=addUser("alice","123456","student","alice@example.com");
            User u2=addUser("bob","123456","student","bob@example.com");
            List<TimeSlot> t1=Arrays.asList(new TimeSlot("Mon",10,12),new TimeSlot("Wed",10,12));
            List<TimeSlot> t2=Arrays.asList(new TimeSlot("Tue",14,16),new TimeSlot("Thu",14,16));
            Course c1=addCourse("数据结构",3,2,t1);
            Course c2=addCourse("数据库原理",4,2,t2);
            enroll(u1.id,c1.id); enroll(u2.id,c1.id); // fill
            enroll(u1.id,c2.id);
            setGrade(u1.id,c1.id,88.0);
        }
    }
}