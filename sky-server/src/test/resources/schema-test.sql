-- Schema for the 6 implemented tables, reverse-engineered from mapper XML/annotations
-- and entity classes under sky-pojo/src/main/java/com/sky/entity. There is no sky.sql
-- in the repo to copy from (see integration-e2e-test-plan.md), so this is authored from
-- scratch and mounted into the Testcontainers MySQL instance as an init script.

CREATE TABLE employee (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL,
    name         VARCHAR(50)  NOT NULL,
    password     VARCHAR(100) NOT NULL,
    phone        VARCHAR(20),
    sex          VARCHAR(2),
    id_number    VARCHAR(30),
    status       INT          NOT NULL DEFAULT 1,
    create_time  DATETIME,
    update_time  DATETIME,
    create_user  BIGINT,
    update_user  BIGINT,
    UNIQUE KEY uk_employee_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    type         INT,
    name         VARCHAR(50) NOT NULL,
    sort         INT         NOT NULL DEFAULT 0,
    status       INT         NOT NULL DEFAULT 0,
    create_time  DATETIME,
    update_time  DATETIME,
    create_user  BIGINT,
    update_user  BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dish (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(50)    NOT NULL,
    category_id  BIGINT         NOT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    image        VARCHAR(255),
    description  VARCHAR(255),
    status       INT            NOT NULL DEFAULT 0,
    create_time  DATETIME,
    update_time  DATETIME,
    create_user  BIGINT,
    update_user  BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dish_flavor (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    dish_id BIGINT      NOT NULL,
    name    VARCHAR(50),
    value   VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE setmeal (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT         NOT NULL,
    name         VARCHAR(50)    NOT NULL,
    price        DECIMAL(10, 2) NOT NULL,
    status       INT            NOT NULL DEFAULT 0,
    description  VARCHAR(255),
    image        VARCHAR(255),
    create_time  DATETIME,
    update_time  DATETIME,
    create_user  BIGINT,
    update_user  BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE setmeal_dish (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    setmeal_id BIGINT,
    dish_id    BIGINT,
    name       VARCHAR(50),
    price      DECIMAL(10, 2),
    copies     INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
