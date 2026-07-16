package com.gympro.notification.service;

import com.gympro.notification.exception.EmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

// ✅ EmailService – sends REAL emails via Gmail SMTP
// Uses MimeMessage for HTML-formatted emails (looks professional)
@Slf4j
@Service
public class EmailService {

    // JavaMailSender is auto-configured by Spring Boot
    // from the spring.mail.* properties in application.properties
    @Autowired
    private JavaMailSender mailSender;

    // Sender email from application.properties
    @Value("${spring.mail.username}")
    private String fromEmail;

    // ─── MAIN METHOD ──────────────────────────────────────────────────────

    // Sends a plain-text email
    // Called from NotificationController when Feign clients call /notify/send
    public void sendEmail(String to, String subject, String body) {

        // Validate inputs before trying to send
        validateEmailInput(to, subject, body);

        try {
            // MimeMessage = email object (like composing in Gmail)
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            // MimeMessageHelper = makes it easier to set To, Subject, Body
            // 2nd param = true means we're sending HTML content
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);          // From: your Gmail
            helper.setTo(to);                   // To: recipient
            helper.setSubject(subject);         // Subject line
            helper.setText(buildHtmlBody(subject, body), true);  // HTML body

            // Actually send the email
            mailSender.send(mimeMessage);

            log.info("✅ Email sent to: {} | Subject: {}", to, subject);

        } catch (MessagingException e) {
            // MessagingException = problem creating/formatting the email
            log.error("❌ Failed to create email for {}: {}", to, e.getMessage());
            throw new EmailException("Failed to create email: " + e.getMessage(), e);

        } catch (MailException e) {
            // MailException = problem connecting to Gmail SMTP / authentication failed
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
            throw new EmailException("Failed to send email to " + to + ": " + e.getMessage(), e);
        }
    }

    // ─── SPECIFIC EMAIL TEMPLATES ─────────────────────────────────────────

    // Called when a booking is created – sent to the MEMBER
    public void sendBookingConfirmationToMember(String to, Long bookingId,
                                                 String trainerName, String day, String time) {
        String subject = "GymPro – Session Booked Successfully ✅";
        String body =
        	    "Your training session has been confirmed!\n\n" +
        	    "Booking ID  : #" + bookingId + "\n" +
        	    "Trainer     : " + trainerName + "\n" +
        	    "Day         : " + day + "\n" +
        	    "Time        : " + time + "\n\n" +
        	    "Please arrive 5 minutes early. See you at the gym! 💪";
        sendEmail(to, subject, body);
    }

    // Called when a booking is created – sent to the TRAINER
    public void sendBookingNotificationToTrainer(String to, Long bookingId,
                                                  String memberName, String day, String time) {
        String subject = "GymPro – New Session Booking";
        String body =
        	    "You have a new training session booking!\n\n" +
        	    "Booking ID  : #" + bookingId + "\n" +
        	    "Member      : " + memberName + "\n" +
        	    "Day         : " + day + "\n" +
        	    "Time        : " + time + "\n\n" +
        	    "Please confirm availability. Thank you!";
        sendEmail(to, subject, body);
    }

    // Called when a booking is cancelled
    public void sendCancellationEmail(String to, Long bookingId) {
        String subject = "GymPro – Booking Cancelled";
        String body =
        	    "Your booking #" + bookingId + " has been cancelled.\n\n" +
        	    "If this was a mistake, please re-book through the app.\n" +
        	    "We hope to see you soon! 🏋️";
        sendEmail(to, subject, body);
    }

    // Called after a successful payment
    public void sendPaymentReceipt(String to, Double amount, String method,
                                    String txnId, String description) {
        String subject = "GymPro – Payment Receipt 💳";
        String body =
        	    "Thank you for your payment!\n\n" +
        	    "Amount      : ₹" + String.format("%.2f", amount) + "\n" +
        	    "Method      : " + method + "\n" +
        	    "Transaction : " + txnId + "\n" +
        	    "Description : " + description + "\n\n" +
        	    "This is your official payment receipt. Keep it safe.\n" +
        	    "GymPro Team 💪";
        sendEmail(to, subject, body);
    }

    // Called when a refund is processed by ADMIN
    public void sendRefundEmail(String to, Double amount, String txnId) {
        String subject = "GymPro – Refund Processed 💰";
        String body =
        	    "Your refund has been processed!\n\n" +
        	    "Refund Amount : ₹" + String.format("%.2f", amount) + "\n" +
        	    "Transaction   : " + txnId + "\n\n" +
        	    "The amount will reflect in 3-5 business days.\n" +
        	    "GymPro Team";
        sendEmail(to, subject, body);
    }

    // Called when a member subscribes to a plan
    public void sendPlanSubscriptionEmail(String to, String planName,
                                           String startDate, String endDate) {
        String subject = "GymPro – Plan Activated! 🎉";
        String body =
        	    "Your membership plan has been activated!\n\n" +
        	    "Plan        : " + planName + "\n" +
        	    "Start Date  : " + startDate + "\n" +
        	    "End Date    : " + endDate + "\n\n" +
        	    "Welcome to GymPro! Make the most of your membership. 💪\n" +
        	    "GymPro Team";
        sendEmail(to, subject, body);
    }

    // Sends the OTP code for password reset — called by auth-service via Feign
    public void sendOtpEmail(String to, String otp) {
        String subject = "GymPro – Password Reset OTP 🔐";
        String body =
            "You requested a password reset for your GymPro account.\n\n" +
            "Your One-Time Password (OTP) is:\n\n" +
            "                 " + otp + "\n\n" +
            "This code is valid for 10 minutes.\n" +
            "Do NOT share this code with anyone.\n\n" +
            "If you did not request this, you can safely ignore this email.\n\n" +
            "GymPro Team 💪";
        sendEmail(to, subject, body);
    }

    // Sent to an EXISTING admin when someone tries to register a NEW admin account —
    // called by auth-service via RabbitMQ. The existing admin reads this code out to
    // the person registering; they must enter it to complete their signup.
    public void sendAdminRegistrationCodeEmail(String to, String newAdminName,
                                                String newAdminEmail, String otp) {
        String subject = "GymPro – New Admin Registration Approval Needed 🔐";
        String body =
            "Someone is trying to register a new ADMIN account on GymPro:\n\n" +
            "Name  : " + newAdminName + "\n" +
            "Email : " + newAdminEmail + "\n\n" +
            "If you recognize and approve this person, share the verification code below with them. " +
            "They must enter it to finish creating their admin account:\n\n" +
            "                 " + otp + "\n\n" +
            "This code is valid for 10 minutes.\n" +
            "If you do NOT recognize this request, ignore this email — no account will be created without the code.\n\n" +
            "GymPro Team 💪";
        sendEmail(to, subject, body);
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────

    // Input validation before sending
    private void validateEmailInput(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email cannot be empty");
        }
        if (!to.contains("@")) {
            throw new IllegalArgumentException("Invalid email address: " + to);
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email subject cannot be empty");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Email body cannot be empty");
        }
    }

    // Wraps plain text in a branded HTML template matching GymPro's dark theme
    private String buildHtmlBody(String subject, String plainText) {
        String htmlBody = plainText.replace("\n", "<br>");

        return "<!DOCTYPE html>" +
               "<html><body style='font-family:Arial,sans-serif;margin:0;padding:0;background:#0d1117;'>" +
               "<div style='max-width:600px;margin:30px auto;background:#161b22;border-radius:12px;" +
               "overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.4);border:1px solid #30363d;'>" +

               // Header with GymPro cyan accent
               "<div style='background:linear-gradient(135deg,#0f1923 0%,#0a1628 100%);padding:28px 32px;" +
               "border-bottom:2px solid #00e5a0;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0'><tr>" +
               "<td><div style='font-size:26px;font-weight:900;color:#00e5a0;letter-spacing:3px;'>GYMPRO</div>" +
               "<div style='color:#8b949e;font-size:11px;letter-spacing:2px;margin-top:3px;'>GYM MANAGEMENT SYSTEM</div></td>" +
               "<td align='right'><div style='background:rgba(0,229,160,0.12);border:1px solid rgba(0,229,160,0.3);" +
               "border-radius:20px;padding:6px 14px;font-size:11px;color:#00e5a0;font-weight:700;'>OFFICIAL MAIL</div></td>" +
               "</tr></table></div>" +

               // Subject bar
               "<div style='background:#1c2128;padding:20px 32px;border-bottom:1px solid #30363d;'>" +
               "<div style='font-size:18px;font-weight:700;color:#e6edf3;'>" + subject + "</div></div>" +

               // Body
               "<div style='padding:28px 32px;'>" +
               "<p style='color:#c9d1d9;line-height:1.8;font-size:15px;margin:0;'>" + htmlBody + "</p>" +
               "</div>" +

               // Divider
               "<div style='height:1px;background:linear-gradient(to right,transparent,#30363d,transparent);margin:0 32px;'></div>" +

               // Footer
               "<div style='padding:20px 32px;'>" +
               "<p style='color:#484f58;font-size:12px;margin:0;line-height:1.6;'>" +
               "This is an automated email from <strong style='color:#6e7681;'>GymPro</strong>. Please do not reply.<br>" +
               "&copy; 2025 GymPro. All rights reserved." +
               "</p></div>" +

               "</div></body></html>";
    }
}
