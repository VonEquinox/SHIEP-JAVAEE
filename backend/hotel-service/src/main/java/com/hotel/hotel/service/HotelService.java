package com.hotel.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.hotel.entity.Hotel;
import com.hotel.hotel.entity.HotelRoom;
import com.hotel.hotel.entity.RoomType;
import com.hotel.hotel.mapper.HotelMapper;
import com.hotel.hotel.mapper.RoomTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;

    public List<Hotel> search(String city, String district, String name,
                              BigDecimal priceMin, BigDecimal priceMax) {
        return hotelMapper.search(city, district, name, priceMin, priceMax);
    }

    public List<HotelRoom> rooms(Integer hotelId) {
        return hotelMapper.selectRooms(hotelId);
    }

    /** 城市 -> 区域列表，供前端级联下拉 */
    public Map<String, List<String>> cities() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Hotel h : hotelMapper.selectList(null)) {
            result.computeIfAbsent(h.getCity(), k -> new java.util.ArrayList<>());
            if (!result.get(h.getCity()).contains(h.getDistrict())) {
                result.get(h.getCity()).add(h.getDistrict());
            }
        }
        return result;
    }

    // ---------- 后台维护 ----------

    public void saveHotel(Hotel hotel) {
        Assert.hasText(hotel.getHotelName(), "酒店名不能为空");
        Assert.hasText(hotel.getCity(), "城市不能为空");
        Assert.hasText(hotel.getDistrict(), "区域不能为空");
        if (hotel.getHotelId() == null) {
            hotelMapper.insert(hotel);
        } else {
            hotelMapper.updateById(hotel);
        }
    }

    public void saveRoomType(RoomType room) {
        Assert.hasText(room.getTypeName(), "房型名不能为空");
        Assert.notNull(room.getHotelId(), "所属酒店不能为空");
        Assert.isTrue(room.getCapacity() != null && room.getCapacity() >= 1, "可住人数至少为1");
        Assert.isTrue(room.getTotalCount() != null && room.getTotalCount() >= 0, "客房总数不能为负");
        if (room.getRoomTypeId() == null) {
            room.setAvailableCount(room.getTotalCount());
            roomTypeMapper.insert(room);
        } else {
            RoomType old = roomTypeMapper.selectById(room.getRoomTypeId());
            Assert.notNull(old, "房型不存在");
            // 调整总数时，同步调整可预订数，保持已占用数量不变
            int occupied = old.getTotalCount() - old.getAvailableCount();
            Assert.isTrue(room.getTotalCount() >= occupied, "总数不能小于已被预订的数量" + occupied);
            room.setAvailableCount(room.getTotalCount() - occupied);
            roomTypeMapper.updateById(room);
        }
    }

    public void deleteRoomType(Integer roomTypeId) {
        try {
            roomTypeMapper.deleteById(roomTypeId);
        } catch (Exception e) {
            throw new IllegalArgumentException("该房型已有订单记录，不能删除");
        }
    }
}
