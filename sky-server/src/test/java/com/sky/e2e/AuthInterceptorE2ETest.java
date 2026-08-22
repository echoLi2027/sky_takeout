package com.sky.e2e;

import com.sky.dto.EmployeeLoginDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Documents JwtTokenAdminInterceptor's current behavior against /admin/**, and the
 * current (unauthenticated) state of /user/** and /admin/employee/login, so a future
 * change to either is a visible test failure rather than a silent regression.
 */
class AuthInterceptorE2ETest extends BaseE2ETest {

    private static final String SHOP_STATUS_KEY = "SHOP_STATUS";

    @AfterEach
    void resetRedisKey() {
        redisTemplate.delete(SHOP_STATUS_KEY);
    }

    @Test
    void adminEndpoint_missingToken_isRejectedWithBareStatus401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/category/list?type=1", HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNullOrEmpty();
    }

    @Test
    void adminEndpoint_malformedToken_isRejected() {
        HttpHeaders headers = authHeaders("not-a-real-jwt");
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/category/list?type=1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminEndpoint_expiredToken_isRejected() {
        Long empId = seedEmployee("expop", "123456", 1);
        String expired = expiredAdminToken(empId);

        HttpHeaders headers = authHeaders(expired);
        ResponseEntity<String> response = restTemplate.exchange(
                "/admin/category/list?type=1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginEndpoint_isReachableWithoutToken() {
        EmployeeLoginDTO dto = new EmployeeLoginDTO();
        dto.setUsername("nosuchuser");
        dto.setPassword("whatever");

        ResponseEntity<Map> response = restTemplate.postForEntity("/admin/employee/login", dto, Map.class);

        // Interceptor excludes this path, so the request reaches the controller and
        // fails on business logic (account not found), not on auth.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("code")).isEqualTo(0);
    }

    @Test
    void userShopEndpoint_currentlyNeedsNoToken() {
        // Pre-populate the Redis key directly so this test isolates "no auth token
        // required" from the separate NPE-on-unset-key bug documented in
        // ShopStatusE2ETest#getUserShopStatus_whenRedisKeyNeverSet_currentlyThrows500.
        redisTemplate.opsForValue().set(SHOP_STATUS_KEY, 1);

        ResponseEntity<Map> response = restTemplate.getForEntity("/user/shop/1", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
