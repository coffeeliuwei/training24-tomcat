package com.training.dao.memory;

import java.util.List;
import java.util.Map;
import com.training.dao.EnrollmentDao;
import com.training.db.Db;
import com.training.db.Db.Enrollment;

public class InMemoryEnrollmentDao implements EnrollmentDao {
    @Override public Enrollment enroll(String userId, String courseId){
        return Db.enroll(userId, courseId);
    }
    @Override public boolean drop(String userId, String courseId){
        return Db.drop(userId, courseId);
    }
    @Override public List<Enrollment> listUserEnrollments(String userId){
        return Db.listUserEnrollments(userId);
    }
    @Override public java.util.List<Map<String,Object>> calendar(String userId){
        return Db.calendar(userId);
    }
}