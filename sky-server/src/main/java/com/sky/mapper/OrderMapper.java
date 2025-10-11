package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderStatisticsVO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    /**
     * insert one order
     * @param order
     */
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber} and user_id = #{userId}")
    Orders getByNumber(String orderNumber, Long userId);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * page query orders, also need to get order details by order id
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    Page<Orders> pageQuery4User(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * we only need orders,no order details
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> conditionQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * return particular order by order_id
     * @param id
     * @return
     */
    Orders queryByOrderId(Long id);

    @MapKey("status")
    List<Map<String, Object>> getStatistics();

    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrdertimeLT(Integer status, LocalDateTime orderTime);


    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * get assigned date's turnover
     * @param map
     * @return
     */
    Double getTurnover(Map map);

    /**
     * get user count
     * @param begin
     * @param end
     * @param status
     * @return
     */
    Integer orderCount(LocalDateTime begin, LocalDateTime end, Integer status);

    /**
     * get top 10
     * @param begin
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop10(LocalDateTime begin, LocalDateTime end);
}
