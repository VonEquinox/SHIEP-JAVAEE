package com.hotel.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Order {

    private Integer orderId;
    private String orderNo;
    private Integer userId;
    private Integer hotelId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BigDecimal totalPrice;
    private BigDecimal payPrice;
    private String status;
    private LocalDateTime createdAt;
}
