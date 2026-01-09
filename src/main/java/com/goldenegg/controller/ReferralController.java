// [file name]: ReferralController.java
package com.goldenegg.controller;

import com.goldenegg.entity.ReferralRecord;
import com.goldenegg.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/referrals")
@CrossOrigin(origins = "*")
public class ReferralController {

    @Autowired
    private ReferralService referralService;

    /**
     * 获取推荐记录列表（支持多种查询条件）
     */
    @GetMapping
    public Map<String, Object> getReferralRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String referrerPhone,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Boolean isApproved) {

        Map<String, Object> result = new HashMap<>();

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize,
                    Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<ReferralRecord> records;

            // 如果有搜索关键词，使用搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                records = referralService.searchReferralRecords(keyword, pageable);
            }
            // 如果指定了推荐人手机号
            else if (referrerPhone != null && !referrerPhone.trim().isEmpty()) {
                records = referralService.getReferralRecordsByReferrer(referrerPhone, pageable);
            }
            // 如果有审核状态筛选
            else if (verified != null || isApproved != null) {
                records = referralService.getRecordsByVerifiedAndApproved(verified, isApproved, pageable);
            }
            // 默认获取所有记录
            else {
                records = referralService.getAllRecords(pageable);
            }

            result.put("success", true);
            result.put("data", records.getContent());
            result.put("total", records.getTotalElements());
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", records.getTotalPages());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取推荐记录失败: " + e.getMessage());
            e.printStackTrace(); // 打印堆栈信息便于调试
        }

        return result;
    }

    /**
     * 获取待审核的推荐记录
     */
    @GetMapping("/pending")
    public Map<String, Object> getPendingReviewRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Map<String, Object> result = new HashMap<>();

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize,
                    Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<ReferralRecord> records = referralService.getPendingReviewRecords(pageable);

            result.put("success", true);
            result.put("data", records.getContent());
            result.put("total", records.getTotalElements());
            result.put("page", page);
            result.put("pageSize", pageSize);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取待审核记录失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 审核推荐记录
     */
    @PostMapping("/review/{id}")
    public Map<String, Object> reviewReferralRecord(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> params) {

        Map<String, Object> result = new HashMap<>();

        try {
            Boolean isApproved = (Boolean) params.get("isApproved");
            Boolean hasConsumptionRecord = (Boolean) params.get("hasConsumptionRecord");
            String remark = (String) params.get("remark");
            String verifiedBy = (String) params.get("verifiedBy");

            if (isApproved == null) {
                result.put("success", false);
                result.put("message", "审核状态不能为空");
                return result;
            }

            ReferralRecord record = referralService.reviewReferralRecord(
                    id, isApproved, hasConsumptionRecord, remark, verifiedBy);

            result.put("success", true);
            result.put("message", "审核成功");
            result.put("data", record);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "审核失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 批量审核推荐记录
     */
    @PostMapping("/batch-review")
    public Map<String, Object> batchReviewReferralRecords(
            @RequestBody Map<String, Object> params) {

        Map<String, Object> result = new HashMap<>();

        try {
            @SuppressWarnings("unchecked")
            java.util.List<Integer> recordIds = (java.util.List<Integer>) params.get("recordIds");
            Boolean isApproved = (Boolean) params.get("isApproved");
            Boolean hasConsumptionRecord = (Boolean) params.get("hasConsumptionRecord");
            String remark = (String) params.get("remark");
            String verifiedBy = (String) params.get("verifiedBy");

            if (recordIds == null || recordIds.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要审核的记录");
                return result;
            }

            int successCount = 0;
            int failCount = 0;
            java.util.List<String> failedRecords = new java.util.ArrayList<>();

            for (Integer recordId : recordIds) {
                try {
                    referralService.reviewReferralRecord(
                            recordId, isApproved, hasConsumptionRecord, remark, verifiedBy);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    failedRecords.add("记录ID:" + recordId + " - " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("message", String.format("批量审核完成，成功：%d条，失败：%d条", successCount, failCount));
            result.put("successCount", successCount);
            result.put("failCount", failCount);

            if (!failedRecords.isEmpty()) {
                result.put("failedRecords", failedRecords);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "批量审核失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 发放推荐奖励
     */
    @PostMapping("/grant-reward")
    public Map<String, Object> grantReferralReward(
            @RequestBody Map<String, Object> params) {

        Map<String, Object> result = new HashMap<>();

        try {
            String referrerPhone = (String) params.get("referrerPhone");
            Integer rewardCount = (Integer) params.get("rewardCount");
            String grantedBy = (String) params.get("grantedBy");

            if (referrerPhone == null || referrerPhone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "推荐人手机号不能为空");
                return result;
            }

            if (rewardCount == null || rewardCount <= 0) {
                result.put("success", false);
                result.put("message", "奖励次数必须大于0");
                return result;
            }

            referralService.grantReferralReward(referrerPhone, rewardCount, grantedBy);

            result.put("success", true);
            result.put("message", "奖励发放成功");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "奖励发放失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取推荐人统计信息
     */
    @GetMapping("/stats/{referrerPhone}")
    public Map<String, Object> getReferrerStats(@PathVariable String referrerPhone) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> stats = referralService.getReferrerStats(referrerPhone);
            result.put("success", true);
            result.put("data", stats);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败: " + e.getMessage());
        }

        return result;
    }
}