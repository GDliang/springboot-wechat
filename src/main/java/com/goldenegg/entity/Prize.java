package com.goldenegg.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "prizes")
@Data
public class Prize {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String imageUrl;
    private Integer totalCount;
    private Integer remainingCount;
    private Double probability;
    private Integer type;
    private String region;
    private String description;
    private Boolean isActive;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    // 新增方法：判断奖品等级
    public int getPrizeLevel() {
        if (name == null) return 3; // 默认普通奖品

        String prizeName = name.trim();
        if (prizeName.contains("特等奖") || prizeName.contains("特等") || "special".equalsIgnoreCase(region)) {
            return 1; // 最高级
        } else if (prizeName.contains("一等奖") || prizeName.contains("一等")) {
            return 2; // 高级
        } else if (prizeName.contains("二等奖") || prizeName.contains("二等")) {
            return 3; // 中级
        } else if (prizeName.contains("三等奖") || prizeName.contains("三等")) {
            return 4; // 普通级
        } else {
            return 5; // 最低级（谢谢参与等）
        }
    }

    // 新增方法：获取剩余库存百分比
    public double getRemainingPercentage() {
        if (totalCount == null || totalCount <= 0) return 0;
        if (remainingCount == null) return 0;
        return (double) remainingCount / totalCount;
    }
}