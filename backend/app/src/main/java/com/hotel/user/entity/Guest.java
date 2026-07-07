package com.hotel.user.entity;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Guest {

    private Integer guestId;
    private Integer userId;
    private String name;
    private String idCard;
    private String gender;
    private LocalDate birthDate;
    private String occupation;
    private String education;
    private String incomeLevel;
}
