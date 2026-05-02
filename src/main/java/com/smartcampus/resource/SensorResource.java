package com.smartcampus.resource;

import com.smartcampus.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations
 * Base path: /api/v1/sensors
 *
 * @Consumes note (Part 3 Q1):
 * If a client sends Content-Type: text/plain or application/xml to a method
 * annotated @Consumes(MediaType.APPLICATION_JSON), JAX-RS will return
 * HTTP 415 Unsupported Media Type automatically — the runtime inspects the
 * Content-Type header, finds no matching @Consumes value, and rejects the
 * request before the method body is ever reached. The server stays safe and
 * the client receives a clear protocol-level rejection.
 *
 * @QueryParam vs path segment (Part 3 Q2):
 * Query parameters (?type=CO2) are semantically reserved for filtering,
 * sorting, and searching an existing collection. The collection /api/v1/sensors
 * represents ALL sensors; the type parameter narrows that view without
 * implying a separate resource hierarchy. A path segment approach
 * (/sensors/type/CO2) would suggest "CO2" is a first-class sub-resource,
 * which is conceptually wrong. Query params are also optional and composable
 * (e.g., ?type=CO2&status=ACTIVE), which path segments handle awkwardly.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors          - list all sensors
     * GET /api/v1/sensors?type=CO2 - filter by type
     */
    @GET
    public Collection<Sensor> getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = store.sensors.values();
        if (type == null || type.isBlank()) {
            return all;
        }
        return all.stream()
                .filter(s -> s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    /** GET /api/v1/sensors/{sensorId} - get a specific sensor */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.sensors.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found", "sensorId", sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * POST /api/v1/sensors - register a new sensor.
     * Validates that the referenced roomId exists; throws
     * LinkedResourceNotFoundException (→ 422) if not.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor id is required"))
                    .build();
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "roomId is required"))
                    .build();
        }
        // Integrity check: referenced room must exist
        if (!store.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        }
        if (store.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "Sensor '" + sensor.getId() + "' already exists"))
                    .build();
        }

        // Default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.sensors.put(sensor.getId(), sensor);

        // Link sensor to room
        store.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Initialise empty reading log
        store.readings.put(sensor.getId(), new ArrayList<>());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    /**
     * Sub-resource locator for readings history.
     * Part 4 - delegates to SensorReadingResource.
     *
     * Sub-resource locator benefit note (Part 4 Q1):
     * Rather than crowding SensorResource with all the reading CRUD paths, we
     * delegate to a dedicated SensorReadingResource. This separation of concerns
     * keeps each class small and focused. As the API grows (e.g., paginated
     * reading history, aggregations), SensorReadingResource can evolve
     * independently without touching sensor registration logic. JAX-RS handles
     * the bridging at runtime — no boilerplate routing code needed.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor exists before delegating
        if (!store.sensors.containsKey(sensorId)) {
            throw new WebApplicationException(
                    Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("error", "Sensor not found", "sensorId", sensorId))
                            .build()
            );
        }
        return new SensorReadingResource(sensorId);
    }
}
