package com.training.dao.memory;

import java.util.List;
import com.training.dao.CourseDao;
import com.training.db.Db;
import com.training.db.Db.Course;
import com.training.db.Db.TimeSlot;

/**
 * CourseDao 的内存实现
 * - 职责：将课程增删改查、过滤与推荐委托至 `Db`
 * - 适用场景：教学演示、快速原型与单机调试
 * - 线程安全：依赖 `Db` 的内部数据结构与锁
 */
public class InMemoryCourseDao implements CourseDao {
    /** 新增课程（委托 Db.addCourse）
     * @param name 课程名
     * @param credit 学分
     * @param capacity 容量
     * @param times 上课时间段列表
     * @return 创建成功的课程对象
     */
    @Override public Course addCourse(String name, int credit, int capacity, List<TimeSlot> times){
        return Db.addCourse(name, credit, capacity, times);
    }
    /** 更新课程（委托 Db.updateCourse）
     * @param id 课程ID
     * @param name 课程名（null 表示不修改）
     * @param credit 学分（null 表示不修改）
     * @param capacity 容量（null 表示不修改）
     * @param times 上课时间段列表（null 表示不修改）
     * @return 是否成功
     */
    @Override public boolean updateCourse(String id, String name, Integer credit, Integer capacity, List<TimeSlot> times){
        return Db.updateCourse(id, name, credit, capacity, times);
    }
    /** 删除课程（委托 Db.deleteCourse）
     * @param id 课程ID
     * @return 是否成功
     */
    @Override public boolean deleteCourse(String id){
        return Db.deleteCourse(id);
    }
    /** 列出课程（委托 Db.listCourses）
     * @return 课程列表
     */
    @Override public List<Course> listCourses(){
        return Db.listCourses();
    }
    /** 过滤课程（委托 Db.filterCourses）
     * @param minCredit 最小学分（null 表示不限）
     * @param maxCredit 最大学分（null 表示不限）
     * @param day 上课日（如 MON/TUE；null 表示不限）
     * @return 过滤结果列表
     */
    @Override public List<Course> filterCourses(Integer minCredit, Integer maxCredit, String day){
        return Db.filterCourses(minCredit, maxCredit, day);
    }
    /** 推荐课程（委托 Db.recommend）
     * @param userId 用户ID
     * @return 推荐课程列表（排序稳定性由 Db 保证）
     */
    @Override public List<Course> recommend(String userId){
        return Db.recommend(userId);
    }
}