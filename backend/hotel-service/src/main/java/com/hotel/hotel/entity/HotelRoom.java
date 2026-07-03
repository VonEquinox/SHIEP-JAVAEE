package com.hotel.hotel.entity;

import lombok.Data;

import java.math.BigDecimal;

/** 对应视图 v_hotel_room：某酒店的客房详细信息 */
@Data
public class HotelRoom {

    private Integer hotelId;
    private String hotelName;
    private String city;
    private String district;
    private String address;
    private Integer star;
    private Integer roomTypeId;
    private String typeName;
    private BigDecimal price;
    private Integer capacity;
    private Integer totalCount;
    private Integer availableCount;
}
