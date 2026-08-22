package com.sky.controller.user;


import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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


    /**
     * check all historical orders [according to status]
     * @param page,pageSize,status
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("check user history orders")
    public Result<PageResult> checkHistoryOrder(int page, int pageSize, Integer status){

        log.info("zzy_log order page query 3 params: {},{}, {}",page, pageSize, status);

        PageResult result = orderService.pageQuery4User(page,pageSize, status);

        return Result.success(result);
    }

    @GetMapping("/orderDetail/{id}")
    @ApiOperation("get oder details by orderId")
    public Result<Orders> getByOrderId(@PathVariable Long id){
        log.info("zzy_log order historical query by order id: {}",id);

        OrderVO result = orderService.getByOrderId(id);

        return Result.success(result);
    }


    @PutMapping("/cancel/{id}")
    @ApiOperation("user cancel order, also need to refund the money")
    public Result cancelOrder(@PathVariable Long id){
        log.info("zzy_log user cancel order, focus on the id: {}",id);

        orderService.rejectOrder(id);

        return Result.success();
    }

    @PostMapping("/repetition/{id}")
    @ApiOperation("user get a repetition order")
    public Result repetitionOrder(@PathVariable Long id) {

        orderService.orderRepetition(id);


        return Result.success();
    }

    @GetMapping("/reminder/{id}")
    @ApiOperation("user send msg to the shop")
    public Result reminder(@PathVariable("id") Long id){
        orderService.reminder(id);

        return Result.success();
    }

}
