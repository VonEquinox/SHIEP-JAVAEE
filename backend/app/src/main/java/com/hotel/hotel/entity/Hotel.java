package com.hotel.hotel.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Hotel {

    private Integer hotelId;
    private String hotelName;
    private String city;
    private String district;
    private String address;
    private Integer star;
    private String phone;

    /** 以下为搜索列表附加字段，不在表中 */
    private BigDecimal minPrice;
    private BigDecimal avgScore;
}
