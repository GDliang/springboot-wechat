package com.goldenegg.repository;

import com.goldenegg.entity.Prize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.repository.query.Param;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Integer> {
    List<Prize> findByIsActiveTrue();

//    @Modifying
//    @Transactional
//    @Query("UPDATE Prize p SET p.remainingCount = p.remainingCount - 1 WHERE p.id = ?1 AND p.remainingCount > 0")
//    int decreaseRemainingCount(Integer prizeId);
@Modifying
@Transactional
@Query("UPDATE Prize p SET p.remainingCount = p.remainingCount - 1 WHERE p.id = :id AND p.remainingCount > 0")
int decreaseRemainingCount(@Param("id") Integer id);
}