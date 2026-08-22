package com.sky.e2e;

import com.sky.dto.SetmealDTO;
import com.sky.entity.SetmealDish;
import com.sky.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SetmealE2ETest extends BaseE2ETest {

    private String tokenAsOperator() {
        Long operatorId = seedEmployee("smop", "123456", 1);
        return adminToken(operatorId);
    }

    private SetmealDTO newSetmealDto(String name, Long categoryId, Integer status, Long dishId) {
        SetmealDTO dto = new SetmealDTO();
        dto.setName(name);
        dto.setCategoryId(categoryId);
        dto.setPrice(BigDecimal.valueOf(58.0));
        dto.setDescription("combo");
        dto.setImage("http://example.com/s.png");
        dto.setStatus(status);
        SetmealDish setmealDish = SetmealDish.builder()
                .dishId(dishId)
                .name("comp dish")
                .price(BigDecimal.valueOf(18.0))
                .copies(1)
                .build();
        dto.setSetmealDishes(Arrays.asList(setmealDish));
        return dto;
    }

    @Test
    void insertSetmeal_persistsSetmealAndSetmealDishBatch() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmCat", 2, 1);
        Long dishCategoryId = seedCategory("SmDishCat", 1, 1);
        Long dishId = seedDish("Combo Dish", dishCategoryId, 1);

        SetmealDTO dto = newSetmealDto("Family Combo", categoryId, 0, dishId);
        HttpEntity<SetmealDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/setmeal", HttpMethod.POST, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);

        Long setmealId = jdbcTemplate.queryForObject("select id from setmeal where name = ?", Long.class, "Family Combo");
        Integer linkCount = jdbcTemplate.queryForObject("select count(*) from setmeal_dish where setmeal_id = ?", Integer.class, setmealId);
        assertThat(linkCount).isEqualTo(1);
    }

    @Test
    void pageQuery_filtersByStatus() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmPageCat", 2, 1);
        seedSetmeal("On Sale Combo", categoryId, 1);
        seedSetmeal("Off Sale Combo", categoryId, 0);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/setmeal/page?page=1&pageSize=10&status=1",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        Map data = (Map) response.getBody().get("data");
        assertThat(((Number) data.get("total")).intValue()).isEqualTo(1);
    }

    @Test
    void deleteSetmeals_whenDisabled_succeeds() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmDelCat", 2, 1);
        Long setmealId = seedSetmeal("Deletable Combo", categoryId, 0);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/setmeal?ids=" + setmealId, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        Integer count = jdbcTemplate.queryForObject("select count(*) from setmeal where id = ?", Integer.class, setmealId);
        assertThat(count).isZero();
    }

    @Test
    void deleteSetmeals_whenOnSale_isRejected() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmDelCat2", 2, 1);
        Long setmealId = seedSetmeal("Onsale Combo", categoryId, 1);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/setmeal?ids=" + setmealId, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getMsg()).isEqualTo("起售中的套餐不能删除");
    }

    @Test
    void setStatus_enable_succeedsWhenAllDishesEnabled() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmEnableCat", 2, 1);
        Long dishCategoryId = seedCategory("SmEnableDishCat", 1, 1);
        Long dishId = seedDish("Enabled Combo Dish", dishCategoryId, 1);
        Long setmealId = seedSetmeal("Enable Combo", categoryId, 0);
        jdbcTemplate.update("insert into setmeal_dish(setmeal_id, dish_id, name, price, copies) values (?, ?, ?, ?, ?)",
                setmealId, dishId, "dish", BigDecimal.valueOf(18.0), 1);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/setmeal/status/1?id=" + setmealId,
                HttpMethod.POST, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        Integer status = jdbcTemplate.queryForObject("select status from setmeal where id = ?", Integer.class, setmealId);
        assertThat(status).isEqualTo(1);
    }

    @Test
    void setStatus_enable_rejectedWhenAnyDishDisabled() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmBlockCat", 2, 1);
        Long dishCategoryId = seedCategory("SmBlockDishCat", 1, 1);
        Long dishId = seedDish("Disabled Combo Dish", dishCategoryId, 0);
        Long setmealId = seedSetmeal("Blocked Combo", categoryId, 0);
        jdbcTemplate.update("insert into setmeal_dish(setmeal_id, dish_id, name, price, copies) values (?, ?, ?, ?, ?)",
                setmealId, dishId, "dish", BigDecimal.valueOf(18.0), 1);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/setmeal/status/1?id=" + setmealId,
                HttpMethod.POST, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(0);
        assertThat(response.getBody().getMsg()).isEqualTo("套餐内包含未启售菜品，无法启售");
    }

    @Test
    void getById_returnsSetmealVOWithDishes() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmGetCat", 2, 1);
        Long setmealId = seedSetmeal("Detail Combo", categoryId, 0);
        jdbcTemplate.update("insert into setmeal_dish(setmeal_id, dish_id, name, price, copies) values (?, ?, ?, ?, ?)",
                setmealId, 1L, "dish", BigDecimal.valueOf(18.0), 1);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/setmeal/" + setmealId, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        Map data = (Map) response.getBody().get("data");
        assertThat(data.get("name")).isEqualTo("Detail Combo");
        assertThat((java.util.List) data.get("setmealDishes")).hasSize(1);
    }

    @Test
    void updateSetmeal_changesNameAndDishes() {
        String token = tokenAsOperator();
        Long categoryId = seedCategory("SmUpdCat", 2, 1);
        Long dishCategoryId = seedCategory("SmUpdDishCat", 1, 1);
        Long dishId = seedDish("Upd Combo Dish", dishCategoryId, 1);
        Long setmealId = seedSetmeal("Before Update Combo", categoryId, 0);

        SetmealDTO dto = newSetmealDto("After Update Combo", categoryId, 0, dishId);
        dto.setId(setmealId);

        HttpEntity<SetmealDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/setmeal", HttpMethod.PUT, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        String name = jdbcTemplate.queryForObject("select name from setmeal where id = ?", String.class, setmealId);
        assertThat(name).isEqualTo("After Update Combo");
    }

    private Long seedSetmeal(String name, Long categoryId, Integer status) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        org.springframework.jdbc.support.KeyHolder keyHolder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            java.sql.PreparedStatement ps = connection.prepareStatement(
                    "insert into setmeal(category_id, name, price, status, description, image, "
                            + "create_time, update_time, create_user, update_user) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, categoryId);
            ps.setString(2, name);
            ps.setBigDecimal(3, BigDecimal.valueOf(58.0));
            ps.setInt(4, status);
            ps.setString(5, "combo");
            ps.setString(6, "http://example.com/s.png");
            ps.setObject(7, now);
            ps.setObject(8, now);
            ps.setLong(9, 1L);
            ps.setLong(10, 1L);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
