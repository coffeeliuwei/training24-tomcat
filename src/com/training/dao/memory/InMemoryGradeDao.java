package com.training.dao.memory;

import java.util.List;
import com.training.dao.GradeDao;
import com.training.db.Db;
import com.training.db.Db.Grade;

/**
 * GradeDao 的内存实现
 * - 职责：将成绩读写委托至 `Db`
 * - 适用场景：教学演示、快速原型与单机调试
 * - 线程安全：依赖 `Db` 的内部并发控制
 */
public class InMemoryGradeDao implements GradeDao {
    /** 设置成绩（委托 Db.setGrade）
     * @param userId 用户ID
     * @param courseId 课程ID
     * @param score 分数（0-100）
     */
    @Override public void setGrade(String userId, String courseId, double score){
        Db.setGrade(userId, courseId, score);
    }
    /** 查询成绩列表（委托 Db.getGrades）
     * @param userId 用户ID
     * @return 成绩列表（courseId/score）
     */
    @Override public java.util.List<Grade> getGrades(String userId){
        return Db.getGrades(userId);
    }
}