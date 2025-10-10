package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * deal timeout and unpaid order, cancel order
     */
    @Scheduled(cron = "0 * * * * *")
    public void processTimeoutOrder(){
        log.info("zzy_log process timeout order: {}", new Date());
//        check if rest unpaid order exceed 15 minutes didn't pay
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);

        List<Orders> orders = orderMapper.getByStatusAndOrdertimeLT(Orders.UN_PAID, time);
        if (orders != null && orders.size() > 0){
            orders.forEach(order -> {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("unpaid timeout, cancel automatically");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            });
        }
    }

    /**
     * deal with still in delivery order, after all day
     */
    @Scheduled(cron = "0 46 19 * * *")
    public void processDeliveryOrder(){
        log.info("zzy_log process process in deliveringj order:{}", new Date());

//        every day after closing the shop check the in delivery orders and change them into completed status
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orders = orderMapper.getByStatusAndOrdertimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        if (orders!=null && orders.size()>0){
            orders.forEach(order -> {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            });
        }
    }





}
