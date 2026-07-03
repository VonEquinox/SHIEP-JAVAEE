package com.hotel.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("t_guest")
public class Guest {

    @TableId(type = IdType.AUTO)
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
