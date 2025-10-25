package com.training.dao;

import com.training.dao.memory.*;

public final class DaoFactory {
    private static final UserDao USER = new InMemoryUserDao();
    private static final CourseDao COURSE = new InMemoryCourseDao();
    private static final EnrollmentDao ENROLL = new InMemoryEnrollmentDao();
    private static final GradeDao GRADE = new InMemoryGradeDao();
    private static final AdminDao ADMIN = new InMemoryAdminDao();

    private DaoFactory(){}

    public static UserDao user(){ return USER; }
    public static CourseDao course(){ return COURSE; }
    public static EnrollmentDao enrollment(){ return ENROLL; }
    public static GradeDao grade(){ return GRADE; }
    public static AdminDao admin(){ return ADMIN; }
}