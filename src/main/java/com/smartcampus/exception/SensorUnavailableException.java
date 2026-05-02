package com.smartcampus.exception;

/**
 * Part 5.3 - Thrown when a POST reading is attempted on a sensor
 * whose status is "MAINTENANCE".
 * Mapped to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;

    public SensorUnavailableException(String sensorId) {
        super("Sensor '" + sensorId + "' is currently under MAINTENANCE and cannot accept new readings.");
        this.sensorId = sensorId;
    }

    public String getSensorId() { return sensorId; }
}
