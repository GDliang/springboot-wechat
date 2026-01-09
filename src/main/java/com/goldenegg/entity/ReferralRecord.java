// [file name]: ReferralRecord.java
package com.goldenegg.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "referral_records")
@Data
public class ReferralRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "referrer_phone", nullable = false)
    private String referrerPhone;  // 推荐人手机号

    @Column(name = "referred_openid", nullable = false)
    private String referredOpenid;  // 被推荐人openid

    @Column(name = "referred_phone", nullable = false)
    private String referredPhone;  // 被推荐人手机号

    @Column(name = "referred_nickname")
    private String referredNickname;  // 被推荐人昵称

    @Column(name = "consumption_amount", columnDefinition = "int default 0")
    private Integer consumptionAmount;  // 被推荐人消费金额（分）

    @Column(name = "verified", columnDefinition = "boolean default false")
    private Boolean verified = false;  // 是否已审核

    @Column(name = "verified_by")
    private String verifiedBy;  // 审核人

    @Column(name = "verified_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date verifiedAt;  // 审核时间

    @Column(name = "has_consumption_record", columnDefinition = "boolean default false")
    private Boolean hasConsumptionRecord = false;  // 是否有线下消费记录

    @Column(name = "is_approved", columnDefinition = "boolean default false")
    private Boolean isApproved = false;  // 是否通过审核

    @Column(name = "reward_granted", columnDefinition = "boolean default false")
    private Boolean rewardGranted = false;  // 是否已发放奖励

    @Column(columnDefinition = "text")
    private String remark;  // 审核备注

    @Column(name = "created_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}