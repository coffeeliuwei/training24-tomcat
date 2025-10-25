package com.training.dao;

import java.util.List;
import com.training.db.Db.Grade;

/**
 * 成绩数据访问接口
 * - 职责：设置与查询学生成绩
 * - 典型方法：
 *   - setGrade(userId, courseId, score)：设置分数
 *   - getGrades(userId)：查询某学生的全部成绩（可能为空）
 * - 说明：Web 层通过 `DaoFactory.grade()` 获取实现，默认委托内存 `Db`
 */
public interface GradeDao {
    /** 设置成绩
     * @param userId 学生ID
     * @param courseId 课程ID
     * @param score 分数
     */
    void setGrade(String userId, String courseId, double score);
    /** 查询学生成绩
     * @param userId 学生ID
     * @return 成绩列表（可能为空）
     */
    java.util.List<com.training.db.Db.Grade> getGrades(String userId);
}