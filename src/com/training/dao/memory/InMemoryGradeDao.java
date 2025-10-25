package com.training.dao.memory;

import java.util.List;
import com.training.dao.GradeDao;
import com.training.db.Db;
import com.training.db.Db.Grade;

public class InMemoryGradeDao implements GradeDao {
    @Override public void setGrade(String userId, String courseId, double score){
        Db.setGrade(userId, courseId, score);
    }
    @Override public List<Grade> getGrades(String userId){
        return Db.getGrades(userId);
    }
}