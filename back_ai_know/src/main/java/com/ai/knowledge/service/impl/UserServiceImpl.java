package com.ai.knowledge.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import com.ai.knowledge.entity.User;
import com.ai.knowledge.mapper.UserMapper;
import com.ai.knowledge.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String USER_TOKEN_PREFIX = "user:token:";
    private static final String USER_INFO_PREFIX = "user:info:";
    private static final long TOKEN_EXPIRE_HOURS = 24;
    
    @Override
    public User login(String username, String password) {
        User user = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        
        if (user == null) {
            return null;
        }
        
        String encryptedPassword = DigestUtil.md5Hex(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            return null;
        }
        
        String token = DigestUtil.md5Hex(username + System.currentTimeMillis());
        redisTemplate.opsForValue().set(
            USER_TOKEN_PREFIX + token,
            user.getId(),
            TOKEN_EXPIRE_HOURS,
            TimeUnit.HOURS
        );
        
        redisTemplate.opsForValue().set(
            USER_INFO_PREFIX + user.getId(),
            user,
            TOKEN_EXPIRE_HOURS,
            TimeUnit.HOURS
        );
        
        return user;
    }
    
    @Override
    public User register(String username, String password, String email) {
        User existUser = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        
        if (existUser != null) {
            return null;
        }
        
        User user = new User();
        user.setUsername(username);
        user.setPassword(DigestUtil.md5Hex(password));
        user.setEmail(email);
        user.setPoints(100);
        
        userMapper.insert(user);
        return user;
    }
    
    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    
    @Override
    public boolean updateUserPoints(Long userId, Integer points) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        
        user.setPoints(user.getPoints() + points);
        return userMapper.updateById(user) > 0;
    }
    
    @Override
    public boolean deductPoints(Long userId, Integer points) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getPoints() < points) {
            return false;
        }
        
        user.setPoints(user.getPoints() - points);
        return userMapper.updateById(user) > 0;
    }
}