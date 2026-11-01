package com.gympro.payment.dto;

import lombok.Data;

/**
 * Body for POST /payments/refund/request/{id}.
 * Sent by a MEMBER to request a refund on their own payment.
 */
@Data
public class RefundRequestDto {

    // Must match the payment's memberId — verified server-side so a member
    // cannot request a refund on someone else's payment.
    private Long memberId;

    // Why the member wants a refund. Required — shown to the admin.
    private String reason;
}
