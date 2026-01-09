// [file name]: AdminService.java（完整修改版）
package com.goldenegg.service;

import com.goldenegg.entity.Admin;
import com.goldenegg.repository.AdminRepository;
import com.goldenegg.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.Optional;

@Service
public class AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 管理员登录验证（基于openid白名单）
     */
    public Admin loginByOpenid(String openid) {
        System.out.println("AdminService - 开始查询管理员，openid: " + openid);

        // 查找有效管理员
        Optional<Admin> adminOpt = adminRepository.findByOpenidAndIsActiveTrue(openid);

        System.out.println("AdminService - 查询结果: " + (adminOpt.isPresent() ? "找到管理员" : "未找到管理员"));

        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            System.out.println("AdminService - 管理员信息: id=" + admin.getId() + ", nickname=" + admin.getNickname());

            // 更新最后登录时间
            admin.setLastLogin(new Date());
            adminRepository.save(admin);

            // 如果没有昵称，从用户表获取
            if (admin.getNickname() == null || admin.getNickname().isEmpty()) {
                userRepository.findByOpenid(openid).ifPresent(user -> {
                    admin.setNickname(user.getNickname());
                    adminRepository.save(admin);
                    System.out.println("AdminService - 从用户表更新昵称: " + user.getNickname());
                });
            }

            return admin;
        }
        System.out.println("AdminService - 未找到有效管理员");
        return null;
    }

    /**
     * 检查是否是管理员（通过openid）
     */
    public boolean isAdmin(String openid) {
        return adminRepository.existsByOpenidAndIsActiveTrue(openid);
    }

    /**
     * 根据openid获取管理员信息（新增方法）
     */
    public Optional<Admin> getAdminByOpenid(String openid) {
        return adminRepository.findByOpenid(openid);
    }

    /**
     * 获取有效管理员信息（新增方法）
     */
    public Optional<Admin> getActiveAdminByOpenid(String openid) {
        return adminRepository.findByOpenidAndIsActiveTrue(openid);
    }

    /**
     * 添加管理员（手动添加openid到白名单）
     */
    public Admin addAdmin(String openid, String role, String nickname) {
        if (adminRepository.existsByOpenid(openid)) {
            throw new RuntimeException("该openid已是管理员");
        }

        Admin admin = new Admin();
        admin.setOpenid(openid);
        admin.setRole(role != null ? role : "admin");
        admin.setNickname(nickname != null ? nickname : "管理员");
        admin.setIsActive(true);

        return adminRepository.save(admin);
    }
}