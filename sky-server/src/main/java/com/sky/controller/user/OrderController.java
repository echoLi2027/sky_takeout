package com.sky.controller.user;


import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "order relevant api")
@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * user submit order
     * @param ordersSubmitDTO
     * @return
     */
    @ApiOperation("order submit")
    @PostMapping("/submit")
    public Result<OrderSubmitVO> submitOrder(@RequestBody OrdersSubmitDTO ordersSubmitDTO){

        log.info("zzy_log order info: {}",ordersSubmitDTO);

        OrderSubmitVO orderSubmitVO = orderService.submit(ordersSubmitDTO);

        return Result.success(orderSubmitVO);
    }


    /**
     * order payment
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("order payment")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("zzy_log order payment：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("generate pre-payment invoice：{}", orderPaymentVO);

//        emulate payment success, modify db order status
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        log.info("zzy_log emulate payment success: {}",ordersPaymentDTO.getOrderNumber());

        return Result.success(orderPaymentVO);
    }


    @GetMapping("/historyOrders")
    @ApiOperation("check user history orders")
    public Result<PageResult> checkHistoryOrder(OrdersPageQueryDTO ordersPageQueryDTO){

        log.info("zzy_log order page queryDTO: {}",ordersPageQueryDTO);

        PageResult result = orderService.pageHistory(ordersPageQueryDTO);

        return Result.success(result);
    }
}
