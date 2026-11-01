package com.gympro.plan.repository;

import com.gympro.plan.entity.MemberFreeSessionUsage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repository for tracking non-subscribed member free session usage.
 */
public interface MemberFreeSessionUsageRepository extends JpaRepository<MemberFreeSessionUsage, Long> {

    Optional<MemberFreeSessionUsage> findByMemberId(Long memberId);

    /**
     * Row-locking variant of findByMemberId. Used inside useIncludedSession()
     * so two concurrent free-session requests for the same (non-subscribed)
     * member can't both read hasUsedFreeSession=false and both succeed.
     * Must be called inside a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM MemberFreeSessionUsage u WHERE u.memberId = :memberId")
    Optional<MemberFreeSessionUsage> findByMemberIdForUpdate(@Param("memberId") Long memberId);
}
