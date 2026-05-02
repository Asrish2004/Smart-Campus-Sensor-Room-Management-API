package com.smartcampus.exception;

import com.smartcampus.model.ApiError;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Part 5.4 - Global safety net: catches ALL unhandled Throwables.
 *
 * Security note (Part 5.4 Q):
 * Exposing raw Java stack traces to external clients is dangerous because:
 * 1. Package/class names reveal the internal software architecture, making
 *    it easier for attackers to craft targeted exploits.
 * 2. Library version strings in stack traces enable vulnerability fingerprinting
 *    (e.g., a known CVE in an older Jackson version).
 * 3. SQL or file-path fragments that appear in exception messages expose
 *    database schemas or filesystem layouts.
 * 4. Variable names and logic flow hints help reverse-engineer business rules.
 * This mapper replaces all of that with a generic 500 response.
 *
 * WebApplicationExceptions (e.g., 404/405 thrown by Jersey itself) are
 * re-thrown so Jersey's own handling applies; only unexpected exceptions
 * are swallowed here.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // Let JAX-RS handle its own exceptions normally (404, 405, 415, etc.)
        if (ex instanceof WebApplicationException) {
            return ((WebApplicationException) ex).getResponse();
        }

        // Log the full trace server-side for debugging, but never expose it
        LOGGER.log(Level.SEVERE, "Unhandled exception: " + ex.getMessage(), ex);

        ApiError error = new ApiError(
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please contact the API administrator."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
