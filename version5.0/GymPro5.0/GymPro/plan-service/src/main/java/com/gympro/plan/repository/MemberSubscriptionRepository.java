package com.gympro.plan.repository;

import com.gympro.plan.entity.MemberSubscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {
    List<MemberSubscription> findByMemberId(Long memberId);
    Optional<MemberSubscription> findByMemberIdAndStatus(Long memberId, String status);

    /**
     * Row-locking variant of findByMemberId, used only where a subscription's
     * sessionsUsed counter is about to be read-then-incremented (useIncludedSession /
     * restoreFreeSession). Prevents two concurrent bookings for the same member from
     * both reading the same sessionsUsed value and double-consuming a free session.
     * Must be called inside a @Transactional method.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM MemberSubscription s WHERE s.memberId = :memberId")
    List<MemberSubscription> findByMemberIdForUpdate(@Param("memberId") Long memberId);
}
