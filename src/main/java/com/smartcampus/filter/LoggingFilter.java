package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5.5 - Observability filter.
 * Logs every incoming request (method + URI) and every outgoing response (status).
 *
 * Why use filters rather than per-method logging (Part 5.5 Q):
 * Filters implement a cross-cutting concern — logging applies to EVERY endpoint
 * uniformly. Placing Logger.info() inside each resource method would:
 * (a) violate DRY — the same boilerplate repeated 20+ times,
 * (b) risk omission — developers forget to add it in new methods,
 * (c) mix infrastructure concerns with business logic, hurting readability.
 * A single filter class captures 100% of traffic with zero per-method changes
 * and can be enabled/disabled centrally.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
                "[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
                "[RESPONSE] %s %s -> HTTP %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()
        ));
    }
}
