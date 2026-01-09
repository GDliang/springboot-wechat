// [file name]: ReferralRecordRepository.java
package com.goldenegg.repository;

import com.goldenegg.entity.ReferralRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ReferralRecordRepository extends JpaRepository<ReferralRecord, Integer> {

    // 根据推荐人手机号查找推荐记录
    List<ReferralRecord> findByReferrerPhone(String referrerPhone);

    // 分页查询推荐记录
    Page<ReferralRecord> findByReferrerPhone(String referrerPhone, Pageable pageable);

    // 根据审核状态查找
    List<ReferralRecord> findByReferrerPhoneAndVerified(String referrerPhone, Boolean verified);

    // 根据审核通过状态查找
    List<ReferralRecord> findByReferrerPhoneAndIsApproved(String referrerPhone, Boolean isApproved);

    // 根据被推荐人openid查找
    ReferralRecord findByReferredOpenid(String referredOpenid);

    // 统计某个推荐人待审核的推荐记录数
    @Query("SELECT COUNT(r) FROM ReferralRecord r WHERE r.referrerPhone = :referrerPhone AND r.verified = false")
    int countPendingByReferrerPhone(@Param("referrerPhone") String referrerPhone);

    // 统计某个推荐人通过审核的推荐记录数
    @Query("SELECT COUNT(r) FROM ReferralRecord r WHERE r.referrerPhone = :referrerPhone AND r.isApproved = true")
    int countApprovedByReferrerPhone(@Param("referrerPhone") String referrerPhone);

    // 统计某个推荐人未发放奖励的通过审核记录数
    @Query("SELECT COUNT(r) FROM ReferralRecord r WHERE r.referrerPhone = :referrerPhone AND r.isApproved = true AND r.rewardGranted = false")
    int countApprovedNotRewardedByReferrerPhone(@Param("referrerPhone") String referrerPhone);

    // 统计某个推荐人的总推荐记录数
    @Query("SELECT COUNT(r) FROM ReferralRecord r WHERE r.referrerPhone = :referrerPhone")
    int countByReferrerPhone(@Param("referrerPhone") String referrerPhone);

    // 搜索推荐记录（按推荐人或被推荐人手机号）
    @Query("SELECT r FROM ReferralRecord r WHERE " +
            "r.referrerPhone LIKE %:keyword% OR " +
            "r.referredPhone LIKE %:keyword% OR " +
            "r.referredNickname LIKE %:keyword%")
    Page<ReferralRecord> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 获取需要审核的记录（未审核）
    @Query("SELECT r FROM ReferralRecord r WHERE r.verified = false ORDER BY r.createdAt DESC")
    Page<ReferralRecord> findPendingReview(Pageable pageable);

    // 根据是否已审核查找（分页）
    Page<ReferralRecord> findByVerified(Boolean verified, Pageable pageable);

    // 根据是否通过审核查找（分页）
    Page<ReferralRecord> findByIsApproved(Boolean isApproved, Pageable pageable);

    // 根据审核状态和是否通过查找（分页）
    Page<ReferralRecord> findByVerifiedAndIsApproved(Boolean verified, Boolean isApproved, Pageable pageable);

    // 获取未发放奖励的通过审核记录
    @Query("SELECT r FROM ReferralRecord r WHERE r.referrerPhone = :referrerPhone AND r.isApproved = true AND r.rewardGranted = false ORDER BY r.verifiedAt ASC")
    List<ReferralRecord> findApprovedNotRewardedByReferrerPhone(@Param("referrerPhone") String referrerPhone);
}