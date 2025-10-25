package com.training.dao.memory;

import java.util.List;
import java.util.Map;
import com.training.dao.EnrollmentDao;
import com.training.db.Db;
import com.training.db.Db.Enrollment;

/**
 * EnrollmentDao 的内存实现
 * - 职责：将选课/退课/我的选课/课表映射委托至 `Db`
 * - 适用场景：教学演示、快速原型与单机调试
 * - 线程安全：依赖 `Db` 的课程级锁与内部并发控制
 */
public class InMemoryEnrollmentDao implements EnrollmentDao {
    /** 选课（委托 Db.enroll）
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 选课记录（包含状态：成功/候补/已在候补）
     */
    @Override public Enrollment enroll(String userId, String courseId){
        return Db.enroll(userId, courseId);
    }
    /** 退课（委托 Db.drop）
     * @param userId 用户ID
     * @param courseId 课程ID
     * @return 是否成功（若触发候补转正由 Db 保证并发安全）
     */
    @Override public boolean drop(String userId, String courseId){
        return Db.drop(userId, courseId);
    }
    /** 我的选课列表（委托 Db.listUserEnrollments）
     * @param userId 用户ID
     * @return 选课记录列表
     */
    @Override public List<Enrollment> listUserEnrollments(String userId){
        return Db.listUserEnrollments(userId);
    }
    /** 我的课表（委托 Db.calendar）
     * @param userId 用户ID
     * @return 事件列表（title/day/start/end）
     */
    @Override public java.util.List<Map<String,Object>> calendar(String userId){
        return Db.calendar(userId);
    }
}