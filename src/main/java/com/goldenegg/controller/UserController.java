package com.goldenegg.controller;

import com.goldenegg.entity.User;
import com.goldenegg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
@RestController
@RequestMapping("/admin/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 获取用户列表（分页）
    @GetMapping
    public Map<String, Object> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {

        Map<String, Object> result = new HashMap<>();

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<User> userPage;

            if (keyword != null && !keyword.trim().isEmpty()) {
                // 搜索用户（按昵称、openid或手机号）
                userPage = userRepository.searchByKeyword(keyword, pageable);
            } else {
                userPage = userRepository.findAll(pageable);
            }

            List<User> users = userPage.getContent();

            // 格式化返回数据，确保包含手机号
            List<Map<String, Object>> formattedUsers = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("openid", user.getOpenid());
                        userMap.put("nickname", user.getNickname());
                        userMap.put("avatarUrl", user.getAvatarUrl());
                        userMap.put("lotteryCount", user.getLotteryCount());
                        userMap.put("totalConsumption", user.getTotalConsumption());
                        userMap.put("availableLotteryCount", user.getAvailableLotteryCount());
                        userMap.put("usedLotteryCount", user.getUsedLotteryCount());
                        userMap.put("remainingLotteryCount", user.getRemainingLotteryCount());
                        userMap.put("lastLotteryDate", user.getLastLotteryDate());
                        userMap.put("createdAt", user.getCreatedAt());
                        // 新增字段
                        userMap.put("phone", user.getPhone() != null ? user.getPhone() : "");
                        userMap.put("address", user.getAddress() != null ? user.getAddress() : "");
                        userMap.put("hasContactInfo", user.getHasContactInfo() != null ? user.getHasContactInfo() : false);
                        return userMap;
                    })
                    .collect(Collectors.toList());

            result.put("success", true);
            result.put("data", formattedUsers);
            result.put("total", userPage.getTotalElements());
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("totalPages", userPage.getTotalPages());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户列表失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // 新增搜索方法（如果还没有的话）
    @GetMapping("/search")
    public Map<String, Object> searchUsers(@RequestParam String keyword) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<User> users = userRepository.searchByKeyword(keyword);
            List<Map<String, Object>> formattedUsers = users.stream()
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("id", user.getId());
                        userMap.put("openid", user.getOpenid());
                        userMap.put("nickname", user.getNickname());
                        userMap.put("phone", user.getPhone());
                        userMap.put("totalConsumption", user.getTotalConsumption());
                        userMap.put("remainingLotteryCount", user.getRemainingLotteryCount());
                        return userMap;
                    })
                    .collect(Collectors.toList());

            result.put("success", true);
            result.put("data", formattedUsers);
            result.put("count", formattedUsers.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
        }

        return result;
    }

    // 获取用户详情
    @GetMapping("/{openid}")
    public Map<String, Object> getUserDetail(@PathVariable String openid) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                result.put("success", true);
                result.put("data", user);
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取用户详情失败: " + e.getMessage());
        }

        return result;
    }

    // 批量更新用户抽奖次数
    @PostMapping("/batch-update")
    public Map<String, Object> batchUpdateUserLottery(@RequestBody List<Map<String, Object>> updates) {
        Map<String, Object> result = new HashMap<>();

        try {
            int successCount = 0;
            int failCount = 0;
            List<String> failedUsers = new ArrayList<>();

            for (Map<String, Object> update : updates) {
                String openid = (String) update.get("openid");
                Integer consumption = (Integer) update.get("consumption");
                Integer lotteryCount = (Integer) update.get("lotteryCount");
                String phone = (String) update.get("phone");
                String address = (String) update.get("address");

                if (openid != null) {
                    Optional<User> userOpt = userRepository.findByOpenid(openid);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        if (consumption != null) {
                            user.setTotalConsumption(consumption);
                        }
                        if (lotteryCount != null) {
                            user.setAvailableLotteryCount(lotteryCount);
                        }
                        if (phone != null && !phone.trim().isEmpty()) {
                            user.setPhone(phone);
                        }
                        if (address != null && !address.trim().isEmpty()) {
                            user.setAddress(address);
                        }

                        userRepository.save(user);
                        successCount++;
                    } else {
                        failCount++;
                        failedUsers.add(openid);
                    }
                }
            }

            result.put("success", true);
            result.put("message", String.format("批量更新完成，成功：%d条，失败：%d条", successCount, failCount));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            if (!failedUsers.isEmpty()) {
                result.put("failedUsers", failedUsers);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "批量更新失败: " + e.getMessage());
        }

        return result;
    }

    // 重置用户抽奖次数
    @PostMapping("/reset-lottery")
    public Map<String, Object> resetUserLottery(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");
            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "openid不能为空");
                return result;
            }

            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setUsedLotteryCount(0);
                userRepository.save(user);

                result.put("success", true);
                result.put("message", "重置成功");
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重置失败: " + e.getMessage());
        }

        return result;
    }

    // 获取用户统计信息
    @GetMapping("/statistics")
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> result = new HashMap<>();

        try {
            List<User> users = userRepository.findAll();

            long totalUsers = users.size();
            long activeUsers = users.stream()
                    .filter(u -> u.getLotteryCount() != null && u.getLotteryCount() > 0)
                    .count();
            long totalLotteryCount = users.stream()
                    .mapToLong(u -> u.getLotteryCount() != null ? u.getLotteryCount() : 0)
                    .sum();
            long totalConsumption = users.stream()
                    .mapToLong(u -> u.getTotalConsumption() != null ? u.getTotalConsumption() : 0)
                    .sum();

            result.put("success", true);
            result.put("data", Map.of(
                    "totalUsers", totalUsers,
                    "activeUsers", activeUsers,
                    "totalLotteryCount", totalLotteryCount,
                    "totalConsumption", totalConsumption / 100.0,
                    "avgConsumptionPerUser", totalConsumption / Math.max(1, totalUsers) / 100.0
            ));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计信息失败: " + e.getMessage());
        }

        return result;
    }
    /**
     * 获取推荐人排行榜
     */
    @GetMapping("/referrerRank")
    public Map<String, Object> getReferrerRank(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Map<String, Object> result = new HashMap<>();

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "referrerCount"));

            // 只查询有推荐记录的用户
            Page<User> userPage = userRepository.findUsersWithPhone(pageable);

            List<Map<String, Object>> rankList = userPage.getContent().stream()
                    .filter(u -> u.getReferrerCount() != null && u.getReferrerCount() > 0)
                    .map(user -> {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("nickname", user.getNickname());
                        userMap.put("phone", maskPhone(user.getPhone())); // 脱敏显示
                        userMap.put("referrerCount", user.getReferrerCount());
                        userMap.put("avatarUrl", user.getAvatarUrl());
                        userMap.put("totalReward", user.getAvailableLotteryCount()); // 总奖励次数
                        return userMap;
                    })
                    .collect(Collectors.toList());

            result.put("success", true);
            result.put("data", rankList);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("total", userPage.getTotalElements());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取排行榜失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户的推荐记录（用于用户管理界面）
     */
    @GetMapping("/{openid}/referrals")
    public Map<String, Object> getUserReferralRecords(
            @PathVariable String openid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Map<String, Object> result = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            User user = userOpt.get();
            String phone = user.getPhone();

            if (phone == null || phone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "用户未绑定手机号");
                return result;
            }

            // 使用ReferralService获取推荐记录
            // 这里需要注入ReferralService
            // 或者直接调用ReferralRecordRepository

            result.put("success", true);
            result.put("data", new HashMap<String, Object>() {{
                put("userInfo", user);
                put("referralRecords", new ArrayList<>()); // 这里返回推荐记录
                put("totalRecords", 0);
                put("approvedRecords", 0);
                put("pendingRecords", 0);
            }});

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取推荐记录失败: " + e.getMessage());
        }

        return result;
    }

    // 手机号脱敏方法
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}