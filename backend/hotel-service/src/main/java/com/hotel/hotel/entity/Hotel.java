package com.hotel.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_hotel")
public class Hotel {

    @TableId(type = IdType.AUTO)
    private Integer hotelId;
    private String hotelName;
    private String city;
    private String district;
    private String address;
    private Integer star;
    private String phone;

    /** 以下为搜索列表附加字段，不在表中 */
    @TableField(exist = false)
    private BigDecimal minPrice;
    @TableField(exist = false)
    private BigDecimal avgScore;
}
