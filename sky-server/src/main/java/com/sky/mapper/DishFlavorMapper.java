package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    /**
     * insert several dish flavors
     * @param dishFlavors
     * @return
     */
    void saveBatch(List<DishFlavor> dishFlavors);

}
