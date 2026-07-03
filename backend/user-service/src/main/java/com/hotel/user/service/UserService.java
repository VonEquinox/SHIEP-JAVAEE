package com.hotel.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.user.entity.User;
import com.hotel.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
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

    public void register(User user) {
        Assert.hasText(user.getUsername(), "用户名不能为空");
        Assert.hasText(user.getPassword(), "密码不能为空");
        Assert.isTrue(user.getPhone() != null && user.getPhone().matches("\\d{11}"), "手机号必须为11位数字");
        Assert.isTrue(user.getIdCard() == null || user.getIdCard().length() == 18, "身份证号必须为18位");
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        Assert.isTrue(count == 0, "用户名已存在");

        userMapper.insert(user);
    }

    /** 登录成功签发 token：Base64("userId:role")，由网关解码校验 */
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        Assert.isTrue(user != null && user.getPassword().equals(password), "用户名或密码错误");

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
        user.setPassword(form.getPassword());
        user.setIdCard(form.getIdCard());
        userMapper.updateById(user);
    }

    /** 后台：用户列表 */
    public List<User> listAll() {
        List<User> users = userMapper.selectList(null);
        users.forEach(u -> u.setPassword(null));
        return users;
    }
}
