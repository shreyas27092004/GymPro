package com.gympro.auth.exception;

// Thrown at login when a TRAINER account exists but its trainer-service
// profile is not yet ACTIVE (still PENDING admin approval, REJECTED, or
// deactivated/INACTIVE).
public class TrainerNotApprovedException extends RuntimeException {

    public TrainerNotApprovedException(String message) {
        super(message);
    }

    public static TrainerNotApprovedException forStatus(String status) {
        String message;
        if ("REJECTED".equalsIgnoreCase(status)) {
            message = "Your trainer application was rejected by an admin. Please contact the gym for details.";
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            message = "Your trainer account has been deactivated by an admin.";
        } else {
            // PENDING, null, or anything unexpected
            message = "Your trainer account is pending admin approval. You'll be able to log in once an admin approves it.";
        }
        return new TrainerNotApprovedException(message);
    }
}
