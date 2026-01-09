package com.goldenegg.repository;

import com.goldenegg.entity.ActivityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ActivityConfigRepository extends JpaRepository<ActivityConfig, Integer> {
    Optional<ActivityConfig> findByIsActiveTrue();

    long countByIsActiveTrue();
}