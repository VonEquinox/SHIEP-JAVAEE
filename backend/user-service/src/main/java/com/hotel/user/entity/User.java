package com.hotel.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer userId;
    private String username;
    private String password;
    private String phone;
    private String idCard;
    private Integer points;
    private String level;
    private String role;
    private LocalDateTime createdAt;
}
