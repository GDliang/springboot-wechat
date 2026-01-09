// [file name]: AdminRepository.java
package com.goldenegg.repository;

import com.goldenegg.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {
    // 根据openid查找有效管理员
    Optional<Admin> findByOpenidAndIsActiveTrue(String openid);

    // 根据openid查找（不限制状态）
    Optional<Admin> findByOpenid(String openid);

    // 检查openid是否在管理员表中
    boolean existsByOpenid(String openid);

    // 检查openid是否是有效管理员
    boolean existsByOpenidAndIsActiveTrue(String openid);

    // 根据用户名查找（可选）
    Optional<Admin> findByUsername(String username);

    // 根据用户名查找有效管理员（可选）
    Optional<Admin> findByUsernameAndIsActiveTrue(String username);
}