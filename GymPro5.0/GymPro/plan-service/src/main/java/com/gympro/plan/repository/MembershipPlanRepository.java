package com.gympro.plan.repository;

import com.gympro.plan.entity.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    List<MembershipPlan> findByActive(boolean active);
}
