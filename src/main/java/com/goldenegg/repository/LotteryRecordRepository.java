package com.goldenegg.repository;

import com.goldenegg.entity.LotteryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Date;
import java.util.List;

@Repository
public interface LotteryRecordRepository extends JpaRepository<LotteryRecord, Integer> {
    List<LotteryRecord> findByUserOpenidOrderByLotteryTimeDesc(String openid);

    List<LotteryRecord> findByUserOpenidAndLotteryTimeAfter(String openid, Date date);

    int countByUserOpenid(String openid);

    int countByUserOpenidAndPrizeIdIsNotNull(String openid);

    long countByLotteryTimeAfter(Date date);

}
