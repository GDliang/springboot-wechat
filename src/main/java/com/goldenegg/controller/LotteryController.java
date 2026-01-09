package com.goldenegg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goldenegg.config.WxConfig;
import com.goldenegg.entity.LotteryRecord;
import com.goldenegg.entity.Prize;
import com.goldenegg.entity.User;
import com.goldenegg.repository.PrizeRepository;
import com.goldenegg.repository.LotteryRecordRepository;
import com.goldenegg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.goldenegg.service.LotteryService;
import java.util.*;
import java.util.stream.Collectors;
import com.goldenegg.service.UserService;
import com.goldenegg.entity.Admin;  // 添加这行
import com.goldenegg.service.AdminService;  // 添加这行
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/lottery")
@CrossOrigin(origins = "*")
public class LotteryController {

    @Autowired
    private PrizeRepository prizeRepository;

    @Autowired
    private LotteryService lotteryService;

    @Autowired
    private LotteryRecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WxConfig wxConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;
    @Autowired  // 新增：注入AdminService
    private AdminService adminService;

    @Value("${lottery.algorithm.level-protection.level1:0.3}")
    private double level1Protection;

    @Value("${lottery.algorithm.level-protection.level2:0.5}")
    private double level2Protection;

    @Value("${lottery.algorithm.inventory-protection.low-stock-threshold:0.2}")
    private double lowStockThreshold;

    @Value("${lottery.algorithm.inventory-protection.medium-stock-threshold:0.5}")
    private double mediumStockThreshold;

    @Value("${lottery.algorithm.stages.initial:50}")
    private int initialStage;

    @Value("${lottery.algorithm.stages.middle:500}")
    private int middleStage;

    @Value("${lottery.algorithm.dynamic.base-adjustment:0.001}")
    private double baseAdjustment;

    // 微信登录接口
    @PostMapping("/wxlogin")
    public Map<String, Object> wxLogin(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String code = params.get("code");
            String nickname = params.get("nickname");
            String avatarUrl = params.get("avatarUrl");

            if (code == null || code.isEmpty()) {
                result.put("success", false);
                result.put("message", "code不能为空");
                return result;
            }

            // 调用微信API获取openid
            String url = String.format(
                    "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    wxConfig.getAppid(), wxConfig.getSecret(), code
            );

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();

            Map<String, Object> wxResult = objectMapper.readValue(responseBody, Map.class);

            if (wxResult == null || wxResult.containsKey("errcode")) {
                result.put("success", false);
                result.put("message", "微信登录失败：" + (wxResult != null ? wxResult.get("errmsg") : "未知错误"));
                return result;
            }

            String openid = (String) wxResult.get("openid");
            String sessionKey = (String) wxResult.get("session_key");

            // 保存或更新用户信息
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            User user = userOpt.orElse(new User());
            user.setOpenid(openid);

            if (nickname != null && !nickname.isEmpty()) {
                user.setNickname(nickname);
            }
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }

            user.setCreatedAt(new Date());
            userRepository.save(user);

            result.put("success", true);
            result.put("openid", openid);
            result.put("session_key", sessionKey);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败：" + e.getMessage());
        }

        return result;
    }

    // 保存用户信息
    @PostMapping("/saveUserInfo")
    public Map<String, Object> saveUserInfo(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");
            String nickname = params.get("nickname");
            String avatarUrl = params.get("avatarUrl");

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "openid不能为空");
                return result;
            }

            Optional<User> userOpt = userRepository.findByOpenid(openid);
            User user = userOpt.orElse(new User());
            user.setOpenid(openid);
            user.setNickname(nickname);
            user.setAvatarUrl(avatarUrl);
            user.setCreatedAt(new Date());
            userRepository.save(user);

            result.put("success", true);
            result.put("message", "用户信息保存成功");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败：" + e.getMessage());
        }

        return result;
    }


    // 智能动态概率抽奖算法
    private Prize smartDrawAlgorithm(List<Prize> availablePrizes, String openid) {
        if (availablePrizes == null || availablePrizes.isEmpty()) {
            return null;
        }

        // 获取总抽奖次数
        long totalDraws = recordRepository.count();

        // 获取当前用户的抽奖次数
        Optional<User> userOpt = userRepository.findByOpenid(openid);
        int userDraws = 0;
        if (userOpt.isPresent()) {
            userDraws = userOpt.get().getUsedLotteryCount() != null ? userOpt.get().getUsedLotteryCount() : 0;
        }

        // 阶段判断
        String stage = getCurrentStage(totalDraws);

        // 计算每个奖品的调整后概率
        Map<Prize, Double> adjustedProbabilities = new HashMap<>();
        double totalAdjustedProbability = 0.0;

        for (Prize prize : availablePrizes) {
            if (prize != null && prize.getProbability() != null) {
                double baseProb = prize.getProbability();

                // 1. 根据奖品等级调整
                double levelFactor = getLevelFactor(prize.getPrizeLevel(), stage, userDraws,totalDraws);

                // 2. 根据库存调整
                double inventoryFactor = getInventoryFactor(prize.getRemainingPercentage());

                // 3. 计算最终概率
                double adjustedProb = baseProb * levelFactor * inventoryFactor;

                // 4. 概率上限控制
                adjustedProb = Math.min(adjustedProb, 0.8);

                // 5. 确保概率不为负
                adjustedProb = Math.max(adjustedProb, 0.001);

                adjustedProbabilities.put(prize, adjustedProb);
                totalAdjustedProbability += adjustedProb;
            }
        }

        // 如果没有可抽奖的奖品
        if (totalAdjustedProbability <= 0) {
            return null;
        }

        // 根据调整后的概率进行抽奖
        Random random = new Random();
        double randomValue = random.nextDouble() * totalAdjustedProbability;
        double cumulative = 0.0;

        for (Map.Entry<Prize, Double> entry : adjustedProbabilities.entrySet()) {
            cumulative += entry.getValue();
            if (randomValue <= cumulative) {
                return entry.getKey();
            }
        }

        return null;
    }

    // 获取当前阶段
    private String getCurrentStage(long totalDraws) {
        if (totalDraws < initialStage) {
            return "initial";
        } else if (totalDraws < middleStage) {
            return "middle";
        } else {
            return "late";
        }
    }

    // 获取等级调整系数
    private double getLevelFactor(int prizeLevel, String stage, int userDraws,long totalDraws) {
        double factor = 1.0;

        switch (prizeLevel) {
            case 1: // 特等奖
                switch (stage) {
                    case "initial":
                        factor = level1Protection; // 初期大幅降低
                        break;
                    case "middle":
                        factor = 0.6; // 中期适当提高
                        break;
                    case "late":
                        // 后期如果库存充足，恢复正常概率
                        factor = 1.2;
                        break;
                }
                // 用户抽奖次数越多，中大奖概率适当增加（保底机制）
                factor *= (1 + userDraws * 0.0005);
                break;

            case 2: // 一等奖
                switch (stage) {
                    case "initial":
                        factor = level2Protection;
                        break;
                    case "middle":
                        factor = 0.7;
                        break;
                    case "late":
                        factor = 1.1;
                        break;
                }
                factor *= (1 + userDraws * 0.0003);
                break;

            case 3: // 二等奖
                factor = 0.9 + (totalDraws > 100 ? 0.1 : 0);
                break;

            case 4: // 三等奖
                factor = 1.0;
                break;

            default: // 其他奖品
                // 低价值奖品在后期概率降低
                factor = stage.equals("late") ? 0.8 : 1.0;
                break;
        }

        return Math.max(factor, 0.1); // 保证最低有10%的概率
    }

    // 获取库存调整系数
    private double getInventoryFactor(double remainingPercentage) {
        if (remainingPercentage <= lowStockThreshold) {
            // 库存极低，大幅降低概率（防止快速抽空）
            return 0.3;
        } else if (remainingPercentage <= mediumStockThreshold) {
            // 库存中等，适当降低概率
            return 0.7;
        } else if (remainingPercentage <= 0.8) {
            // 库存充足，正常概率
            return 1.0;
        } else {
            // 库存很多，适当提高概率
            return 1.2;
        }
    }


    // 修改后的抽奖接口核心逻辑
    @PostMapping("/draw")
    public Map<String, Object> draw(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            // 1. 检查用户是否存在
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            User user = userOpt.get();

            // 2. 检查用户剩余抽奖次数
            Integer remainingCount = user.getRemainingLotteryCount();
            if (remainingCount == null || remainingCount <= 0) {
                result.put("success", false);
                result.put("message", "抽奖次数不足");
                return result;
            }

            // 3. 获取所有有效奖品
            List<Prize> activePrizes = prizeRepository.findByIsActiveTrue();
            if (activePrizes == null) {
                activePrizes = new ArrayList<>();
            }

            // 4. 过滤出还有库存的奖品
            List<Prize> availablePrizes = activePrizes.stream()
                    .filter(p -> p != null &&
                            p.getRemainingCount() != null &&
                            p.getRemainingCount() > 0)
                    .collect(Collectors.toList());

            // 5. 使用智能算法抽奖
            Prize selectedPrize = smartDrawAlgorithm(availablePrizes, openid);

            // 6. 创建抽奖记录
            LotteryRecord record = new LotteryRecord();
            record.setUserOpenid(openid);
            record.setLotteryTime(new Date());

            if (selectedPrize != null) {
                // 减少奖品库存
                selectedPrize.setRemainingCount(selectedPrize.getRemainingCount() - 1);
                prizeRepository.save(selectedPrize);

                record.setPrizeId(selectedPrize.getId());
                record.setPrizeName(selectedPrize.getName() != null ? selectedPrize.getName() : "未知奖品");
                record.setIsWinner(true);
                record.setExchangeCode(generateExchangeCode());
            } else {
                // 未中奖
                record.setIsWinner(false);
                record.setPrizeName("谢谢参与");
            }

            recordRepository.save(record);

            // 7. 更新用户信息
            if (user.getUsedLotteryCount() == null) {
                user.setUsedLotteryCount(0);
            }
            user.setUsedLotteryCount(user.getUsedLotteryCount() + 1);

            if (user.getLotteryCount() == null) {
                user.setLotteryCount(0);
            }
            user.setLotteryCount(user.getLotteryCount() + 1);

            user.setLastLotteryDate(new Date());
            userRepository.save(user);

            // 8. 返回结果
            result.put("success", true);
            result.put("isWinner", record.getIsWinner());
            result.put("prizeName", record.getPrizeName());
            result.put("exchangeCode", record.getExchangeCode());
            result.put("remainingCount", user.getRemainingLotteryCount());

            // 添加调试信息（生产环境可以移除）
            if (selectedPrize != null) {
                result.put("prizeLevel", selectedPrize.getPrizeLevel());
                result.put("prizeInventory", selectedPrize.getRemainingCount() + "/" + selectedPrize.getTotalCount());
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "抽奖失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @GetMapping("/probabilityStats")
    public Map<String, Object> getProbabilityStats() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 获取所有奖品
            List<Prize> allPrizes = prizeRepository.findAll();

            // 2. 按等级分组统计
            Map<Integer, List<Prize>> prizesByLevel = allPrizes.stream()
                    .filter(p -> p != null && p.getIsActive() != null && p.getIsActive())
                    .collect(Collectors.groupingBy(Prize::getPrizeLevel));

            // 3. 计算各等级统计信息
            List<Map<String, Object>> levelStats = new ArrayList<>();
            for (Map.Entry<Integer, List<Prize>> entry : prizesByLevel.entrySet()) {
                int level = entry.getKey();
                List<Prize> prizes = entry.getValue();

                int totalCount = prizes.stream()
                        .mapToInt(p -> p.getTotalCount() != null ? p.getTotalCount() : 0)
                        .sum();

                int remainingCount = prizes.stream()
                        .mapToInt(p -> p.getRemainingCount() != null ? p.getRemainingCount() : 0)
                        .sum();

                double usedPercentage = totalCount > 0 ?
                        (double)(totalCount - remainingCount) / totalCount * 100 : 0;

                Map<String, Object> stat = new HashMap<>();
                stat.put("level", level);
                stat.put("levelName", getLevelName(level));
                stat.put("prizeCount", prizes.size());
                stat.put("totalCount", totalCount);
                stat.put("remainingCount", remainingCount);
                stat.put("usedPercentage", String.format("%.2f%%", usedPercentage));
                stat.put("usedCount", totalCount - remainingCount);

                levelStats.add(stat);
            }

            // 4. 获取总抽奖次数
            long totalDraws = recordRepository.count();

            // 5. 获取今日抽奖次数
            Date today = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            Date startOfDay = calendar.getTime();

            long todayDraws = recordRepository.countByLotteryTimeAfter(startOfDay);

            result.put("success", true);
            result.put("totalDraws", totalDraws);
            result.put("todayDraws", todayDraws);
            result.put("levelStats", levelStats);
            result.put("currentStage", getCurrentStage(totalDraws));

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计失败: " + e.getMessage());
        }

        return result;
    }

    // 获取等级名称
    private String getLevelName(int level) {
        switch (level) {
            case 1: return "特等奖";
            case 2: return "一等奖";
            case 3: return "二等奖";
            case 4: return "三等奖";
            case 5: return "普通奖";
            default: return "未知等级";
        }
    }

    @PostMapping("/updateUserLottery")
    public Map<String, Object> updateUserLottery(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = (String) params.get("openid");
            Integer consumption = (Integer) params.get("consumption"); // 消费金额（分）
            Integer lotteryCount = (Integer) params.get("lotteryCount"); // 直接设置抽奖次数
            Integer usedLotteryCount = (Integer) params.get("usedLotteryCount"); // 已使用抽奖次数
            String phone = (String) params.get("phone"); // 手机号
            String address = (String) params.get("address"); // 地址

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "openid不能为空");
                return result;
            }

            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (!userOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "用户不存在");
                return result;
            }

            User user = userOpt.get();

            // 更新消费金额
            if (consumption != null) {
                user.setTotalConsumption(consumption);
            }

            // 如果直接设置了抽奖次数，优先使用
            if (lotteryCount != null) {
                user.setAvailableLotteryCount(lotteryCount);
            }

            // 更新已使用抽奖次数
            if (usedLotteryCount != null && usedLotteryCount >= 0) {
                user.setUsedLotteryCount(usedLotteryCount);
            }

            // 更新手机号和地址
            if (phone != null && !phone.trim().isEmpty()) {
                if (phone.matches("^1[3-9]\\d{9}$")) {
                    user.setPhone(phone);
                    user.setHasContactInfo(true);
                } else {
                    result.put("success", false);
                    result.put("message", "手机号格式不正确");
                    return result;
                }
            }

            if (address != null && !address.trim().isEmpty()) {
                user.setAddress(address);
                user.setHasContactInfo(true);
            }

            userRepository.save(user);

            result.put("success", true);
            result.put("message", "用户信息更新成功");
            result.put("data", Map.of(
                    "remainingCount", user.getRemainingLotteryCount(),
                    "totalConsumption", user.getTotalConsumption(),
                    "phone", user.getPhone(),
                    "address", user.getAddress(),
                    "hasContactInfo", user.getHasContactInfo()
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }

        return result;
    }

    // 获取用户抽奖信息
    @GetMapping("/userLotteryInfo")
    public Map<String, Object> getUserLotteryInfo(@RequestParam String openid) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                result.put("success", true);
                result.put("totalConsumption", user.getTotalConsumption());
                result.put("availableLotteryCount", user.getAvailableLotteryCount());
                result.put("usedLotteryCount", user.getUsedLotteryCount());
                result.put("remainingCount", user.getRemainingLotteryCount());
                result.put("totalLotteryCount", user.getLotteryCount());

            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    // 获取用户抽奖记录
    @GetMapping("/records")
    public Map<String, Object> getUserRecords(@RequestParam String openid) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<LotteryRecord> records = recordRepository.findByUserOpenidOrderByLotteryTimeDesc(openid);
            result.put("success", true);
            result.put("data", records);
            result.put("count", records.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @GetMapping("/userInfo")
    public Map<String, Object> getUserInfo(@RequestParam String openid) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // 检查用户是否是推荐人（手机号是否被别人填写）
                boolean isReferrer = userRepository.existsByReferrerPhone(user.getPhone());

                result.put("success", true);
                result.put("lotteryCount", user.getLotteryCount());
                result.put("remainingCount", user.getRemainingLotteryCount());
                result.put("totalConsumption", user.getTotalConsumption());
                result.put("nickname", user.getNickname());
                result.put("avatarUrl", user.getAvatarUrl());
                result.put("availableLotteryCount", user.getAvailableLotteryCount());
                result.put("usedLotteryCount", user.getUsedLotteryCount());
                result.put("phone", user.getPhone());
                result.put("address", user.getAddress());
                result.put("hasContactInfo", user.getHasContactInfo());

                // 推荐人相关信息
                result.put("referrerPhone", user.getReferrerPhone());
                result.put("referrerCount", user.getReferrerCount());
                result.put("isReferrer", isReferrer);
                result.put("hasReferrer", user.getReferrerPhone() != null && !user.getReferrerPhone().isEmpty());

            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }

        return result;
    }

    @PostMapping("/updateContact")
    public Map<String, Object> updateUserContact(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");
            String phone = params.get("phone");
            String address = params.get("address");
            String referrerPhone = params.get("referrerPhone");

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            if (phone == null || phone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return result;
            }

            if (address == null || address.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "收件地址不能为空");
                return result;
            }

            // 验证手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                result.put("success", false);
                result.put("message", "手机号格式不正确");
                return result;
            }

            // 验证推荐人手机号格式（如果填写了）
            if (referrerPhone != null && !referrerPhone.trim().isEmpty()) {
                if (!referrerPhone.matches("^1[3-9]\\d{9}$")) {
                    result.put("success", false);
                    result.put("message", "推荐人手机号格式不正确");
                    return result;
                }

                // 检查推荐人手机号不能是自己
                if (referrerPhone.equals(phone)) {
                    result.put("success", false);
                    result.put("message", "不能将自己设置为推荐人");
                    return result;
                }

                // 检查推荐人是否存在于系统中
                List<User> referrers = userRepository.findByPhone(referrerPhone);
                if (referrers.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "推荐人手机号未在系统中注册");
                    return result;
                }
            }

            // 使用 UserService 更新用户联系方式
            User updatedUser = userService.updateUserContact(openid, phone, address, referrerPhone);

            result.put("success", true);
            result.put("message", "联系方式保存成功");
            result.put("data", Map.of(
                    "phone", updatedUser.getPhone(),
                    "address", updatedUser.getAddress(),
                    "referrerPhone", updatedUser.getReferrerPhone(),
                    "hasContactInfo", updatedUser.getHasContactInfo()
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户联系方式
     */
    @GetMapping("/getContact")
    public Map<String, Object> getUserContact(@RequestParam String openid) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            User user = userService.getUserContact(openid);

            result.put("success", true);
            result.put("data", Map.of(
                    "phone", user.getPhone(),
                    "address", user.getAddress(),
                    "hasContactInfo", user.getHasContactInfo()
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }

        return result;
    }

    // 兑换奖品接口
    @PostMapping("/exchange")
    public Map<String, Object> exchangePrize(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = (String) params.get("openid");
            Integer recordId = (Integer) params.get("recordId");

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            if (recordId == null) {
                result.put("success", false);
                result.put("message", "抽奖记录ID不能为空");
                return result;
            }

            // 查找抽奖记录
            Optional<LotteryRecord> recordOpt = recordRepository.findById(recordId);
            if (!recordOpt.isPresent()) {
                result.put("success", false);
                result.put("message", "抽奖记录不存在");
                return result;
            }

            LotteryRecord record = recordOpt.get();

            // 验证记录是否属于当前用户
            if (!record.getUserOpenid().equals(openid)) {
                result.put("success", false);
                result.put("message", "无权兑换此奖品");
                return result;
            }

            // 检查是否已中奖
            if (record.getIsWinner() == null || !record.getIsWinner()) {
                result.put("success", false);
                result.put("message", "此记录未中奖，无法兑换");
                return result;
            }

            // 检查是否已兑换
            if (record.getExchangeStatus() != null && record.getExchangeStatus() == 1) {
                result.put("success", false);
                result.put("message", "此奖品已兑换，请勿重复操作");
                return result;
            }

            // 记录兑换时间和状态
            record.setExchangeTime(new Date());
            record.setExchangeStatus(1);  // 设置为已兑换
            recordRepository.save(record);

            result.put("success", true);
            result.put("message", "兑换成功");
            result.put("prizeName", record.getPrizeName());
            result.put("exchangeCode", record.getExchangeCode());
            result.put("exchangeTime", record.getExchangeTime());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "兑换失败: " + e.getMessage());
        }

        return result;
    }

    private String generateExchangeCode() {
        return "EGG" + System.currentTimeMillis() + (new Random().nextInt(9000) + 1000);
    }

    /**
     * 检查推荐人手机号是否有效
     */
    @GetMapping("/checkReferrer")
    public Map<String, Object> checkReferrerPhone(
            @RequestParam String phone,
            @RequestParam(required = false) String openid) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 验证手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                result.put("success", false);
                result.put("exists", false);
                result.put("message", "手机号格式不正确");
                return result;
            }

            // 2. 检查是否是自己（如果提供了openid）
            if (openid != null && !openid.isEmpty()) {
                Optional<User> currentUserOpt = userRepository.findByOpenid(openid);
                if (currentUserOpt.isPresent()) {
                    User currentUser = currentUserOpt.get();
                    if (phone.equals(currentUser.getPhone())) {
                        result.put("success", false);
                        result.put("exists", false);
                        result.put("message", "不能将自己设置为推荐人");
                        return result;
                    }
                }
            }

            // 3. 使用 UserService 检查推荐人
            try {
                User referrer = userService.checkReferrerPhone(phone, openid);
                boolean exists = referrer != null;

                result.put("success", true);
                result.put("exists", exists);

                if (exists) {
                    // 返回推荐人信息
                    Map<String, Object> referrerData = new HashMap<>();
                    referrerData.put("nickname", referrer.getNickname());
                    referrerData.put("avatarUrl", referrer.getAvatarUrl());
                    referrerData.put("approvedReferralCount", referrer.getApprovedReferralCount() != null ? referrer.getApprovedReferralCount() : 0);
                    referrerData.put("referrerCount", referrer.getReferrerCount());
                    referrerData.put("lotteryCount", referrer.getLotteryCount());

                    result.put("data", referrerData);
                    result.put("message", "推荐人验证通过");
                } else {
                    result.put("message", "该手机号未注册");
                }
            } catch (RuntimeException e) {
                result.put("success", false);
                result.put("exists", false);
                result.put("message", e.getMessage());
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("exists", false);
            result.put("message", "检查失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取用户的推荐人统计信息
     */
    @GetMapping("/referrerStats")
    public Map<String, Object> getReferrerStats(@RequestParam String openid) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // 1. 获取用户作为推荐人的统计
                List<User> referredUsers = userRepository.findByReferrerPhone(user.getPhone());
                int totalReferred = referredUsers.size();

                // 2. 获取推荐的用户消费总额
                int totalReferredConsumption = referredUsers.stream()
                        .mapToInt(u -> u.getTotalConsumption() != null ? u.getTotalConsumption() : 0)
                        .sum();

                // 3. 计算推荐奖励
                int rewardCount = totalReferred;

                result.put("success", true);
                result.put("data", Map.of(
                        "totalReferred", totalReferred,
                        "totalReferredConsumption", totalReferredConsumption,
                        "rewardCount", rewardCount,
                        "referrerPhone", user.getPhone(),
                        "referredUsers", referredUsers.stream()
                                .map(u -> Map.of(
                                        "nickname", u.getNickname(),
                                        "phone", u.getPhone(),
                                        "consumption", u.getTotalConsumption(),
                                        "registerTime", u.getCreatedAt()
                                ))
                                .collect(Collectors.toList())
                ));
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取统计失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新推荐人奖励（自动发放）
     */
    @PostMapping("/updateReferrerReward")
    public Map<String, Object> updateReferrerReward(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");
            String referrerPhone = params.get("referrerPhone");

            if (openid == null || openid.isEmpty() || referrerPhone == null || referrerPhone.isEmpty()) {
                result.put("success", false);
                result.put("message", "参数错误");
                return result;
            }

            // 1. 查找推荐人
            List<User> referrerUsers = userRepository.findByPhone(referrerPhone);
            if (referrerUsers.isEmpty()) {
                result.put("success", false);
                result.put("message", "推荐人不存在");
                return result;
            }

            User referrer = referrerUsers.get(0);

            // 2. 更新推荐人奖励（每推荐1人获得1次抽奖机会）
            referrer.setAvailableLotteryCount(referrer.getAvailableLotteryCount() + 1);
            referrer.setReferrerCount(referrer.getReferrerCount() + 1);
            userRepository.save(referrer);

            result.put("success", true);
            result.put("message", "推荐奖励发放成功");
            result.put("data", Map.of(
                    "rewardCount", 1,
                    "availableLotteryCount", referrer.getAvailableLotteryCount(),
                    "referrerCount", referrer.getReferrerCount()
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "奖励发放失败: " + e.getMessage());
        }

        return result;
    }

    // 新增接口：只更新手机号和推荐人
    @PostMapping("/updatePhone")
    public Map<String, Object> updateUserPhone(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");
            String phone = params.get("phone");
            String referrerPhone = params.get("referrerPhone");

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            if (phone == null || phone.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "手机号不能为空");
                return result;
            }

            // 验证手机号格式
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                result.put("success", false);
                result.put("message", "手机号格式不正确");
                return result;
            }

            // 验证推荐人手机号（如果填写了）
            if (referrerPhone != null && !referrerPhone.trim().isEmpty()) {
                if (!referrerPhone.matches("^1[3-9]\\d{9}$")) {
                    result.put("success", false);
                    result.put("message", "推荐人手机号格式不正确");
                    return result;
                }

                if (referrerPhone.equals(phone)) {
                    result.put("success", false);
                    result.put("message", "不能将自己设置为推荐人");
                    return result;
                }

                // 检查推荐人是否存在
                List<User> referrers = userRepository.findByPhone(referrerPhone);
                if (referrers.isEmpty()) {
                    result.put("success", false);
                    result.put("message", "推荐人手机号未在系统中注册");
                    return result;
                }
            }

            // 更新用户信息
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setPhone(phone);

                // 处理推荐人逻辑
                if (referrerPhone != null && !referrerPhone.trim().isEmpty()) {
                    // 检查是否已设置过推荐人
                    boolean hasExistingReferrer = user.getReferrerPhone() != null &&
                            !user.getReferrerPhone().isEmpty();

                    if (!hasExistingReferrer) {
                        user.setReferrerPhone(referrerPhone);
                        // 调用推荐人奖励处理方法
                        userService.processReferrerReward(referrerPhone, user);
                    }
                }

                userRepository.save(user);

                result.put("success", true);
                result.put("message", "手机号保存成功");
                result.put("data", Map.of(
                        "phone", user.getPhone(),
                        "referrerPhone", user.getReferrerPhone()
                ));
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
        }

        return result;
    }


    /**
     * 管理员登录接口
     */
    @PostMapping("/admin/login")
    public Map<String, Object> adminLogin(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");

            System.out.println("管理员登录请求 - openid: " + openid);

            if (openid == null || openid.isEmpty()) {
                System.out.println("错误：openid为空");
                result.put("success", false);
                result.put("message", "openid不能为空");
                return result;
            }

            // 调用管理员服务验证（基于openid白名单）
            Admin admin = adminService.loginByOpenid(openid);

            System.out.println("管理员查询结果: " + (admin != null ? "找到管理员" : "未找到管理员"));

            if (admin != null) {
                // 生成管理员Token
                String adminToken = "admin_" + System.currentTimeMillis() + "_" + admin.getId();

                System.out.println("生成adminToken: " + adminToken);

                // 返回结果
                result.put("success", true);
                result.put("message", "管理员登录成功");
                result.put("adminToken", adminToken);
                result.put("adminInfo", Map.of(
                        "id", admin.getId(),
                        "openid", admin.getOpenid(),
                        "nickname", admin.getNickname(),
                        "role", admin.getRole()
                ));

            } else {
                System.out.println("openid不在管理员白名单中: " + openid);
                result.put("success", false);
                result.put("message", "您不是管理员，无权限访问");
            }

        } catch (Exception e) {
            System.out.println("管理员登录异常: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "登录失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 检查openid是否是管理员
     */
    @GetMapping("/admin/check")
    public Map<String, Object> checkAdmin(@RequestParam String openid) {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean isAdmin = adminService.isAdmin(openid);

            result.put("success", true);
            result.put("isAdmin", isAdmin);
            result.put("message", isAdmin ? "是管理员" : "不是管理员");

            // 如果是管理员，返回基本信息（使用getActiveAdminByOpenid方法）
            if (isAdmin) {
                adminService.getActiveAdminByOpenid(openid).ifPresent(admin -> {
                    result.put("adminInfo", Map.of(
                            "nickname", admin.getNickname(),
                            "role", admin.getRole()
                    ));
                });
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "检查失败：" + e.getMessage());
        }

        return result;
    }


    // 新增接口：只更新收货地址
    @PostMapping("/updateAddress")
    public Map<String, Object> updateUserAddress(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");
            String address = params.get("address");

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }

            if (address == null || address.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "收货地址不能为空");
                return result;
            }

            if (address.length() < 10) {
                result.put("success", false);
                result.put("message", "地址过短，请填写详细地址");
                return result;
            }

            // 更新用户信息
            Optional<User> userOpt = userRepository.findByOpenid(openid);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setAddress(address);
                user.setHasContactInfo(true); // 有地址才算完成联系方式

                userRepository.save(user);

                result.put("success", true);
                result.put("message", "收货地址保存成功");
                result.put("data", Map.of(
                        "address", user.getAddress(),
                        "hasContactInfo", user.getHasContactInfo()
                ));
            } else {
                result.put("success", false);
                result.put("message", "用户不存在");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "保存失败: " + e.getMessage());
        }

        return result;
    }
}