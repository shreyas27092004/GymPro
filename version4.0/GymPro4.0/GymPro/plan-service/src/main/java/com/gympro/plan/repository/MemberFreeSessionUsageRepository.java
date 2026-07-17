package com.gympro.plan.repository;

import com.gympro.plan.entity.MemberFreeSessionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for tracking non-subscribed member free session usage.
 */
public interface MemberFreeSessionUsageRepository extends JpaRepository<MemberFreeSessionUsage, Long> {

    Optional<MemberFreeSessionUsage> findByMemberId(Long memberId);
}
