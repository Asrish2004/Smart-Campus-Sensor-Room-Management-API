package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint
 * GET /api/v1
 *
 * Returns versioning info, admin contact, and a resource map (HATEOAS links).
 *
 * HATEOAS note: Providing hypermedia links inside responses means clients
 * can navigate the API dynamically without hard-coding every URL. This
 * reduces coupling between client and server and allows the API to evolve
 * without breaking clients that follow links rather than assuming paths.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors");
        response.put("contact", Map.of(
                "name", "Campus IT Admin",
                "email", "admin@smartcampus.ac.uk"
        ));
        response.put("resources", Map.of(
                "rooms",    "/api/v1/rooms",
                "sensors",  "/api/v1/sensors"
        ));
        response.put("links", Map.of(
                "self",         "/api/v1",
                "rooms",        "/api/v1/rooms",
                "sensors",      "/api/v1/sensors"
        ));
        return Response.ok(response).build();
    }
}
