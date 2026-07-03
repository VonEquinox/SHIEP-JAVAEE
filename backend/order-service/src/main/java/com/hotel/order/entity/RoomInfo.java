package com.hotel.order.entity;

import lombok.Data;

import java.math.BigDecimal;

/** 下单校验用的房型信息（查询 t_room_type，属于订单域的只读数据） */
@Data
public class RoomInfo {

    private Integer roomTypeId;
    private Integer hotelId;
    private String typeName;
    private BigDecimal price;
    private Integer capacity;
    private Integer availableCount;
}
