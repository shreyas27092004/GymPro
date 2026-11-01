package com.gympro.notification.dto;

import lombok.Data;

// DTO used for structured email requests (alternative to query params)
@Data
public class EmailRequest {
    private String to;        // recipient email
    private String subject;   // email subject line
    private String body;      // email body text
}
