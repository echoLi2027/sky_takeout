package com.sky.controller.admin;


import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "admin order relevant api")
@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/conditionSearch")
    @ApiOperation("get orders on condition")
    public Result conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){

        log.info("zzy_log admin orders query conditions: {}", ordersPageQueryDTO);

        PageResult result = orderService.conditionSearch(ordersPageQueryDTO);

        return Result.success(result);
    }

    @GetMapping("/statistics")
    @ApiOperation("account of different types of orders")
    public Result ordersStatistics(){
        OrderStatisticsVO result = orderService.getOrderStatistics();
        return Result.success(result);
    }

    @GetMapping("/details/{id}")
    @ApiOperation("admin get oder details by orderId")
    public Result<Orders> getByOrderId(@PathVariable Long id){
        log.info("zzy_log order historical query by order id: {}",id);

        Orders result = orderService.getByOrderId(id);

        return Result.success(result);
    }

    @PutMapping("/confirm")
    @ApiOperation("admin update orders status")
    public Result ConfirmOrder(@RequestBody Orders orders){
        log.info("zzy_log order status change: {}",orders);

        orderService.updateStatus(orders);

        return Result.success();
    }

    @PutMapping("/rejection")
    @ApiOperation("reject order, also need to refund the money")
    public Result rejectOrder(@RequestBody Orders orders){
        log.info("zzy_log reject order, focus on the reject reason: {}",orders);

//        orderService.rejectOrder(orders);

        return Result.success();
    }

    @PutMapping("/cancel")
    @ApiOperation("cancel order, also need to refund the money")
    public Result cancelOrder(@RequestBody Orders orders){
        log.info("zzy_log admin cancel order, focus on the reject reason: {}",orders);

//        orderService.rejectOrder(orders);

        return Result.success();
    }

    @PutMapping("/delivery/{id}")
    @ApiOperation("orders already starts delivery")
    public Result deliveryOrders(@PathVariable Long id){

        log.info("zzy_log admin delivery order, focus on the id: {}",id);

        orderService.deliveryOrder(id);

        return Result.success();
    }

    @PutMapping("/complete/{id}")
    @ApiOperation("order delivered")
    public Result orderComplte(@PathVariable Long id){

        log.info("zzy_log admin complete order, focus on the id: {}",id);

        orderService.completeOrder(id);

        return Result.success();
    }



}
