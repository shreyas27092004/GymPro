package com.gympro.booking.dto;

import java.io.Serializable;

/**
 * ────────────────────────────────────────────────────────────────────────────
 *  NotificationEvent – shared RabbitMQ message payload
 *
 *  Copy this file into EACH producer service at:
 *    booking-service  → com.gympro.booking.dto.NotificationEvent
 *    payment-service  → com.gympro.payment.dto.NotificationEvent
 *    auth-service     → com.gympro.auth.dto.NotificationEvent
 *    notification-service → com.gympro.notification.dto.NotificationEvent
 *
 *  (Change the package declaration to match each service's package.)
 *
 *  eventType constants:
 *    BOOKING_CONFIRMED   – member gets booking confirmation email
 *    BOOKING_CANCELLED   – member gets cancellation email
 *    BOOKING_TO_TRAINER  – trainer gets new booking notification email
 *    PAYMENT_SUCCESS     – member gets payment receipt email
 *    PAYMENT_REFUND      – member gets refund email
 *    OTP_REQUESTED       – member gets password-reset OTP email
 *    PLAN_ACTIVATED      – member gets plan subscription email
 * ────────────────────────────────────────────────────────────────────────────
 */
public class NotificationEvent implements Serializable {

    // ── Event type (routing / dispatch key) ──────────────────────────────
    private String eventType;           // one of the constants above

    // ── Common ───────────────────────────────────────────────────────────
    private String recipientEmail;      // always required — who to email

    // ── In-app routing (MUST be set — notification-service uses this to
    //    persist + SSE-push the in-app notification; null = silently skipped) ──
    private Long   userId;
    private String userRole;

    // ── Admin broadcast — every ADMIN-role user ID to also notify ───────────
    private java.util.List<Long> adminUserIds;

    // ── Booking fields ───────────────────────────────────────────────────
    private Long   bookingId;
    private String trainerName;
    private String memberName;
    private String sessionDay;
    private String sessionTime;

    // ── Payment fields ───────────────────────────────────────────────────
    private Double amount;
    private String method;              // CREDIT_CARD, UPI, etc.
    private String txnId;              // transaction ID
    private String description;         // payment description

    // ── Plan fields ──────────────────────────────────────────────────────
    private String planName;
    private String startDate;
    private String endDate;

    // ── OTP fields ───────────────────────────────────────────────────────
    private String otp;

    // ── No-arg constructor (required by Jackson) ─────────────────────────
    public NotificationEvent() {}

    // ── All-arg constructor ───────────────────────────────────────────────
    public NotificationEvent(String eventType, String recipientEmail,
                              Long bookingId, Double amount, String otp,
                              String trainerName, String memberName,
                              String sessionDay, String sessionTime,
                              String description, String method, String txnId,
                              String planName, String startDate, String endDate) {
        this.eventType      = eventType;
        this.recipientEmail = recipientEmail;
        this.bookingId      = bookingId;
        this.amount         = amount;
        this.otp            = otp;
        this.trainerName    = trainerName;
        this.memberName     = memberName;
        this.sessionDay     = sessionDay;
        this.sessionTime    = sessionTime;
        this.description    = description;
        this.method         = method;
        this.txnId          = txnId;
        this.planName       = planName;
        this.startDate      = startDate;
        this.endDate        = endDate;
    }

    // ── Static factory helpers (builder-style convenience) ────────────────

    /** Booking confirmed → member */
    public static NotificationEvent bookingConfirmed(String memberEmail, Long memberId, Long bookingId,
                                                      String trainerName, String day, String time) {
        NotificationEvent e = new NotificationEvent();
        e.eventType      = "BOOKING_CONFIRMED";
        e.recipientEmail = memberEmail;
        e.userId         = memberId;
        e.userRole       = "MEMBER";
        e.bookingId      = bookingId;
        e.trainerName    = trainerName;
        e.sessionDay     = day;
        e.sessionTime    = time;
        return e;
    }

    /** Booking notification → trainer */
    public static NotificationEvent bookingToTrainer(String trainerEmail, Long trainerId, Long bookingId,
                                                      String memberName, String day, String time) {
        NotificationEvent e = new NotificationEvent();
        e.eventType      = "BOOKING_TO_TRAINER";
        e.recipientEmail = trainerEmail;
        e.userId         = trainerId;
        e.userRole       = "TRAINER";
        e.bookingId      = bookingId;
        e.memberName     = memberName;
        e.sessionDay     = day;
        e.sessionTime    = time;
        return e;
    }

    /** Booking cancelled → member */
    public static NotificationEvent bookingCancelled(String memberEmail, Long memberId, Long bookingId) {
        NotificationEvent e = new NotificationEvent();
        e.eventType      = "BOOKING_CANCELLED";
        e.recipientEmail = memberEmail;
        e.userId         = memberId;
        e.userRole       = "MEMBER";
        e.bookingId      = bookingId;
        return e;
    }

    /** Payment success → member */
    public static NotificationEvent paymentSuccess(String memberEmail, Long memberId, Double amount,
                                                    String method, String txnId, String description) {
        NotificationEvent e = new NotificationEvent();
        e.eventType      = "PAYMENT_SUCCESS";
        e.recipientEmail = memberEmail;
        e.userId         = memberId;
        e.userRole       = "MEMBER";
        e.amount         = amount;
        e.method         = method;
        e.txnId          = txnId;
        e.description    = description;
        return e;
    }

    /** Refund processed → member */
    public static NotificationEvent paymentRefund(String memberEmail, Long memberId, Double amount, String txnId) {
        NotificationEvent e = new NotificationEvent();
        e.eventType      = "PAYMENT_REFUND";
        e.recipientEmail = memberEmail;
        e.userId         = memberId;
        e.userRole       = "MEMBER";
        e.amount         = amount;
        e.txnId          = txnId;
        return e;
    }

    /** OTP requested → member */
    public static NotificationEvent otpRequested(String memberEmail, String otp) {
        NotificationEvent e = new NotificationEvent();
        e.eventType      = "OTP_REQUESTED";
        e.recipientEmail = memberEmail;
        e.otp            = otp;
        return e;
    }

    /** Plan activated → member */
    public static NotificationEvent planActivated(String memberEmail, Long memberId, String planName,
                                                   String startDate, String endDate) {
        NotificationEvent e = new NotificationEvent();
        e.eventType      = "PLAN_ACTIVATED";
        e.recipientEmail = memberEmail;
        e.userId         = memberId;
        e.userRole       = "MEMBER";
        e.planName       = planName;
        e.startDate      = startDate;
        e.endDate        = endDate;
        return e;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getEventType()      { return eventType; }
    public void   setEventType(String eventType) { this.eventType = eventType; }

    public String getRecipientEmail() { return recipientEmail; }
    public void   setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public Long   getUserId()         { return userId; }
    public void   setUserId(Long userId) { this.userId = userId; }

    public String getUserRole()       { return userRole; }
    public void   setUserRole(String userRole) { this.userRole = userRole; }

    public java.util.List<Long> getAdminUserIds()      { return adminUserIds; }
    public void                 setAdminUserIds(java.util.List<Long> adminUserIds) { this.adminUserIds = adminUserIds; }

    public Long   getBookingId()      { return bookingId; }
    public void   setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Double getAmount()         { return amount; }
    public void   setAmount(Double amount) { this.amount = amount; }

    public String getOtp()            { return otp; }
    public void   setOtp(String otp)  { this.otp = otp; }

    public String getTrainerName()    { return trainerName; }
    public void   setTrainerName(String trainerName) { this.trainerName = trainerName; }

    public String getMemberName()     { return memberName; }
    public void   setMemberName(String memberName) { this.memberName = memberName; }

    public String getSessionDay()     { return sessionDay; }
    public void   setSessionDay(String sessionDay) { this.sessionDay = sessionDay; }

    public String getSessionTime()    { return sessionTime; }
    public void   setSessionTime(String sessionTime) { this.sessionTime = sessionTime; }

    public String getDescription()    { return description; }
    public void   setDescription(String description) { this.description = description; }

    public String getMethod()         { return method; }
    public void   setMethod(String method) { this.method = method; }

    public String getTxnId()          { return txnId; }
    public void   setTxnId(String txnId) { this.txnId = txnId; }

    public String getPlanName()       { return planName; }
    public void   setPlanName(String planName) { this.planName = planName; }

    public String getStartDate()      { return startDate; }
    public void   setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate()        { return endDate; }
    public void   setEndDate(String endDate) { this.endDate = endDate; }

    @Override
    public String toString() {
        return "NotificationEvent{" +
               "eventType='" + eventType + '\'' +
               ", recipientEmail='" + recipientEmail + '\'' +
               ", bookingId=" + bookingId +
               '}';
    }
}
