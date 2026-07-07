package com.hotel.user.service;

import com.hotel.user.entity.Guest;
import com.hotel.user.mapper.GuestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestMapper guestMapper;

    public List<Guest> listByUser(Integer userId) {
        return guestMapper.selectByUserId(userId);
    }

    public void add(Integer userId, Guest guest) {
        Assert.hasText(guest.getName(), "入住人姓名不能为空");
        Assert.isTrue(guest.getIdCard() == null || guest.getIdCard().length() == 18, "身份证号必须为18位");
        guest.setGuestId(null);
        guest.setUserId(userId);
        guestMapper.insert(guest);
    }

    public void update(Integer userId, Guest guest) {
        checkOwner(userId, guest.getGuestId());
        Assert.isTrue(guest.getIdCard() == null || guest.getIdCard().length() == 18, "身份证号必须为18位");
        guest.setUserId(userId);
        guestMapper.update(guest);
    }

    public void delete(Integer userId, Integer guestId) {
        checkOwner(userId, guestId);
        try {
            guestMapper.deleteById(guestId);
        } catch (Exception e) {
            throw new IllegalArgumentException("该入住人已有订单记录，不能删除");
        }
    }

    private void checkOwner(Integer userId, Integer guestId) {
        Guest db = guestMapper.selectById(guestId);
        Assert.isTrue(db != null && db.getUserId().equals(userId), "入住人不存在");
    }
}
