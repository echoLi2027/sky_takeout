package com.sky.vo;

import lombok.Data;
import java.io.Serializable;

@Data
public class OrderStatisticsVO implements Serializable {
    //待接单数量
    private Long toBeConfirmed;

    //待派送数量
    private Long confirmed;

    //派送中数量
    private Long deliveryInProgress;

    private Long completed;

    private Long cancelled;
}
