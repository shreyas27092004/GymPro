package com.gympro.payment.dto;

import lombok.Data;

/**
 * Body for POST /payments/refund/approve/{id} and POST /payments/refund/reject/{id}.
 * Sent by an ADMIN. `note` is optional for approve, required for reject
 * (the rejection reason shown to the member).
 */
@Data
public class RefundDecisionDto {
    private String note;
}
