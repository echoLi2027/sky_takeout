package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
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
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList != null && shoppingCartList.size()>0){
//            just add the count for this product or if user subtract the number of the dishes
            shoppingCart = shoppingCartList.get(0);
            Integer number = shoppingCart.getNumber() + 1;
            shoppingCart.setNumber(number);
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

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
//        for sub situation this product must in shopping cart, so we just need to find the shopping cart list
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO,shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());

        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

//        and this size of list is one, either it's setmeal or dish
        shoppingCart = shoppingCartList.get(0);

//        if the number of that product is 1 then we should delete that product
        if (shoppingCart.getNumber()==1){
            shoppingCartMapper.deleteById(shoppingCart.getId());
        }else{
//            subtract the number of setmeal or dish
            Integer number = shoppingCart.getNumber() - 1;
            shoppingCart.setNumber(number);
            shoppingCartMapper.updateNumberById(shoppingCart);
        }
    }

    @Override
    public void clear(Long currentId) {
        shoppingCartMapper.deleteByUserId(currentId);
    }

    @Override
    public List<ShoppingCart> getShoppingCart(Long currentId) {

        return shoppingCartMapper.getByUserId(currentId);
    }

}
