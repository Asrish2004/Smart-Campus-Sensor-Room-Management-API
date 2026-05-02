package com.smartcampus.exception;

/**
 * Part 5.2 - Thrown when a new sensor references a roomId that does not exist.
 * Mapped to HTTP 422 Unprocessable Entity.
 *
 * Why 422 rather than 404 (Part 5.2 Q):
 * HTTP 404 means "the requested URL/resource was not found." In this scenario,
 * the URL /api/v1/sensors IS valid and was found. The problem is inside the
 * JSON payload — the roomId field references a resource that does not exist.
 * HTTP 422 ("Unprocessable Entity") explicitly signals that the request was
 * syntactically correct (well-formed JSON) but semantically invalid (a foreign-
 * key reference is broken). Using 422 gives the client precise diagnostic
 * information: "your URL is fine, your JSON is parseable, but the content
 * violates a business rule."
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public LinkedResourceNotFoundException(String resourceType, String resourceId) {
        super("Referenced " + resourceType + " with id '" + resourceId + "' does not exist.");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
}
