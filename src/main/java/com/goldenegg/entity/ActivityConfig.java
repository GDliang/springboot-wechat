package com.goldenegg.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "activity_config")
@Data
public class ActivityConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "activity_name", nullable = false)
    private String activityName;

    @Column(name = "start_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "end_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "daily_limit", nullable = false, columnDefinition = "int default 1")
    private Integer dailyLimit = 1;

    @Column(name = "total_limit", nullable = false, columnDefinition = "int default 0")
    private Integer totalLimit = 0;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private Boolean isActive = true;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}