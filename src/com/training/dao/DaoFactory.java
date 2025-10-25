package com.training.dao;

import com.training.dao.memory.*;

/**
 * DAO 工厂
 * - 职责：提供各 DAO 的默认实现入口（单例），解耦 Web 层与存储实现
 * - 默认绑定：`memory` 包中的内存实现，全部委托 `Db` 静态方法
 * - 使用方式：`DaoFactory.user()/course()/enrollment()/grade()/admin()`
 * - 可替换性：如需切换到数据库实现（MySQL 等），替换内部绑定即可，无需改动上层代码
 */
public final class DaoFactory {
    private static final UserDao USER = new InMemoryUserDao();
    private static final CourseDao COURSE = new InMemoryCourseDao();
    private static final EnrollmentDao ENROLL = new InMemoryEnrollmentDao();
    private static final GradeDao GRADE = new InMemoryGradeDao();
    private static final AdminDao ADMIN = new InMemoryAdminDao();

    private DaoFactory(){}

    /** 获取用户DAO（默认内存实现，委托 Db） */
    public static UserDao user(){ return USER; }
    /** 获取课程DAO（默认内存实现，委托 Db） */
    public static CourseDao course(){ return COURSE; }
    /** 获取选课DAO（默认内存实现，委托 Db） */
    public static EnrollmentDao enrollment(){ return ENROLL; }
    /** 获取成绩DAO（默认内存实现，委托 Db） */
    public static GradeDao grade(){ return GRADE; }
    /** 获取管理员DAO（默认内存实现，委托 Db） */
    public static AdminDao admin(){ return ADMIN; }
}