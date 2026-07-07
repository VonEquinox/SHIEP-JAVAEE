package com.hotel.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 订单列表项（我的订单 / 后台订单管理共用） */
@Data
public class OrderListItem {

    private Integer orderId;
    private String orderNo;
    private String username;
    private String hotelName;
    private String city;
    private String district;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private BigDecimal payPrice;
    private String status;
    private LocalDateTime createdAt;
}
