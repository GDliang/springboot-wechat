package com.goldenegg.repository;

import com.goldenegg.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByOpenid(String openid);
    // 按手机号查询用户
    List<User> findByPhone(String phone);

    // 检查手机号是否作为推荐人存在
    boolean existsByReferrerPhone(String referrerPhone);

    // 获取所有推荐了某个手机号的用户
    List<User> findByReferrerPhone(String referrerPhone);

    // 搜索用户（按昵称或openid）
    @Query("SELECT u FROM User u WHERE u.nickname LIKE %:keyword% OR u.openid LIKE %:keyword%")
    Page<User> findByNicknameContainingOrOpenidContaining(
            @Param("keyword") String keyword1,
            @Param("keyword") String keyword2,
            Pageable pageable);
    // 新增：按昵称、openid或手机号搜索（带分页）
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.openid) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "(u.phone IS NOT NULL AND LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 新增：按昵称、openid或手机号搜索（不分页）
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.openid) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "(u.phone IS NOT NULL AND LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchByKeyword(@Param("keyword") String keyword);

    // 新增：按手机号搜索（用于精确查找）
    List<User> findByPhoneContaining(String phone);

    // 新增：获取有手机号的用户
    @Query("SELECT u FROM User u WHERE u.phone IS NOT NULL AND u.phone != ''")
    List<User> findUsersWithPhone();



    // 获取有手机号的用户（用于排行榜）
    @Query("SELECT u FROM User u WHERE u.phone IS NOT NULL AND u.phone != '' AND u.referrerCount > 0")
    Page<User> findUsersWithPhone(Pageable pageable);

    // 获取用户的推荐用户列表
    @Query("SELECT u FROM User u WHERE u.referrerPhone = :referrerPhone ORDER BY u.createdAt DESC")
    List<User> findReferredUsers(@Param("referrerPhone") String referrerPhone);
}