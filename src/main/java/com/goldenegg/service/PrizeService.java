package com.goldenegg.service;

import com.goldenegg.entity.Prize;
import java.util.List;
import java.util.Map;

public interface PrizeService {
    List<Prize> getAllPrizes();
    List<Prize> getActivePrizes();
    Prize savePrize(Prize prize);
    void deletePrize(Integer id);
    Prize getPrizeById(Integer id);
}