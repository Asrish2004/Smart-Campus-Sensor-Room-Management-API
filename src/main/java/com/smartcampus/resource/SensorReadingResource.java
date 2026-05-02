package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Part 4 - Sub-resource for sensor reading history.
 * Resolved via sub-resource locator in SensorResource.
 * Effective path: /api/v1/sensors/{sensorId}/readings
 *
 * This class is NOT registered directly — Jersey instantiates it on demand
 * when the locator method in SensorResource returns it.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full historical log for the sensor.
     */
    @GET
    public List<SensorReading> getReadings() {
        return store.readings.getOrDefault(sensorId, new ArrayList<>());
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading and updates the sensor's currentValue.
     *
     * If sensor status is MAINTENANCE → throws SensorUnavailableException (403).
     * Side-effect: updates sensor.currentValue with the new reading's value.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.sensors.get(sensorId);

        // Block readings for sensors under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        if (reading.getValue() == 0 && reading.getId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Reading value is required"))
                    .build();
        }

        // Create a proper reading with generated id and current timestamp
        SensorReading newReading = new SensorReading(reading.getValue());

        // Side-effect: update parent sensor's current value
        sensor.setCurrentValue(newReading.getValue());

        // Persist the reading
        store.readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(newReading);

        return Response.status(Response.Status.CREATED)
                .entity(Map.of(
                        "message", "Reading recorded successfully",
                        "reading", newReading,
                        "updatedSensorValue", sensor.getCurrentValue()
                ))
                .build();
    }
}
