package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Api(tags = "shopping cart api")
@RequestMapping("/user/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @ApiOperation("add item into shoppingCart")
    @PostMapping("/add")
    public Result addShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO){

        log.info("zzy_log- add shoppingCartDTO: {}",shoppingCartDTO);

        shoppingCartService.addShoppingCart(shoppingCartDTO);

        return Result.success();
    }

    @ApiOperation("sub item into shoppingCart")
    @PostMapping("/sub")
    public Result subShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO){

        log.info("zzy_log- sub shoppingCartDTO: {}",shoppingCartDTO);

        shoppingCartService.subShoppingCart(shoppingCartDTO);

        return Result.success();
    }

    @ApiOperation("display shopping cart info")
    @GetMapping("/list")
    public Result<List<ShoppingCart>> displayShoppingCart(){
//        we already get user_id from threadLocal, just this userId we can get the shopping cart info
        List<ShoppingCart> shoppingCartList = shoppingCartService.getShoppingCart(BaseContext.getCurrentId());

        return Result.success(shoppingCartList);

    }

    @ApiOperation("clear shopping cart")
    @DeleteMapping("/clean")
    public Result clearShoppingCart(){
        shoppingCartService.clear(BaseContext.getCurrentId());
        return Result.success();
    }

}
