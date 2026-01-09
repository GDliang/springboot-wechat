package com.goldenegg.service.impl;

import com.goldenegg.entity.Prize;
import com.goldenegg.repository.PrizeRepository;
import com.goldenegg.service.PrizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PrizeServiceImpl implements PrizeService {

    @Autowired
    private PrizeRepository prizeRepository;

    @Override
    public List<Prize> getAllPrizes() {
        return prizeRepository.findAll();
    }

    @Override
    public List<Prize> getActivePrizes() {
        return prizeRepository.findByIsActiveTrue();
    }

    @Override
    public Prize savePrize(Prize prize) {
        if (prize.getId() == null) {
            // 新增奖品时，剩余数量等于总数量
            prize.setRemainingCount(prize.getTotalCount());
        } else {
            // 更新时，保持原有剩余数量，除非用户手动修改
            Prize existingPrize = prizeRepository.findById(prize.getId()).orElse(null);
            if (existingPrize != null && prize.getRemainingCount() == null) {
                prize.setRemainingCount(existingPrize.getRemainingCount());
            }
        }
        return prizeRepository.save(prize);
    }

    @Override
    public void deletePrize(Integer id) {
        prizeRepository.deleteById(id);
    }

    @Override
    public Prize getPrizeById(Integer id) {
        return prizeRepository.findById(id).orElse(null);
    }
}