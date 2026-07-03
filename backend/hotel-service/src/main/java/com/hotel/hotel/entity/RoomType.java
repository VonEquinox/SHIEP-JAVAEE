package com.hotel.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_room_type")
public class RoomType {

    @TableId(type = IdType.AUTO)
    private Integer roomTypeId;
    private Integer hotelId;
    private String typeName;
    private BigDecimal price;
    private Integer capacity;
    private Integer totalCount;
    private Integer availableCount;
}
