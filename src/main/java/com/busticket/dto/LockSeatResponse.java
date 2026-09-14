package com.busticket.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LockSeatResponse {

    private boolean success;
    private String message;
    private List<Long> lockedSeatIds;
    private LocalDateTime expiresAt;
    private long remainingSeconds;

    public LockSeatResponse() {}

    public LockSeatResponse(boolean success, String message, List<Long> lockedSeatIds, LocalDateTime expiresAt, long remainingSeconds) {
        this.success = success;
        this.message = message;
        this.lockedSeatIds = lockedSeatIds;
        this.expiresAt = expiresAt;
        this.remainingSeconds = remainingSeconds;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Long> getLockedSeatIds() {
        return lockedSeatIds;
    }

    public void setLockedSeatIds(List<Long> lockedSeatIds) {
        this.lockedSeatIds = lockedSeatIds;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void setRemainingSeconds(long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }
}
