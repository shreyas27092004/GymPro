package com.gympro.plan.repository;

import com.gympro.plan.entity.MemberSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, Long> {
    List<MemberSubscription> findByMemberId(Long memberId);
    Optional<MemberSubscription> findByMemberIdAndStatus(Long memberId, String status);
}
