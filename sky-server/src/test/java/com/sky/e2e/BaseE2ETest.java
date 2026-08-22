package com.sky.e2e;

import com.sky.constant.JwtClaimsConstant;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.DigestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Base class for full HTTP-level E2E suites: real Spring context, real servlet
 * container (random port), real MySQL + Redis via Testcontainers, real interceptor/AOP
 * chain.
 *
 * Data lifecycle: HTTP calls through TestRestTemplate execute on the embedded servlet
 * container's own worker thread, not the test thread, so Spring's usual
 * @Transactional-per-test rollback (which only wraps the *test thread's* connection)
 * does not roll these writes back — the controller thread uses a different DB
 * connection entirely. So instead of relying on transactional rollback, every table
 * touched by these suites is wiped after each test via JdbcTemplate.
 */
@Testcontainers
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseE2ETest {

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("sky_take_out_test")
            .withUsername("root")
            .withPassword("root")
            .withInitScript("schema-test.sql");

    protected static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("sky.datasource.host", MYSQL::getHost);
        registry.add("sky.datasource.port", MYSQL::getFirstMappedPort);
        registry.add("sky.datasource.database", MYSQL::getDatabaseName);
        registry.add("sky.datasource.username", MYSQL::getUsername);
        registry.add("sky.datasource.password", MYSQL::getPassword);

        registry.add("sky.redis.host", REDIS::getHost);
        registry.add("sky.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected JwtProperties jwtProperties;

    @Autowired
    protected RedisTemplate<Object, Object> redisTemplate;

    @AfterEach
    void cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM setmeal_dish");
        jdbcTemplate.update("DELETE FROM dish_flavor");
        jdbcTemplate.update("DELETE FROM setmeal");
        jdbcTemplate.update("DELETE FROM dish");
        jdbcTemplate.update("DELETE FROM category");
        jdbcTemplate.update("DELETE FROM employee");
    }

    /**
     * Seeds an employee row directly via JDBC (bypassing the HTTP API) and returns its
     * generated id. Password is stored MD5-hashed, matching
     * EmployeeServiceImpl#login's comparison.
     */
    protected Long seedEmployee(String username, String rawPassword, Integer status) {
        String hashedPassword = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into employee(username, name, password, phone, sex, id_number, status, "
                            + "create_time, update_time, create_user, update_user) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, username);
            ps.setString(3, hashedPassword);
            ps.setString(4, "13800000000");
            ps.setString(5, "1");
            ps.setString(6, "110101199001011234");
            ps.setInt(7, status);
            ps.setObject(8, now);
            ps.setObject(9, now);
            ps.setLong(10, 1L);
            ps.setLong(11, 1L);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * Builds a genuine admin JWT directly (same secret/ttl the real login flow uses),
     * without exercising the login endpoint. Faster than logging in for every suite
     * that just needs *a* valid token; EmployeeE2ETest separately covers the real
     * POST /admin/employee/login flow end-to-end.
     */
    protected String adminToken(Long empId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, empId);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), jwtProperties.getAdminTtl(), claims);
    }

    protected String expiredAdminToken(Long empId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, empId);
        return JwtUtil.createJWT(jwtProperties.getAdminSecretKey(), -1000L, claims);
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(jwtProperties.getAdminTokenName(), token);
        return headers;
    }

    /**
     * Seeds a category row directly via JDBC and returns its generated id.
     */
    protected Long seedCategory(String name, Integer type, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into category(type, name, sort, status, create_time, update_time, create_user, update_user) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, type);
            ps.setString(2, name);
            ps.setInt(3, 1);
            ps.setInt(4, status);
            ps.setObject(5, now);
            ps.setObject(6, now);
            ps.setLong(7, 1L);
            ps.setLong(8, 1L);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * Seeds a dish row directly via JDBC and returns its generated id.
     */
    protected Long seedDish(String name, Long categoryId, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into dish(name, category_id, price, image, description, status, "
                            + "create_time, update_time, create_user, update_user) "
                            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setLong(2, categoryId);
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(18.0));
            ps.setString(4, "http://example.com/dish.png");
            ps.setString(5, "seeded dish");
            ps.setInt(6, status);
            ps.setObject(7, now);
            ps.setObject(8, now);
            ps.setLong(9, 1L);
            ps.setLong(10, 1L);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
