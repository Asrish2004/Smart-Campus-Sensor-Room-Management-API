package com.smartcampus;

import com.smartcampus.exception.*;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * JAX-RS Application configuration.
 *
 * Lifecycle note (answers Part 1 Q1):
 * By default JAX-RS creates a NEW instance of each Resource class per request
 * (request-scoped). This means instance variables are NOT shared across requests.
 * To share state (our in-memory maps), we use static ConcurrentHashMaps inside
 * a singleton DataStore class, ensuring thread-safe access without instantiating
 * resource classes as singletons.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Register resource classes
        register(DiscoveryResource.class);
        register(RoomResource.class);
        register(SensorResource.class);

        // Register exception mappers
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);
        register(GlobalExceptionMapper.class);

        // Register filters
        register(LoggingFilter.class);

        // Register Jackson for JSON
        register(JacksonFeature.class);
    }
}
