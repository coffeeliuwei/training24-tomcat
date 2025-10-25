package com.training.dao;

import com.training.db.Db.User;

/**
 * 用户数据访问接口
 * - 职责：用户的基础增删改查与认证
 * - 典型方法：
 *   - addUser(username, password, role, email)：新增用户
 *   - findByName(username)：按用户名查询用户
 *   - resetPassword(username, newPwd)：重置密码
 *   - auth(username, password)：认证登录并返回用户
 * - 说明：Web 层通过 `DaoFactory.user()` 获取实现，默认委托内存 `Db`
 */
public interface UserDao {
    /** 新增用户
     * @param username 用户名
     * @param password 密码
     * @param role 角色：student/admin
     * @param email 邮箱
     * @return 创建的用户对象
     */
    User addUser(String username, String password, String role, String email);
    /** 按用户名查询
     * @param username 用户名
     * @return 用户对象或 null
     */
    User findByName(String username);
    /** 重置密码
     * @param username 用户名
     * @param newPwd 新密码
     * @return 是否成功
     */
    boolean resetPassword(String username, String newPwd);
    /** 认证登录
     * @param username 用户名
     * @param password 密码
     * @return 认证通过的用户或 null
     */
    User auth(String username, String password);
}