package com.hotel.user.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

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
