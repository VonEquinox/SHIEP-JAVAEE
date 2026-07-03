package com.hotel.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.user.entity.Guest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GuestMapper extends BaseMapper<Guest> {
}
