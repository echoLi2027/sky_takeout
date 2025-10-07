package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private ShoppingCartService shoppingCartService;


    /**
     * user submit order
     * @param ordersSubmitDTO
     */
    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

//        1. exception handler, although frontend will verify it
//        but to make the project robust, we can verify again for example for api test case
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null){
            throw new AddressBookBusinessException((MessageConstant.ADDRESS_BOOK_IS_NULL));
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

//        2. check user shopping cart info
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.getByUserId(userId);
        if (shoppingCartList == null && shoppingCartList.size() > 0){
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

//        3. create order data
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

//        4. insert order into db, use generatedKey, later we need id for OrderDetail table
        orderMapper.insert(order);

//        5. insert order details(dishes, setmeals in the order, 1 order : m order_details)
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.batchInsert(orderDetailList);

//        6. clear shopping cart
        shoppingCartMapper.deleteByUserId(userId);

//        7. return submitted order info to frontend
        return OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        warning: access with 3rd party api need to avoid
        //调用微信支付接口，生成预支付交易单
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "sky_takeout order", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("code","ORDERPAID");
        jsonObject.put("nonceStr", UUID.randomUUID());
        jsonObject.put("paySign","wechatPaySimulation");
        jsonObject.put("timeStamp", Timestamp.valueOf(LocalDateTime.now()));
        jsonObject.put("signType","SHA256withRSA");
        jsonObject.put("packageStr","testPackage");



        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public PageResult pageQuery4User(int page, int pageSize, Integer status) {

        PageHelper.startPage(page, pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setPage(page);
        ordersPageQueryDTO.setPageSize(pageSize);
        ordersPageQueryDTO.setStatus(status);
//        it's current user's orders
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());

//        Page<Orders> ordersPage =  orderMapper.pageQuery(ordersPageQueryDTO);

        Page<Orders> ordersPage = orderMapper.pageQuery4User(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

//        make sure that this user has orders
        if (ordersPage != null && ordersPage.getTotal() > 0){
            for (Orders orders : ordersPage) {

                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetailList);

                list.add(orderVO);
            }
        }

        return new PageResult(ordersPage.getTotal(), list);
    }

    @Override
    public OrderVO getByOrderId(Long id) {

        Orders orders = orderMapper.queryByOrderId(id);

//        get orderDetails list by orders
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);


        return orderVO;
    }

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        /*
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> ordersPage =  orderMapper.conditionQuery(ordersPageQueryDTO);

        List<Orders> result = ordersPage.getResult();

        for (Orders orders : result) {

            List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

            List<String> names = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList) {
                String name = orderDetail.getName();
                names.add(name);
            }
            String collect = names.stream().collect(Collectors.joining(","));
            orders.setOrderDishes(collect);
            names.clear();
        }

        return new PageResult(ordersPage.getTotal(), result);

 */

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> ordersPage =  orderMapper.conditionQuery(ordersPageQueryDTO);

        List<OrderVO> orderVOList = getOrderVOList(ordersPage);

        return new PageResult(ordersPage.getTotal(), orderVOList);

    }

    private List<OrderVO> getOrderVOList(Page<Orders> page){

        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> result = page.getResult();

        if (!CollectionUtils.isEmpty(result)){
            for (Orders orders : result) {
//                copy same properties into OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                String orderDishesStr = getOrderDishesStr(orders);

//                add order detail info into orderVO
                orderVO.setOrderDishes(orderDishesStr);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders){
//        get order dishes and amount
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

//        concat each dish info(dishName*amount)
        List<String> stringList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";

            return orderDish;
        }).collect(Collectors.toList());

        return String.join("",stringList);

    }

    @Override
    public OrderStatisticsVO getOrderStatistics() {

        List<Map<String, Object>> statistics = orderMapper.getStatistics();
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        for (Map<String, Object> statistic : statistics) {
            String status = (String)statistic.get("status");
            if (status.equals("toBeConfirmed")){
                orderStatisticsVO.setToBeConfirmed((Long) statistic.get("num"));
            }
            else if(status.equals("confirmed")){
                orderStatisticsVO.setConfirmed((Long) statistic.get("num"));
            }else if(status.equals("deliveryInProgress")){
                orderStatisticsVO.setDeliveryInProgress((Long) statistic.get("num"));
            }else if(status.equals("completed")){
                orderStatisticsVO.setCompleted((Long) statistic.get("num"));
            }else if(status.equals("cancelled")){
                orderStatisticsVO.setCancelled((Long) statistic.get("num"));
            }
        }
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {

        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
        /*
        orders.setStatus(Orders.CONFIRMED);
//        orders.setDeliveryStatus(0);
        orderMapper.update(orders);*/
    }

    @Override
    public void rejectOrder(Long id) {

//        1. get order by id
        Orders ordersDB = orderMapper.queryByOrderId(id);
//        verify if order exist
        if (ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

//        verify the order status correct or not
//        by far admin hasn't accepted the order, so the status should less than 2
        if (ordersDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

//        if order is already paid we need to refund
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            //        emulate refund the money
            log.info("zzy_log emulate refund method.....");

//            change payStatus to refund, because from here we know the money has refunded.
            orders.setPayStatus(Orders.REFUND);
        }


//        update orders
//        after refund the money we can change it into cancelled
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("user cancelled.");


        orderMapper.update(orders);

    }

    @Override
    public void deliveryOrder(Long id) {

//        get order
        Orders ordersDB = orderMapper.queryByOrderId(id);

//        verify order
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();

        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
//        orders.setDeliveryStatus(1);

        orderMapper.update(orders);
    }

    @Override
    public void completeOrder(Long id) {
//        get order
        Orders ordersDB = orderMapper.queryByOrderId(id);

//        verify order
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();

        orders.setId(id);
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    @Override
    public void orderRepetition(Long id) {

        /*for (OrderDetail orderDetail : orderDetailMapper.getByOrderId(id)) {
            Long dishId = orderDetail.getDishId();
            ShoppingCartDTO shoppingCartDTO = new ShoppingCartDTO();
            if (dishId!=null){
                shoppingCartDTO.setDishId(dishId);
                shoppingCartService.addShoppingCart(shoppingCartDTO);
            }else{
                shoppingCartDTO.setSetmealId(orderDetail.getSetmealId());
                shoppingCartDTO.setDishFlavor(orderDetail.getDishFlavor());
                shoppingCartService.addShoppingCart(shoppingCartDTO);
            }
        }*/

//        we can also create our own logic not depend on shopping service, make application more robust, each part independently
//        1. check current user id
        Long userId = BaseContext.getCurrentId();

//        check order details
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

//        convert order details obj into shopping cart obj
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

//            copy original order details into new shopping cart obj
//            "id" property is the ignore property we don't need copy to shoppingCart
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

//            so we convert orderDetail obj into shoppingCart obj
            return shoppingCart;

        }).collect(Collectors.toList());

//        insert shoppingCartList into db
        shoppingCartMapper.insertBatch(shoppingCartList);


    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
//        get order by id
        Orders orderDB = orderMapper.queryByOrderId(ordersRejectionDTO.getId());

//        only order exists and order status == 2(waiting to accept) admin can reject order
        if (orderDB == null || !orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

//        refund
        Integer payStatus = orderDB.getPayStatus();
        if (Objects.equals(payStatus, Orders.PAID)){
            log.info("zzy_log emulate refund api........");
        }

//        create a new order update orderDB into new order
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
        orders.setCancelTime(LocalDateTime.now());
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setStatus(Orders.CANCELLED);

        orderMapper.update(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {

//        check if order exist
        Orders orderDB = orderMapper.queryByOrderId(ordersCancelDTO.getId());


//        if we paid we refund
        if(orderDB.getPayStatus().equals(Orders.PAID)){
            log.info("zzy_log emulate refund api........");
        }

//        create new orders and update to db
        Orders orders = new Orders();
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(orders.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setId(orderDB.getId());

        orderMapper.update(orders);

    }
}
