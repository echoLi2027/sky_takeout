package com.sky.e2e;

import com.sky.dto.CategoryDTO;
import com.sky.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryE2ETest extends BaseE2ETest {

    private String tokenAsOperator() {
        Long operatorId = seedEmployee("catop", "123456", 1);
        return adminToken(operatorId);
    }

    @Test
    void save_dishCategory_defaultsToDisabled() {
        String token = tokenAsOperator();
        CategoryDTO dto = new CategoryDTO();
        dto.setType(1);
        dto.setName("Appetizers");
        dto.setSort(1);

        HttpEntity<CategoryDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/category", HttpMethod.POST, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);

        Integer status = jdbcTemplate.queryForObject("select status from category where name = ?", Integer.class, "Appetizers");
        assertThat(status).isEqualTo(0);
    }

    @Test
    void save_setmealCategory_defaultsToDisabled() {
        String token = tokenAsOperator();
        CategoryDTO dto = new CategoryDTO();
        dto.setType(2);
        dto.setName("Combo Meals");
        dto.setSort(1);

        HttpEntity<CategoryDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        ResponseEntity<Result> response = restTemplate.exchange("/admin/category", HttpMethod.POST, entity, Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        Integer type = jdbcTemplate.queryForObject("select type from category where name = ?", Integer.class, "Combo Meals");
        assertThat(type).isEqualTo(2);
    }

    @Test
    void page_filtersByNameAndType() {
        String token = tokenAsOperator();
        seedCategory("Drinks", 1, 1);
        seedCategory("Party Sets", 2, 1);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/category/page?page=1&pageSize=10&type=1",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        Map data = (Map) response.getBody().get("data");
        assertThat(((Number) data.get("total")).intValue()).isEqualTo(1);
    }

    @Test
    void update_changesName() {
        String token = tokenAsOperator();
        Long id = seedCategory("OldName", 1, 0);

        CategoryDTO dto = new CategoryDTO();
        dto.setId(id);
        dto.setType(1);
        dto.setName("NewName");
        dto.setSort(2);

        HttpEntity<CategoryDTO> entity = new HttpEntity<>(dto, authHeaders(token));
        restTemplate.exchange("/admin/category", HttpMethod.PUT, entity, Result.class);

        String name = jdbcTemplate.queryForObject("select name from category where id = ?", String.class, id);
        assertThat(name).isEqualTo("NewName");
    }

    @Test
    void deleteById_removesUnreferencedCategory() {
        String token = tokenAsOperator();
        Long id = seedCategory("Deletable", 1, 0);

        ResponseEntity<Result> response = restTemplate.exchange(
                "/admin/category?id=" + id, HttpMethod.DELETE, new HttpEntity<>(authHeaders(token)), Result.class);

        assertThat(response.getBody().getCode()).isEqualTo(1);
        Integer count = jdbcTemplate.queryForObject("select count(*) from category where id = ?", Integer.class, id);
        assertThat(count).isZero();
    }

    @Test
    void startOrStop_enablesCategory() {
        String token = tokenAsOperator();
        Long id = seedCategory("ToEnable", 1, 0);

        restTemplate.exchange(
                "/admin/category/status/1?id=" + id,
                HttpMethod.POST, new HttpEntity<>(authHeaders(token)), Result.class);

        Integer status = jdbcTemplate.queryForObject("select status from category where id = ?", Integer.class, id);
        assertThat(status).isEqualTo(1);
    }

    @Test
    void list_onlyReturnsEnabledCategoriesOfRequestedType() {
        String token = tokenAsOperator();
        seedCategory("EnabledDish", 1, 1);
        seedCategory("DisabledDish", 1, 0);
        seedCategory("EnabledSetmeal", 2, 1);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/category/list?type=1",
                HttpMethod.GET, new HttpEntity<>(authHeaders(token)), Map.class);

        List<Map> data = (List<Map>) response.getBody().get("data");
        assertThat(data).hasSize(1);
        assertThat(data.get(0).get("name")).isEqualTo("EnabledDish");
    }
}
