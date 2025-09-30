package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Transactional(rollbackFor = RuntimeException.class)
    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
//        only query shopping cart for current user
        shoppingCart.setUserId(BaseContext.getCurrentId());

//        check if current product already in the shopping cart
//        because for setmeal type of product, one setmeal can have multiple dish
//        thus we will get List<ShoppingCart> for one setmeal
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList != null && shoppingCartList.size()>0){
//            just add the count for this product
            shoppingCart = shoppingCartList.get(0);
//            shoppingCart.setNumber(shoppingCart.getNumber());
            shoppingCartMapper.updateNumberById(shoppingCart);

        }else{
//            current product not exist in shopping cart, we need to add it in the table

//            make sure it's dish or setmeal
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null){
//                add dish
                DishVO dishVO = dishMapper.selectById(dishId);
                shoppingCart.setName(dishVO.getName());
                shoppingCart.setImage(dishVO.getImage());
                shoppingCart.setAmount(dishVO.getPrice());
            }else {
//           add setmeal
                Setmeal setmeal = setmealMapper.queryById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }

    }
}
