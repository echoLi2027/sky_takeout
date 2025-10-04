package com.sky.service;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;

public interface OrderService {

    /**
     * user submit order
     * @param ordersSubmitDTO
     */
    OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * order payment
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * payment success, change order status
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * page query history order
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageHistory(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * query orders by id
     *
     * @param id
     * @return
     */
    Orders getByOrderId(Long id);

    /**
     * admin page search
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult pageSearch(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * get count of different status for the orders
     * @return
     */
    OrderStatisticsVO getOrderStatistics();


    /**
     * update status into confirmed
     * @param orders
     */
    void updateStatus(Orders orders);
}
