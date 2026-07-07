package com.hotel.user.service;

import com.hotel.user.entity.User;
import com.hotel.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public void register(User user) {
        Assert.hasText(user.getUsername(), "用户名不能为空");
        Assert.hasText(user.getPassword(), "密码不能为空");
        Assert.isTrue(user.getPhone() != null && user.getPhone().matches("\\d{11}"), "手机号必须为11位数字");
        Assert.isTrue(user.getIdCard() == null || user.getIdCard().length() == 18, "身份证号必须为18位");
        long count = userMapper.countByUsername(user.getUsername());
        Assert.isTrue(count == 0, "用户名已存在");

        user.setPassword(PASSWORD_ENCODER.encode(user.getPassword()));
        userMapper.insert(user);
    }

    /** 登录成功签发 token：Base64("userId:role")，由后端过滤器解码校验 */
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        Assert.isTrue(user != null && PASSWORD_ENCODER.matches(password, user.getPassword()),
                "用户名或密码错误");

        String token = Base64.getEncoder().encodeToString(
                (user.getUserId() + ":" + user.getRole()).getBytes(StandardCharsets.UTF_8));
        user.setPassword(null);
        return Map.of("token", token, "user", user);
    }

    public User getById(Integer userId) {
        User user = userMapper.selectById(userId);
        user.setPassword(null);
        return user;
    }

    /** 用户只能改自己的手机号、密码、身份证号 */
    public void updateMe(Integer userId, User form) {
        Assert.isTrue(form.getPhone() == null || form.getPhone().matches("\\d{11}"), "手机号必须为11位数字");
        Assert.isTrue(form.getIdCard() == null || form.getIdCard().length() == 18, "身份证号必须为18位");

        User user = new User();
        user.setUserId(userId);
        user.setPhone(form.getPhone());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(PASSWORD_ENCODER.encode(form.getPassword()));
        }
        user.setIdCard(form.getIdCard());
        userMapper.updateProfile(user);
    }

    /** 后台：用户列表 */
    public List<User> listAll() {
        List<User> users = userMapper.selectAll();
        users.forEach(u -> u.setPassword(null));
        return users;
    }
}
