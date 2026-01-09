// 修改 User.java
package com.goldenegg.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String openid;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "has_contact_info")
    private Boolean hasContactInfo = false;

    // 新增：推荐人手机号
    @Column(name = "referrer_phone")
    private String referrerPhone;

    // 新增：作为推荐人的推荐计数（被多少人填写了）
    @Column(name = "referrer_count", columnDefinition = "int default 0")
    private Integer referrerCount = 0;

    // 新增：是否是推荐人（根据手机号判断）
    @Transient
    private Boolean isReferrer = false;

    // 新增：待审核推荐数
    @Column(name = "pending_referral_count", columnDefinition = "int default 0")
    private Integer pendingReferralCount = 0;

    // 新增：通过审核的推荐数（用于计算奖励）
    @Column(name = "approved_referral_count", columnDefinition = "int default 0")
    private Integer approvedReferralCount = 0;

    // 新增：上次审核时间
    @Column(name = "last_review_date")
    @Temporal(TemporalType.DATE)
    private Date lastReviewDate;

    // 新增：获取可奖励的推荐人数（每6个通过审核的可奖励1次）
    @Transient
    public Integer getRewardableReferralCount() {
        if (approvedReferralCount == null) {
            return 0;
        }
        return approvedReferralCount / 6;  // 每6个通过审核的可获得1次奖励
    }

    // 新增：获取已发放奖励的推荐人数
    @Column(name = "rewarded_referral_count", columnDefinition = "int default 0")
    private Integer rewardedReferralCount = 0;

    private String nickname;
    private String avatarUrl;
    private Integer lotteryCount;
    private Integer totalConsumption;
    private Integer availableLotteryCount;
    private Integer usedLotteryCount;

    @Temporal(TemporalType.DATE)
    private Date lastLotteryDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    // 修改 getHasContactInfo 方法
    public Boolean getHasContactInfo() {
        return hasContactInfo != null ? hasContactInfo : false;
    }

    // 计算剩余抽奖次数
    @Transient
    public Integer getRemainingLotteryCount() {
        try {
            Integer available = this.availableLotteryCount;
            Integer used = this.usedLotteryCount;

            if (available == null) available = 0;
            if (used == null) used = 0;

            return Math.max(0, available - used);
        } catch (Exception e) {
            return 0;
        }
    }

    // 新增：获取推荐人是否有效（格式检查）
    @Transient
    public boolean isValidReferrerPhone() {
        if (referrerPhone == null || referrerPhone.trim().isEmpty()) {
            return false;
        }
        // 手机号格式验证：1开头，11位数字
        return referrerPhone.matches("^1[3-9]\\d{9}$");
    }

    // 新增：推荐人相关 getter/setter
    public String getReferrerPhone() {
        return referrerPhone != null ? referrerPhone : "";
    }

    public void setReferrerPhone(String referrerPhone) {
        this.referrerPhone = referrerPhone;
    }

    public Integer getReferrerCount() {
        // 默认返回已通过审核的数量
        return approvedReferralCount != null ? approvedReferralCount :
                (referrerCount != null ? referrerCount : 0);
    }

    public void setReferrerCount(Integer referrerCount) {
        this.referrerCount = referrerCount != null ? referrerCount : 0;
    }

    // 原 getter/setter 方法保持不变
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setHasContactInfo(Boolean hasContactInfo) {
        this.hasContactInfo = hasContactInfo;
    }

    public Integer getTotalConsumption() {
        return totalConsumption != null ? totalConsumption : 0;
    }

    public void setTotalConsumption(Integer totalConsumption) {
        this.totalConsumption = totalConsumption;
        // 根据消费金额自动计算可用抽奖次数（每100元=1次）
        if (totalConsumption != null) {
            this.availableLotteryCount = totalConsumption / 10000; // 10000分=100元
        }
    }

    public Integer getAvailableLotteryCount() {
        return availableLotteryCount != null ? availableLotteryCount : 0;
    }

    public void setAvailableLotteryCount(Integer availableLotteryCount) {
        this.availableLotteryCount = availableLotteryCount;
    }

    public Integer getUsedLotteryCount() {
        return this.usedLotteryCount != null ? this.usedLotteryCount : 0;
    }

    public Integer getLotteryCount() {
        return this.lotteryCount != null ? this.lotteryCount : 0;
    }

    public void setUsedLotteryCount(Integer usedLotteryCount) {
        this.usedLotteryCount = usedLotteryCount != null ? usedLotteryCount : 0;
    }

    public void setLotteryCount(Integer lotteryCount) {
        this.lotteryCount = lotteryCount != null ? lotteryCount : 0;
    }
}