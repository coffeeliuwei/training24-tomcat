package com.training.dao;

import java.util.List;
import com.training.db.Db.Enrollment;

public interface EnrollmentDao {
    Enrollment enroll(String userId, String courseId);
    boolean drop(String userId, String courseId);
    List<Enrollment> listUserEnrollments(String userId);
    // 可选：将选课映射为课表事件（简化格式）
    java.util.List<java.util.Map<String,Object>> calendar(String userId);
}