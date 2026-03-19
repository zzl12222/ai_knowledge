package com.ai.knowledge.service;

import com.ai.knowledge.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    
    User login(String username, String password);
    
    User register(String username, String password, String email);
    
    User getUserById(Long id);
    
    boolean updateUserPoints(Long userId, Integer points);
    
    boolean deductPoints(Long userId, Integer points);
}