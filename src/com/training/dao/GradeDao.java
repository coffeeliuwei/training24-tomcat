package com.training.dao;

import java.util.List;
import com.training.db.Db.Grade;

public interface GradeDao {
    void setGrade(String userId, String courseId, double score);
    List<Grade> getGrades(String userId);
}