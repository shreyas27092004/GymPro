package com.gympro.notification.dto;

import java.io.Serializable;

/**
 * NotificationEvent — shared DTO between all producer services and notification-service.
 *
 * This class must be kept in sync across:
 *   - auth-service/dto/NotificationEvent.java
 *   - booking-service/dto/NotificationEvent.java
 *   - payment-service/dto/NotificationEvent.java
 *   - notification-service/dto/NotificationEvent.java  ← this file
 *
 * New fields added:
 *   - trainerId   (for TRAINER_ASSIGNED events)
 *   - memberId    (for admin notifications)
 *   - adminUserId (for notifications targeted at admins)
 */
public class NotificationEvent implements Serializable {

    private String eventType;
    private String recipientEmail;

    // ── In-app routing ──────────────────────────────────────────────────────
    private Long   userId;       // DB id of the primary recipient
    private String userRole;     // "MEMBER" | "TRAINER" | "ADMIN"
    private java.util.List<Long> adminUserIds;  // Broadcast: every ADMIN-role user ID to also notify

    // ── Booking fields ──────────────────────────────────────────────────────
    private Long   bookingId;
    private String trainerName;
    private Long   trainerId;
    private String memberName;
    private Long   memberId;
    private String sessionDay;
    private String sessionTime;

    // ── Payment fields ──────────────────────────────────────────────────────
    private Double amount;
    private String method;
    private String txnId;
    private String description;

    // ── Plan fields ─────────────────────────────────────────────────────────
    private String planName;
    private String startDate;
    private String endDate;

    // ── OTP fields ──────────────────────────────────────────────────────────
    private String otp;

    // ── Admin-registration-approval fields ───────────────────────────────────
    private String newAdminName;   // name of the person trying to register as ADMIN
    private String newAdminEmail;  // email of the person trying to register as ADMIN

    public NotificationEvent() {}

    // ── Static factory helpers ───────────────────────────────────────────────

    public static NotificationEvent bookingConfirmed(String memberEmail, Long memberId,
                                                      Long bookingId, String trainerName,
                                                      String day, String time) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "BOOKING_CONFIRMED"; e.recipientEmail = memberEmail;
        e.userId = memberId; e.userRole = "MEMBER";
        e.bookingId = bookingId; e.trainerName = trainerName;
        e.sessionDay = day; e.sessionTime = time;
        return e;
    }

    public static NotificationEvent bookingToTrainer(String trainerEmail, Long trainerId,
                                                      Long bookingId, String memberName,
                                                      String day, String time) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "BOOKING_TO_TRAINER"; e.recipientEmail = trainerEmail;
        e.userId = trainerId; e.userRole = "TRAINER";
        e.bookingId = bookingId; e.memberName = memberName;
        e.sessionDay = day; e.sessionTime = time;
        return e;
    }

    public static NotificationEvent bookingCancelled(String memberEmail, Long memberId, Long bookingId) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "BOOKING_CANCELLED"; e.recipientEmail = memberEmail;
        e.userId = memberId; e.userRole = "MEMBER"; e.bookingId = bookingId;
        return e;
    }

    public static NotificationEvent trainerAssigned(String memberEmail, Long memberId,
                                                     Long bookingId, String trainerName) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "TRAINER_ASSIGNED"; e.recipientEmail = memberEmail;
        e.userId = memberId; e.userRole = "MEMBER";
        e.bookingId = bookingId; e.trainerName = trainerName;
        return e;
    }

    public static NotificationEvent sessionReminder(String recipientEmail, Long userId,
                                                     String userRole, Long bookingId,
                                                     String trainerName, String day, String time) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "SESSION_REMINDER"; e.recipientEmail = recipientEmail;
        e.userId = userId; e.userRole = userRole;
        e.bookingId = bookingId; e.trainerName = trainerName;
        e.sessionDay = day; e.sessionTime = time;
        return e;
    }

    public static NotificationEvent paymentSuccess(String memberEmail, Long memberId,
                                                    Double amount, String method,
                                                    String txnId, String description) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "PAYMENT_SUCCESS"; e.recipientEmail = memberEmail;
        e.userId = memberId; e.userRole = "MEMBER";
        e.amount = amount; e.method = method; e.txnId = txnId; e.description = description;
        return e;
    }

    public static NotificationEvent paymentRefund(String memberEmail, Long memberId,
                                                   Double amount, String txnId) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "PAYMENT_REFUND"; e.recipientEmail = memberEmail;
        e.userId = memberId; e.userRole = "MEMBER"; e.amount = amount; e.txnId = txnId;
        return e;
    }

    public static NotificationEvent planActivated(String memberEmail, Long memberId,
                                                   String planName, String startDate, String endDate) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "PLAN_ACTIVATED"; e.recipientEmail = memberEmail;
        e.userId = memberId; e.userRole = "MEMBER";
        e.planName = planName; e.startDate = startDate; e.endDate = endDate;
        return e;
    }

    public static NotificationEvent otpRequested(String memberEmail, String otp) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "OTP_REQUESTED"; e.recipientEmail = memberEmail; e.otp = otp;
        return e;
    }

    public static NotificationEvent adminRegistrationCode(String existingAdminEmail, String otp,
                                                            String newAdminName, String newAdminEmail) {
        NotificationEvent e = new NotificationEvent();
        e.eventType = "ADMIN_REGISTRATION_CODE"; e.recipientEmail = existingAdminEmail;
        e.otp = otp; e.newAdminName = newAdminName; e.newAdminEmail = newAdminEmail;
        return e;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getEventType()      { return eventType; }
    public void   setEventType(String v)  { this.eventType = v; }
    public String getRecipientEmail() { return recipientEmail; }
    public void   setRecipientEmail(String v) { this.recipientEmail = v; }
    public Long   getUserId()         { return userId; }
    public void   setUserId(Long v)   { this.userId = v; }
    public String getUserRole()       { return userRole; }
    public void   setUserRole(String v) { this.userRole = v; }
    public java.util.List<Long> getAdminUserIds()      { return adminUserIds; }
    public void                 setAdminUserIds(java.util.List<Long> v) { this.adminUserIds = v; }
    public Long   getBookingId()      { return bookingId; }
    public void   setBookingId(Long v) { this.bookingId = v; }
    public String getTrainerName()    { return trainerName; }
    public void   setTrainerName(String v) { this.trainerName = v; }
    public Long   getTrainerId()      { return trainerId; }
    public void   setTrainerId(Long v) { this.trainerId = v; }
    public String getMemberName()     { return memberName; }
    public void   setMemberName(String v) { this.memberName = v; }
    public Long   getMemberId()       { return memberId; }
    public void   setMemberId(Long v) { this.memberId = v; }
    public String getSessionDay()     { return sessionDay; }
    public void   setSessionDay(String v) { this.sessionDay = v; }
    public String getSessionTime()    { return sessionTime; }
    public void   setSessionTime(String v) { this.sessionTime = v; }
    public Double getAmount()         { return amount; }
    public void   setAmount(Double v) { this.amount = v; }
    public String getMethod()         { return method; }
    public void   setMethod(String v) { this.method = v; }
    public String getTxnId()          { return txnId; }
    public void   setTxnId(String v)  { this.txnId = v; }
    public String getDescription()    { return description; }
    public void   setDescription(String v) { this.description = v; }
    public String getPlanName()       { return planName; }
    public void   setPlanName(String v) { this.planName = v; }
    public String getStartDate()      { return startDate; }
    public void   setStartDate(String v) { this.startDate = v; }
    public String getEndDate()        { return endDate; }
    public void   setEndDate(String v) { this.endDate = v; }
    public String getOtp()            { return otp; }
    public void   setOtp(String v)    { this.otp = v; }
    public String getNewAdminName()   { return newAdminName; }
    public void   setNewAdminName(String v) { this.newAdminName = v; }
    public String getNewAdminEmail()  { return newAdminEmail; }
    public void   setNewAdminEmail(String v) { this.newAdminEmail = v; }

    @Override
    public String toString() {
        return "NotificationEvent{eventType='" + eventType + "', recipientEmail='" + recipientEmail +
               "', userId=" + userId + ", userRole='" + userRole + "', bookingId=" + bookingId + "}";
    }
}