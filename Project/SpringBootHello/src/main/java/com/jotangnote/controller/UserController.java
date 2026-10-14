
package com.jotangnote.controller;

import com.jotangnote.entity.User;
import com.jotangnote.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            return "用户名和密码不能为空";
        }

        boolean success = userService.register(user);

        if (success) {
            return "注册成功";
        } else {
            return "用户名已存在";
        }
    }

    @PostMapping("/login")
    public String login(@RequestBody User user, HttpSession session) {
        if (user.getUsername() == null || user.getPassword() == null
                || user.getUsername().isBlank() || user.getPassword().isBlank()) {
            return "用户名和密码不能为空";
        }

        User loggedInUser = userService.login(
                user.getUsername(),
                user.getPassword()
        );

        if (loggedInUser == null) {
            return "用户名或密码错误";
        }

        session.setAttribute("userId", loggedInUser.getId());

        return "登录成功";
    }
}
