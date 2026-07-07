package com.hotel.order.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Review {

    private Integer reviewId;
    private Integer orderId;
    private Integer userId;
    private Integer score;
    private String content;
    private LocalDateTime createdAt;
}
