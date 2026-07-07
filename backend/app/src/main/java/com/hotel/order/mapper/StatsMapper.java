package com.hotel.order.mapper;

import com.hotel.order.entity.OrderDetailRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 汇总统计：三个存储过程 + 明细视图，SQL 在 StatsMapper.xml */
@Mapper
public interface StatsMapper {

    List<Map<String, Object>> bookingStats(@Param("start") LocalDate start,
                                           @Param("end") LocalDate end,
                                           @Param("grp") String grp);

    List<Map<String, Object>> guestStats(@Param("start") LocalDate start,
                                         @Param("end") LocalDate end,
                                         @Param("dim") String dim);

    List<Map<String, Object>> occupancy(@Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    /** 报表明细部分：时间段内订单明细（视图 v_order_detail） */
    @Select("SELECT * FROM v_order_detail WHERE status <> '已退订' "
            + "AND check_in_date BETWEEN #{start} AND #{end} ORDER BY check_in_date")
    List<OrderDetailRow> detailBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
