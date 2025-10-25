package com.training.dao.memory;

import com.training.dao.UserDao;
import com.training.db.Db;
import com.training.db.Db.User;

/**
 * UserDao 的内存实现
 * - 职责：将用户相关操作委托至 `Db` 静态方法
 * - 适用场景：教学演示、快速原型与单机调试
 * - 线程安全：依赖 `Db` 的内部并发控制
 */
public class InMemoryUserDao implements UserDao {
    /** 新增用户（委托 Db.addUser）
     * @param username 用户名（唯一）
     * @param password 明文或简单加密密码
     * @param role 角色（student/teacher/admin）
     * @param email 邮箱
     * @return 创建成功的用户对象
     */
    @Override public User addUser(String username, String password, String role, String email){
        return Db.addUser(username, password, role, email);
    }
    /** 按用户名查找用户（委托 Db.findUserByName）
     * @param username 用户名
     * @return 用户对象或 null（不存在）
     */
    @Override public User findByName(String username){
        return Db.findUserByName(username);
    }
    /** 重置用户密码（委托 Db.resetPassword）
     * @param username 用户名
     * @param newPwd 新密码
     * @return 是否成功
     */
    @Override public boolean resetPassword(String username, String newPwd){
        return Db.resetPassword(username, newPwd);
    }
    /** 用户认证（委托 Db.auth）
     * @param username 用户名
     * @param password 密码
     * @return 认证通过的用户对象，失败返回 null
     */
    @Override public User auth(String username, String password){
        return Db.auth(username, password);
    }
}