package com.goldenegg.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "lottery_records")
@Data
public class LotteryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String userOpenid;
    private Integer prizeId;
    private String prizeName;
    private Boolean isWinner;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lotteryTime;

    private String exchangeCode;
    private Integer exchangeStatus;

    @Temporal(TemporalType.TIMESTAMP)
    private Date exchangeTime;

    public Date getExchangeTime() {
        return exchangeTime;
    }

    public void setExchangeTime(Date exchangeTime) {
        this.exchangeTime = exchangeTime;
    }

    public Integer getExchangeStatus() {
        return exchangeStatus;
    }

    public void setExchangeStatus(Integer exchangeStatus) {
        this.exchangeStatus = exchangeStatus;
    }

    // 为了方便，可以添加一个判断是否已兑换的方法
    public boolean isExchanged() {
        return exchangeStatus != null && exchangeStatus == 1;
    }
}