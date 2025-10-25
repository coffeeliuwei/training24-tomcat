package com.training.dao.memory;

import java.util.List;
import java.util.Map;
import com.training.dao.AdminDao;
import com.training.db.Db;

/**
 * AdminDao 的内存实现
 * - 职责：将统计/日志/数据初始化委托至 `Db`
 * - 适用场景：教学演示、快速原型与单机调试
 * - 线程安全：依赖 `Db` 的内部并发控制
 */
public class InMemoryAdminDao implements AdminDao {
    /** 系统统计（委托 Db.stats）
     * @return Map：users/courses/enrollments
     */
    @Override public java.util.Map<String,Object> stats(){
        return Db.stats();
    }
    /** 记录系统操作日志（委托 Db.log）
     * @param text 日志文本
     */
    @Override public void log(String text){
        Db.log(text);
    }
    /** 查询日志列表（委托 Db.getLogs）
     * @return 日志字符串列表
     */
    @Override public java.util.List<String> getLogs(){
        return Db.getLogs();
    }
    /** 初始化示例数据（委托 Db.seed） */
    @Override public void seed(){
        Db.seed();
    }
}