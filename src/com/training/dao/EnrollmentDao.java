package com.training.dao;

import java.util.List;
import com.training.db.Db.Enrollment;

/**
 * 选课数据访问接口
 * - 职责：发起选课、退课、查询我的选课、课表映射
 * - 典型方法：
 *   - enroll(userId, courseId)：选课（返回 enrolled/waitlist/conflict）
 *   - drop(userId, courseId)：退课并可能触发候补转正
 *   - listUserEnrollments(userId)：我的选课记录
 *   - calendar(userId)：将选课映射为课表事件（title/day/start/end）
 * - 说明：Web 层通过 `DaoFactory.enrollment()` 获取实现，默认委托内存 `Db`
 */
public interface EnrollmentDao {
    /** 选课
     * @param userId 学生ID
     * @param courseId 课程ID
     * @return Enrollment（status=enrolled/waitlist/conflict）；课程不存在返回 null
     */
    Enrollment enroll(String userId, String courseId);
    /** 退课
     * @param userId 学生ID
     * @param courseId 课程ID
     * @return 是否成功；可能触发候补转正
     */
    boolean drop(String userId, String courseId);
    /** 我的选课记录
     * @param userId 学生ID
     * @return 选课记录列表
     */
    java.util.List<Enrollment> listUserEnrollments(String userId);
    // 可选：将选课映射为课表事件（简化格式）
    /** 课表事件列表
     * 将选课映射为简化事件
     * @param userId 学生ID
     * @return 事件列表：title/day/start/end
     */
    java.util.List<java.util.Map<String,Object>> calendar(String userId);
}