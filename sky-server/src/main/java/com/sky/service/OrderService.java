package com.sky.service;

import com.sky.dto.*;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

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
     * @param page, pageSize, status
     * @return
     */
    PageResult pageQuery4User(int page, int pageSize, Integer status);

    /**
     * query orders by id
     *
     * @param id
     * @return
     */
    OrderVO getByOrderId(Long id);

    /**
     * admin page search
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * get count of different status for the orders
     * @return
     */
    OrderStatisticsVO getOrderStatistics();


    /**
     * update status into confirmed
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);

    /**
     * reject orders
     * @param id
     */
    void rejectOrder(Long id);

    /**
     * delivery order
     * @param id
     */
    void deliveryOrder(Long id);

    /**
     * complete order
     * @param id
     */
    void completeOrder(Long id);

    /**
     * add order into shopping cart again
     * @param id
     */
    void orderRepetition(Long id);

    /**
     * admin rejects order
     * @param ordersRejectionDTO
     */
    void rejection(OrdersRejectionDTO ordersRejectionDTO);

    /**
     * order cancelled by admin
     * @param ordersCancelDTO
     */
    void cancel(OrdersCancelDTO ordersCancelDTO);

    /**
     * remind the shop to be quick
     * @param id
     */
    void reminder(Long id);
}
