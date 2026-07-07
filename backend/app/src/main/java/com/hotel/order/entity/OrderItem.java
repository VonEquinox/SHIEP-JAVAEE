package com.hotel.order.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItem {

    private Integer itemId;
    private Integer orderId;
    private Integer roomTypeId;
    private BigDecimal price;
}
