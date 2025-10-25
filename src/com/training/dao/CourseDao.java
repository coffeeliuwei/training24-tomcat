package com.training.dao;

import java.util.List;
import com.training.db.Db.Course;
import com.training.db.Db.TimeSlot;

/**
 * 课程数据访问接口
 * - 职责：课程的增删改查、过滤与推荐
 * - 典型方法：
 *   - addCourse(name, credit, capacity, times)：新增课程
 *   - updateCourse(id, ...)：更新课程支持部分字段
 *   - deleteCourse(id)：删除课程
 *   - listCourses()：课程列表
 *   - filterCourses(minCredit?, maxCredit?, day?)：按学分范围/星期过滤
 *   - recommend(userId)：根据选课热度与个人已选做推荐
 * - 说明：Web 层通过 `DaoFactory.course()` 获取实现，默认委托内存 `Db`
 */
public interface CourseDao {
    /** 新增课程
     * @param name 课程名
     * @param credit 学分
     * @param capacity 容量
     * @param times 上课时间片列表
     * @return 创建的课程
     */
    Course addCourse(String name, int credit, int capacity, java.util.List<TimeSlot> times);
    /** 更新课程（字段为 null 表示不修改）
     * @param id 课程ID
     * @param name 课程名（可选）
     * @param credit 学分（可选）
     * @param capacity 容量（可选）
     * @param times 时间片列表（可选）
     * @return 是否更新成功
     */
    boolean updateCourse(String id, String name, Integer credit, Integer capacity, java.util.List<TimeSlot> times);
    /** 删除课程
     * @param id 课程ID
     * @return 是否删除成功
     */
    boolean deleteCourse(String id);
    /** 列出全部课程
     * @return 课程列表
     */
    java.util.List<Course> listCourses();
    /** 按条件过滤课程
     * @param minCredit 最小学分（可选）
     * @param maxCredit 最大学分（可选）
     * @param day 星期（Mon/Tue…，可选）
     * @return 满足条件的课程列表
     */
    java.util.List<Course> filterCourses(Integer minCredit, Integer maxCredit, String day);
    /** 推荐课程
     * 排除已选，按“热度”降序
     * @param userId 学生ID
     * @return 推荐课程列表
     */
    java.util.List<Course> recommend(String userId);
}