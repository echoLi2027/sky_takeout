package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Override
    public PageResult pageHistory(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> ordersPage =  orderMapper.pageQuery(ordersPageQueryDTO);

        return new PageResult(ordersPage.getTotal(), ordersPage.getResult());
    }

    @Override
    public Orders getByOrderId(Long id) {

        Orders orders = orderMapper.queryByOrderId(id);

        return orders;
    }

    @Override
    public PageResult pageSearch(OrdersPageQueryDTO ordersPageQueryDTO) {

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> ordersPage =  orderMapper.pageQuery(ordersPageQueryDTO);

        List<Orders> result = ordersPage.getResult();

        for (Orders orders : result) {
            List<OrderDetail> orderDetailList = orders.getOrderDetailList();
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
    public void updateStatus(Orders orders) {
        orders.setStatus(Orders.CONFIRMED);
//        orders.setDeliveryStatus(0);
        orderMapper.update(orders);
    }

    @Override
    public void rejectOrder(Orders orders) {

//        1. refund the money
        log.info("zzy_log emulate refund method.....");
//        2. update orders
        orders.setPayStatus(Orders.REFUND);
        orders.setStatus(Orders.CANCELLED);

        orderMapper.update(orders);

    }

    @Override
    public void deliveryOrder(Long id) {
        Orders orders = new Orders();

        orders.setId(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
//        orders.setDeliveryStatus(1);

        orderMapper.update(orders);
    }

    @Override
    public void completeOrder(Long id) {
        Orders orders = new Orders();

        orders.setId(id);
        orders.setStatus(Orders.COMPLETED);

        orderMapper.update(orders);
    }
}
