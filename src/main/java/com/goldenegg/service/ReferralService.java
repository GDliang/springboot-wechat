// [file name]: ReferralService.java
package com.goldenegg.service;

import com.goldenegg.entity.ReferralRecord;
import com.goldenegg.entity.User;
import com.goldenegg.repository.ReferralRecordRepository;
import com.goldenegg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReferralService {

    @Autowired
    private ReferralRecordRepository referralRecordRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建推荐记录（用户填写推荐人时调用）
     */
    @Transactional
    public ReferralRecord createReferralRecord(String referrerPhone, User referredUser) {
        // 检查是否已存在该被推荐人的记录
        ReferralRecord existingRecord = referralRecordRepository.findByReferredOpenid(referredUser.getOpenid());
        if (existingRecord != null) {
            throw new RuntimeException("该用户已被推荐过");
        }

        // 检查推荐人是否存在
        List<User> referrers = userRepository.findByPhone(referrerPhone);
        if (referrers.isEmpty()) {
            throw new RuntimeException("推荐人不存在");
        }

        // 创建推荐记录
        ReferralRecord record = new ReferralRecord();
        record.setReferrerPhone(referrerPhone);
        record.setReferredOpenid(referredUser.getOpenid());
        record.setReferredPhone(referredUser.getPhone());
        record.setReferredNickname(referredUser.getNickname());
        record.setVerified(false);  // 默认未审核
        record.setHasConsumptionRecord(false);  // 默认无消费记录
        record.setIsApproved(false);  // 默认未通过审核

        ReferralRecord savedRecord = referralRecordRepository.save(record);

        // 更新推荐人的待审核推荐数
        updateReferrerPendingCount(referrerPhone);

        return savedRecord;
    }

    /**
     * 更新推荐人待审核计数
     */
    private void updateReferrerPendingCount(String referrerPhone) {
        List<User> referrers = userRepository.findByPhone(referrerPhone);
        if (!referrers.isEmpty()) {
            User referrer = referrers.get(0);
            int pendingCount = referralRecordRepository.countPendingByReferrerPhone(referrerPhone);
            referrer.setPendingReferralCount(pendingCount);
            userRepository.save(referrer);
        }
    }

    /**
     * 审核推荐记录
     */
    @Transactional
    public ReferralRecord reviewReferralRecord(Integer recordId, Boolean isApproved,
                                               Boolean hasConsumptionRecord, String remark,
                                               String verifiedBy) {
        Optional<ReferralRecord> recordOpt = referralRecordRepository.findById(recordId);
        if (!recordOpt.isPresent()) {
            throw new RuntimeException("推荐记录不存在");
        }

        ReferralRecord record = recordOpt.get();
        record.setVerified(true);
        record.setVerifiedBy(verifiedBy);
        record.setVerifiedAt(new Date());
        record.setHasConsumptionRecord(hasConsumptionRecord != null ? hasConsumptionRecord : false);
        record.setIsApproved(isApproved != null ? isApproved : false);
        record.setRemark(remark);

        ReferralRecord updatedRecord = referralRecordRepository.save(record);

        // 更新推荐人统计数据
        if (isApproved != null && isApproved) {
            updateReferrerApprovedCount(record.getReferrerPhone());
        }

        return updatedRecord;
    }

    /**
     * 更新推荐人通过审核的推荐数
     */
    private void updateReferrerApprovedCount(String referrerPhone) {
        List<User> referrers = userRepository.findByPhone(referrerPhone);
        if (!referrers.isEmpty()) {
            User referrer = referrers.get(0);
            int approvedCount = referralRecordRepository.countApprovedByReferrerPhone(referrerPhone);
            referrer.setApprovedReferralCount(approvedCount);
            userRepository.save(referrer);
        }
    }

    /**
     * 发放推荐奖励（手动发放）
     */
    @Transactional
    public void grantReferralReward(String referrerPhone, Integer rewardCount, String grantedBy) {
        List<User> referrers = userRepository.findByPhone(referrerPhone);
        if (referrers.isEmpty()) {
            throw new RuntimeException("推荐人不存在");
        }

        User referrer = referrers.get(0);

        // 计算应奖励次数（每6个通过审核的推荐奖励1次）
        int approvedCount = referralRecordRepository.countApprovedByReferrerPhone(referrerPhone);
        int rewardableCount = approvedCount / 6;
        int alreadyRewarded = referrer.getRewardedReferralCount() != null ? referrer.getRewardedReferralCount() : 0;
        int alreadyRewardedCount = alreadyRewarded / 6;  // 转换为已奖励次数

        if (rewardableCount <= alreadyRewardedCount) {
            throw new RuntimeException("没有可发放的奖励次数");
        }

        if (rewardCount > (rewardableCount - alreadyRewardedCount)) {
            throw new RuntimeException("奖励次数超过可发放次数");
        }

        // 发放奖励
        int currentAvailable = referrer.getAvailableLotteryCount() != null ? referrer.getAvailableLotteryCount() : 0;
        referrer.setAvailableLotteryCount(currentAvailable + rewardCount);
        referrer.setRewardedReferralCount(alreadyRewarded + rewardCount * 6);  // 记录已奖励的推荐人数
        referrer.setLastReviewDate(new Date());
        userRepository.save(referrer);

        // 标记已发放奖励的记录
        markRewardGranted(referrerPhone, rewardCount * 6);

        // 记录奖励日志
        System.out.println("推荐奖励发放成功：推荐人=" + referrerPhone +
                ", 发放次数=" + rewardCount +
                ", 发放人=" + grantedBy);
    }

    /**
     * 标记已发放奖励的记录
     */
    private void markRewardGranted(String referrerPhone, int count) {
        List<ReferralRecord> approvedRecords = referralRecordRepository
                .findApprovedNotRewardedByReferrerPhone(referrerPhone);

        int marked = 0;
        for (ReferralRecord record : approvedRecords) {
            if (marked >= count) break;
            record.setRewardGranted(true);
            referralRecordRepository.save(record);
            marked++;
        }
    }

    /**
     * 获取推荐人的推荐记录（分页）
     */
    public Page<ReferralRecord> getReferralRecordsByReferrer(String referrerPhone, Pageable pageable) {
        return referralRecordRepository.findByReferrerPhone(referrerPhone, pageable);
    }

    /**
     * 获取待审核的推荐记录
     */
    public Page<ReferralRecord> getPendingReviewRecords(Pageable pageable) {
        return referralRecordRepository.findPendingReview(pageable);
    }

    /**
     * 搜索推荐记录
     */
    public Page<ReferralRecord> searchReferralRecords(String keyword, Pageable pageable) {
        return referralRecordRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * 获取推荐人统计数据
     */
    public Map<String, Object> getReferrerStats(String referrerPhone) {
        List<User> referrers = userRepository.findByPhone(referrerPhone);
        if (referrers.isEmpty()) {
            throw new RuntimeException("推荐人不存在");
        }

        User referrer = referrers.get(0);

        int totalRecords = referralRecordRepository.countByReferrerPhone(referrerPhone);
        int pendingCount = referralRecordRepository.countPendingByReferrerPhone(referrerPhone);
        int approvedCount = referralRecordRepository.countApprovedByReferrerPhone(referrerPhone);
        int rewardableCount = approvedCount / 6;
        int alreadyRewarded = referrer.getRewardedReferralCount() != null ? referrer.getRewardedReferralCount() : 0;
        int alreadyRewardedCount = alreadyRewarded / 6;
        int remainingReward = Math.max(0, rewardableCount - alreadyRewardedCount);

        Map<String, Object> stats = new HashMap<>();
        stats.put("phone", referrerPhone);
        stats.put("nickname", referrer.getNickname());
        stats.put("totalReferrals", totalRecords);
        stats.put("pendingReview", pendingCount);
        stats.put("approved", approvedCount);
        stats.put("rewardable", rewardableCount);
        stats.put("alreadyRewarded", alreadyRewarded);
        stats.put("alreadyRewardedCount", alreadyRewardedCount);
        stats.put("remainingReward", remainingReward);
        stats.put("availableLotteryCount", referrer.getAvailableLotteryCount() != null ? referrer.getAvailableLotteryCount() : 0);

        return stats;
    }

    /**
     * 获取所有推荐记录
     */
    public Page<ReferralRecord> getAllRecords(Pageable pageable) {
        return referralRecordRepository.findAll(pageable);
    }

    /**
     * 根据审核状态获取记录
     */
    public Page<ReferralRecord> getRecordsByVerified(Boolean verified, Pageable pageable) {
        if (verified == null) {
            return referralRecordRepository.findAll(pageable);
        }
        return referralRecordRepository.findByVerified(verified, pageable);
    }

    /**
     * 根据审核和通过状态获取记录
     */
    public Page<ReferralRecord> getRecordsByVerifiedAndApproved(
            Boolean verified, Boolean isApproved, Pageable pageable) {
        if (verified == null && isApproved == null) {
            return referralRecordRepository.findAll(pageable);
        } else if (verified != null && isApproved != null) {
            return referralRecordRepository.findByVerifiedAndIsApproved(verified, isApproved, pageable);
        } else if (verified != null) {
            return referralRecordRepository.findByVerified(verified, pageable);
        } else {
            // 如果只有 isApproved，需要创建一个查询
            return referralRecordRepository.findByIsApproved(isApproved, pageable);
        }
    }
}