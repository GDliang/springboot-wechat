// [file name]: UserService.java
package com.goldenegg.service;

import com.goldenegg.entity.User;
import com.goldenegg.entity.LotteryRecord;
import com.goldenegg.repository.UserRepository;
import com.goldenegg.repository.LotteryRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LotteryRecordRepository lotteryRecordRepository;

    @Autowired  // 新增：注入ReferralService
    private ReferralService referralService;

    /**
     * 更新用户联系方式
     */
    @Transactional
    public User updateUserContact(String openid, String phone, String address, String referrerPhone) {
        Optional<User> userOpt = userRepository.findByOpenid(openid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPhone(phone);
            user.setAddress(address);
            user.setHasContactInfo(true);

            // 处理推荐人逻辑
            if (referrerPhone != null && !referrerPhone.trim().isEmpty()) {
                // 检查是否已设置过推荐人
                boolean hasExistingReferrer = user.getReferrerPhone() != null &&
                        !user.getReferrerPhone().isEmpty();

                if (!hasExistingReferrer) {
                    user.setReferrerPhone(referrerPhone);
                    // 调用推荐人奖励处理方法
                    processReferrerReward(referrerPhone, user);
                }
            }

            return userRepository.save(user);
        }
        throw new RuntimeException("用户不存在");
    }

    /**
     * 处理推荐人奖励 - 现在改为创建待审核的推荐记录
     */
    @Transactional
    public void processReferrerReward(String referrerPhone, User currentUser) {
        try {
            System.out.println("开始处理推荐人奖励: " + referrerPhone);
            System.out.println("当前用户: " + currentUser.getOpenid() + ", " + currentUser.getPhone());

            // 改为创建待审核的推荐记录
            com.goldenegg.entity.ReferralRecord record = referralService.createReferralRecord(referrerPhone, currentUser);

            System.out.println("推荐记录创建成功，ID: " + record.getId());
            System.out.println("等待管理员审核后发放奖励");

        } catch (Exception e) {
            System.err.println("创建推荐记录失败: " + e.getMessage());
            e.printStackTrace();
            // 不要抛出异常，以免影响主流程
        }
    }

    /**
     * 获取用户联系方式
     */
    public User getUserContact(String openid) {
        return userRepository.findByOpenid(openid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 检查推荐人手机号是否存在
     */
    public User checkReferrerPhone(String phone, String currentUserOpenid) {
        List<User> referrerUsers = userRepository.findByPhone(phone);
        if (!referrerUsers.isEmpty()) {
            User referrer = referrerUsers.get(0);

            // 检查是否是自己
            if (referrer.getOpenid().equals(currentUserOpenid)) {
                throw new RuntimeException("不能将自己设置为推荐人");
            }

            return referrer;
        }
        return null;
    }

    /**
     * 更新用户联系方式（不带推荐人）- 原有的方法
     */
//    @Transactional
//    public User updateUserContact(String openid, String phone, String address) {
//        return this.updateUserContact(openid, phone, address, null);
//    }
}