package com.training.dao;

import java.util.List;
import java.util.Map;

/**
 * 管理员数据访问接口
 * - 职责：系统统计、日志记录/查询、示例数据初始化
 * - 典型方法：
 *   - stats()：返回用户数/课程数/选课总数等统计
 *   - log(text)：记录系统操作日志
 *   - getLogs()：查询日志（简单字符串列表）
 *   - seed()：初始化示例数据（用户/课程/选课/成绩）
 * - 说明：Web 层通过 `DaoFactory.admin()` 获取实现，默认委托内存 `Db`
 */
public interface AdminDao {
    /** 系统统计
     * @return Map：users/courses/enrollments
     */
    java.util.Map<String,Object> stats();
    /** 记录系统操作日志
     * @param text 日志文本
     */
    void log(String text);
    /** 查询日志列表
     * @return 日志字符串列表
     */
    java.util.List<String> getLogs();
    /** 初始化示例数据（用户/课程/选课/成绩） */
    void seed();
}