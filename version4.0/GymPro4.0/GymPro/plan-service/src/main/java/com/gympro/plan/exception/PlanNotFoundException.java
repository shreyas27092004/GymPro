package com.gympro.plan.exception;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(Long id) {
        super("Membership plan not found with ID: " + id);
    }
}
