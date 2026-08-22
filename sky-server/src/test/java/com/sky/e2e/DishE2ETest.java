package com.sky.e2e;

import com.sky.dto.DishDTO;
import com.sky.entity.DishFlavor;
import com.sky.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DishE2ETest extends BaseE2ETest {

    private String tokenAsOperator() {
        Long operatorId = seedEmployee("dishop", "123456", 1);
        return adminToken(operatorId);
    }

    private DishDTO newDishDto(String name, Long categoryId, Integer status) {
        DishDTO dto = new DishDTO();
        dto.setName(name);
        dto.setCategoryId(categoryId);
        dto.setPrice(BigDecimal.valueOf(28.5));
        dto.setImage("http://example.com/d.png");
        dto.setDescription("tasty");
        dto.setStatus(status);
        return dto;
    }

    @Test
    void addDish_withFlavors_persistsDishAndFlavorBatch() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("DishCat", 1, 1);

        DishDTO dto = newDishDto("Kung Pao Chicken", categoryId, 1);
        DishFlavor spicy = DishFlavor.builder().name("Spice Level").value("[\"Mild\",\"Hot\"]").build();
        dto.setFlavors(Arrays.asList(spicy));

        HttpEntity<DishDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/dish", HttpMethod.POST, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);

        Long dishId = jdbcTemplate.queryForObject("select id from dish where name = ?", Long.class, "Kung Pao Chicken");
        Integer flavorCount = jdbcTemplate.queryForObject("select count(*) from dish_flavor where dish_id = ?", Integer.class, dishId);
        assertThat(flavorCount).isEqualTo(1);
    }

    @Test
    void page_filtersByNameCategoryAndStatus() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("PageDishCat", 1, 1);
        seedDish("Fried Rice", categoryId, 1);
        seedDish("Fried Noodles", categoryId, 0);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/dish/page?page=1&pageSize=10&name=Fried&status=1",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        Map data = (Map) response.getBody().get("data");
        assertThat(((Number) data.get("total")).intValue()).isEqualTo(1);
    }

    @Test
    void deleteIds_whenDishDisabled_succeeds() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("DelDishCat", 1, 1);
        Long dishId = seedDish("Disabled Dish", categoryId, 0);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/dish?ids=" + dishId, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        Integer count = jdbcTemplate.queryForObject("select count(*) from dish where id = ?", Integer.class, dishId);
        assertThat(count).isZero();
    }

    @Test
    void deleteIds_whenDishOnSale_isRejected() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("DelDishCat2", 1, 1);
        Long dishId = seedDish("Enabled Dish", categoryId, 1);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/dish?ids=" + dishId, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getMsg()).isEqualTo("起售中的菜品不能删除");
    }

    @Test
    void searchByCategory_returnsOnlyOnSaleDishes() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SearchDishCat", 1, 1);
        seedDish("On Sale Dish", categoryId, 1);
        seedDish("Off Sale Dish", categoryId, 0);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/dish/list?categoryId=" + categoryId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        List<Map> data = (List<Map>) response.getBody().get("data");
        assertThat(data).hasSize(1);
        assertThat(data.get(0).get("name")).isEqualTo("On Sale Dish");
    }

    @Test
    void searchById_returnsDishVOWithFlavors() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("ByIdDishCat", 1, 1);
        Long dishId = seedDish("Detail Dish", categoryId, 1);
        jdbcTemplate.update("insert into dish_flavor(dish_id, name, value) values (?, ?, ?)",
                dishId, "Sweetness", "[\"Low\",\"High\"]");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/dish/" + dishId, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        Map data = (Map) response.getBody().get("data");
        assertThat(data.get("name")).isEqualTo("Detail Dish");
        List<Map> flavors = (List<Map>) data.get("flavors");
        assertThat(flavors).hasSize(1);
    }

    @Test
    void updateDish_changesNameAndPrice() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("UpdDishCat", 1, 1);
        Long dishId = seedDish("Before Update", categoryId, 1);

        DishDTO dto = newDishDto("After Update", categoryId, 1);
        dto.setId(dishId);
        dto.setPrice(BigDecimal.valueOf(35.0));

        HttpEntity<DishDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/dish", HttpMethod.PUT, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        String name = jdbcTemplate.queryForObject("select name from dish where id = ?", String.class, dishId);
        assertThat(name).isEqualTo("After Update");
    }
}
