// [file name]: DataInitializer.java（修改初始化逻辑）
package com.goldenegg.config;

import com.goldenegg.entity.Prize;
import com.goldenegg.entity.ActivityConfig;
import com.goldenegg.entity.Admin;
import com.goldenegg.repository.PrizeRepository;
import com.goldenegg.repository.ActivityConfigRepository;
import com.goldenegg.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private PrizeRepository prizeRepository;

    @Autowired
    private ActivityConfigRepository activityConfigRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("========== 开始初始化基础数据 ==========");

        // 1. 先初始化管理员数据（基于openid的白名单）
        if (adminRepository.count() == 0) {
            System.out.println("初始化管理员白名单数据...");

            // 注意：这里需要填入实际的微信openid
            // 您需要先运行小程序，通过微信登录获取您的openid
            // 然后替换下面的"请在这里填入您的微信openid"

            String yourOpenid = "o-nNu3aO31vnCmwlQBxPHPN59WoE"; // TODO: 需要替换

            // 如果还没有openid，先跳过管理员初始化
            if ("o-nNu3aO31vnCmwlQBxPHPN59WoE".equals(yourOpenid)) {
                System.out.println("⚠️ 警告：请先获取微信openid并替换DataInitializer.java中的yourOpenid");
                System.out.println("⚠️ 暂时跳过管理员初始化，您需要手动在数据库中添加管理员openid");
            } else {
                // 创建管理员
                Admin admin = new Admin();
                admin.setOpenid(yourOpenid);
                admin.setNickname("系统管理员");
                admin.setRole("super_admin");
                admin.setIsActive(true);
                admin.setCreatedAt(new Date());

                // 可以设置username和password（可选）
                admin.setUsername("admin");
                admin.setPassword("admin123");

                adminRepository.save(admin);

                System.out.println("✓ 管理员白名单已添加，openid: " + yourOpenid);
            }
        } else {
            System.out.println("✓ 管理员表已有数据，跳过初始化");
        }

        // 2. 检查奖品数据
        if (prizeRepository.count() > 0) {
            System.out.println("✓ 奖品表已有数据，跳过初始化");
        } else {
            System.out.println("初始化奖品数据...");

            // 插入奖品数据
            Prize[] prizes = {
                    createPrize("特等奖：iPhone 15 Pro", "", 1, 1, 0.005, 1, "最新款苹果手机", true),
                    createPrize("一等奖：iPad Air", "", 2, 2, 0.01, 1, "苹果平板电脑", true),
                    createPrize("二等奖：AirPods Pro", "", 5, 5, 0.03, 1, "无线降噪耳机", true),
                    createPrize("三等奖：小米手环8", "", 10, 10, 0.05, 1, "智能运动手环", true),
                    createPrize("幸运奖：100元话费", "", 50, 50, 0.1, 2, "手机话费充值", true),
                    createPrize("参与奖：10元红包", "", 100, 100, 0.2, 2, "微信红包", true),
                    createPrize("谢谢参与", "", 1000, 1000, 0.606, 2, "感谢参与", true)
            };

            prizeRepository.saveAll(Arrays.asList(prizes));
            System.out.println("✓ 奖品数据已插入，共" + prizes.length + "条");
        }

        // 3. 检查活动配置
        if (activityConfigRepository.count() == 0) {
            System.out.println("初始化活动配置...");

            ActivityConfig activity = new ActivityConfig();
            activity.setActivityName("砸金蛋活动");
            activity.setStartTime(new Date());
            activity.setEndTime(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000));
            activity.setDailyLimit(3);
            activity.setTotalLimit(0);
            activity.setIsActive(true);
            activity.setDescription("每日可抽奖3次，奖品丰富，快来参与！");
            activity.setCreatedAt(new Date());

            activityConfigRepository.save(activity);
            System.out.println("✓ 活动配置已插入");
        } else {
            System.out.println("✓ 活动配置表已有数据，跳过初始化");
        }

        System.out.println("========== 基础数据初始化完成 ==========");
    }

    private Prize createPrize(String name, String imageUrl, Integer totalCount,
                              Integer remainingCount, Double probability,
                              Integer type, String description, Boolean isActive) {
        Prize prize = new Prize();
        prize.setName(name);
        prize.setImageUrl(imageUrl);
        prize.setTotalCount(totalCount);
        prize.setRemainingCount(remainingCount);
        prize.setProbability(probability);
        prize.setType(type);
        prize.setDescription(description);
        prize.setIsActive(isActive);
        prize.setCreatedAt(new Date());
        prize.setUpdatedAt(new Date());
        return prize;
    }
}