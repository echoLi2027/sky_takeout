package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDetailMapper {
    /**
     * insert a batch of OrderDtail
     * @param orderDetailList
     */
    void batchInsert(List<OrderDetail> orderDetailList);
}
