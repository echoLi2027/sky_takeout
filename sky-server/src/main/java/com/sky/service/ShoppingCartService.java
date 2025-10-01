package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    List<ShoppingCart> getShoppingCart(Long currentId);

    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);

    void clear(Long currentId);
}
