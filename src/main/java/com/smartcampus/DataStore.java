package com.smartcampus;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton, thread-safe in-memory data store.
 *
 * Because JAX-RS Resource classes are request-scoped (new instance per request),
 * shared data must live outside them. We use ConcurrentHashMap here so that
 * concurrent requests can read/write without corrupting state (no race conditions).
 *
 * We seed a few demo objects so the API is immediately usable on first run.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    // Primary stores
    public final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    // sensorId -> list of readings
    public final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        seed();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    /** Pre-populate with demo data so curl commands work immediately */
    private void seed() {
        // Rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Lab A", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        // Sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 410.0, "LIB-301");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "LAB-101");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add(s1.getId());
        r1.getSensorIds().add(s2.getId());
        r2.getSensorIds().add(s3.getId());

        // Seed a reading for TEMP-001
        readings.put("TEMP-001", new ArrayList<>());
        readings.put("CO2-001", new ArrayList<>());
        readings.put("OCC-001", new ArrayList<>());
    }
}
