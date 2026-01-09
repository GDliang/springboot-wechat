package com.goldenegg.controller;

import com.goldenegg.entity.Prize;
import com.goldenegg.service.PrizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/prizes")  // 注意：这里不要加/api前缀，因为application.yml中已经有/api了
@CrossOrigin(origins = "*")
public class PrizeController {

    @Autowired
    private PrizeService prizeService;

    @GetMapping
    public Map<String, Object> getAllPrizes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Prize> prizes = prizeService.getAllPrizes();
            result.put("success", true);
            result.put("data", prizes);
            result.put("count", prizes.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/active")
    public Map<String, Object> getActivePrizes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Prize> prizes = prizeService.getActivePrizes();
            result.put("success", true);
            result.put("data", prizes);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping
    public Map<String, Object> createPrize(@RequestBody Prize prize) {
        Map<String, Object> result = new HashMap<>();
        try {
            Prize savedPrize = prizeService.savePrize(prize);
            result.put("success", true);
            result.put("data", savedPrize);
            result.put("message", "添加成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updatePrize(@PathVariable Integer id, @RequestBody Prize prize) {
        Map<String, Object> result = new HashMap<>();
        try {
            prize.setId(id);
            Prize savedPrize = prizeService.savePrize(prize);
            result.put("success", true);
            result.put("data", savedPrize);
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deletePrize(@PathVariable Integer id) {
        Map<String, Object> result = new HashMap<>();
        try {
            prizeService.deletePrize(id);
            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> togglePrizeStatus(@PathVariable Integer id, @RequestBody Map<String, Boolean> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            Prize prize = prizeService.getPrizeById(id);
            if (prize != null) {
                prize.setIsActive(params.get("isActive"));
                prizeService.savePrize(prize);
                result.put("success", true);
                result.put("message", "状态更新成功");
            } else {
                result.put("success", false);
                result.put("message", "奖品不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }


}