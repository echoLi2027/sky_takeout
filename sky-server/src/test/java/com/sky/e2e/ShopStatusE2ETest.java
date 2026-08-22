package com.sky.e2e;

import com.sky.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ShopStatusE2ETest extends BaseE2ETest {

    private static final String KEY = "SHOP_STATUS";

    @AfterEach
    void resetRedisKey() {
        redisTemplate.delete(KEY);
    }

    private String tokenAsOperator() {
        Long operatorId = seedEmployee("shopop", "123456", 1);
        return adminToken(operatorId);
    }

    @Test
    void setShopStatus_requiresAdminToken() {
        HttpEntity<Void> entity = new HttpEntity<>(null);
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/shop/1", HttpMethod.PUT, entity, String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void setShopStatus_thenUserReadsBackTheSameValue() {
        String token = tokenAsOperator();

        ResponseEntity<Result> setResponse = restTemplate.exchange(
                "/admin/shop/1", HttpMethod.PUT, new HttpEntity<>(authHeaders(token)), Result.class);
        assertThat(setResponse.getBody().getCode()).isEqualTo(1);

        ResponseEntity<Map> getResponse = restTemplate.getForEntity("/user/shop/1", Map.class);
        assertThat(getResponse.getBody().get("data")).isEqualTo(1);
    }

    @Test
    void userEndpoint_needsNoAuthToken() {
        String token = tokenAsOperator();
        restTemplate.exchange("/admin/shop/0", HttpMethod.PUT, new HttpEntity<>(authHeaders(token)), Result.class);

        // No auth header at all -- documents that /user/** is currently unauthenticated.
        ResponseEntity<Map> response = restTemplate.getForEntity("/user/shop/0", Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("data")).isEqualTo(0);
    }

    @Test
    void getUserShopStatus_whenRedisKeyNeverSet_currentlyThrows500() {
        // Documents a real bug found during E2E testing: user.ShopController#getShopStatus
        // does `Integer status = (Integer) redisTemplate.opsForValue().get(KEY);` then
        // `status == 1 ? ... : ...`, auto-unboxing a null Integer when the SHOP_STATUS key
        // has never been set (e.g. right after a fresh deploy, before any admin has ever
        // called PUT /admin/shop/{status}). That throws an uncaught NullPointerException,
        // which GlobalExceptionHandler has no handler for, so it falls through to Spring
        // Boot's default error response instead of a Result envelope.
        ResponseEntity<String> response = restTemplate.getForEntity("/user/shop/1", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).doesNotContain("\"code\"");
    }

    @Test
    void userEndpoint_ignoresPathVariable_status0And1ReturnSameBody() {
        String token = tokenAsOperator();
        restTemplate.exchange("/admin/shop/1", HttpMethod.PUT, new HttpEntity<>(authHeaders(token)), Result.class);

        ResponseEntity<Map> viaZero = restTemplate.getForEntity("/user/shop/0", Map.class);
        ResponseEntity<Map> viaOne = restTemplate.getForEntity("/user/shop/1", Map.class);

        assertThat(viaZero.getBody()).isEqualTo(viaOne.getBody());
    }
}
