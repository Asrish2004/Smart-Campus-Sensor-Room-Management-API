package com.smartcampus.model;

/**
 * Standard error response body returned by all exception mappers.
 * Ensures no raw Java stack traces are ever exposed.
 */
public class ApiError {

    private int status;
    private String error;
    private String message;
    private long timestamp;

    public ApiError() {}

    public ApiError(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }

    public void setStatus(int status) { this.status = status; }
    public void setError(String error) { this.error = error; }
    public void setMessage(String message) { this.message = message; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
