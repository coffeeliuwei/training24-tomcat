package com.training.dao;

import com.training.db.Db.User;

public interface UserDao {
    User addUser(String username, String password, String role, String email);
    User findByName(String username);
    boolean resetPassword(String username, String newPwd);
    User auth(String username, String password);
}