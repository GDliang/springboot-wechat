// [file name]: AdminController.java（新建文件）
package com.goldenegg.controller;

import com.goldenegg.entity.Admin;
import com.goldenegg.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/manage")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 添加管理员（需要超级管理员权限）
     */
    @PostMapping("/add")
    public Map<String, Object> addAdmin(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            String openid = params.get("openid");
            String role = params.get("role");
            String nickname = params.get("nickname");

            if (openid == null || openid.isEmpty()) {
                result.put("success", false);
                result.put("message", "openid不能为空");
                return result;
            }

            Admin admin = adminService.addAdmin(openid, role, nickname);

            result.put("success", true);
            result.put("message", "管理员添加成功");
            result.put("data", Map.of(
                    "id", admin.getId(),
                    "openid", admin.getOpenid(),
                    "nickname", admin.getNickname(),
                    "role", admin.getRole()
            ));

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "添加失败：" + e.getMessage());
        }

        return result;
    }

    /**
     * 获取所有管理员列表
     */
    @GetMapping("/list")
    public Map<String, Object> getAdminList() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 实现获取管理员列表的逻辑

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败：" + e.getMessage());
        }

        return result;
    }
}