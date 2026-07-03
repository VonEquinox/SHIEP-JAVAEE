package com.hotel.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Integer reviewId;
    private Integer orderId;
    private Integer userId;
    private Integer score;
    private String content;
    private LocalDateTime createdAt;
}
