
package com.jotangnote.service;

import com.jotangnote.entity.User;
import com.jotangnote.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean register(User user) {
        if (userMapper.findByUsername(user.getUsername()) != null) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userMapper.insert(user) > 0;
    }

    public User login(String username, String rawPassword) {
        User user = userMapper.findByUsername(username);

        if (user == null) {
            return null;
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return null;
        }

        return user;
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }
}
