package com.hotel.user.controller;

import com.hotel.common.Result;
import com.hotel.user.entity.User;
import com.hotel.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@RequestBody User user) {
        userService.register(user);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> form) {
        return Result.ok(userService.login(form.get("username"), form.get("password")));
    }

    /** 当前登录用户信息（X-User-Id 由后端过滤器注入） */
    @GetMapping("/me")
    public Result<User> me(@RequestHeader("X-User-Id") Integer userId) {
        return Result.ok(userService.getById(userId));
    }

    @PutMapping("/me")
    public Result<Void> updateMe(@RequestHeader("X-User-Id") Integer userId, @RequestBody User form) {
        userService.updateMe(userId, form);
        return Result.ok();
    }

    /** 后台：用户列表 */
    @GetMapping("/list")
    public Result<List<User>> list(@RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            throw new IllegalArgumentException("无权限");
        }
        return Result.ok(userService.listAll());
    }
}
