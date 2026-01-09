package com.goldenegg.service;

import com.goldenegg.entity.LotteryRecord;
import com.goldenegg.entity.Prize;
import com.goldenegg.entity.User;
import com.goldenegg.repository.LotteryRecordRepository;
import com.goldenegg.repository.PrizeRepository;
import com.goldenegg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class LotteryService {

    @Autowired
    private PrizeRepository prizeRepository;

    @Autowired
    private LotteryRecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;


    // 新增：直接设置用户抽奖次数（管理员用）
    @Transactional
    public void setUserLotteryCount(String openid, Integer availableCount) {
        User user = userRepository.findByOpenid(openid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (availableCount != null && availableCount >= 0) {
            user.setAvailableLotteryCount(availableCount);
            userRepository.save(user);
        }
    }

    // 新增：获取用户剩余抽奖次数
    public Integer getRemainingLotteryCount(String openid) {
        User user = userRepository.findByOpenid(openid)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return user.getRemainingLotteryCount();
    }
}