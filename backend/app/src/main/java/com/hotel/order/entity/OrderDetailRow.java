package com.hotel.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 对应视图 v_order_detail 的一行（订单×房间×入住人展开） */
@Data
public class OrderDetailRow {

    private Integer orderId;
    private String orderNo;
    private Integer userId;
    private String username;
    private String status;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private BigDecimal payPrice;
    private LocalDateTime createdAt;
    private Integer hotelId;
    private String hotelName;
    private String city;
    private String district;
    private Integer itemId;
    private String typeName;
    private BigDecimal roomPrice;
    private String guestName;
    private String gender;
    private Integer guestAge;
    private String occupation;
    private String education;
    private String incomeLevel;
}
