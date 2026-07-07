package com.hotel.hotel.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomType {

    private Integer roomTypeId;
    private Integer hotelId;
    private String typeName;
    private BigDecimal price;
    private Integer capacity;
    private Integer totalCount;
    private Integer availableCount;
}
