package dev.ordago.shared.infrastructure.ratelimit;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import io.github.bucket4j.ConsumptionProbe;
import io.vertx.core.http.HttpServerRequest;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import dev.ordago.shared.infrastructure.api.response.ApiError;

/**
 * JAX-RS name-bound request and response filter enforcing rate limits on critical endpoints.
 * Returns HTTP 429 Too Many Requests with standard RFC headers when limits are breached.
 */
@Provider
@RateLimited
@Priority(Priorities.AUTHENTICATION - 50)
@ApplicationScoped
public class RateLimitFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String PROPERTY_ABORTED = "ordago.ratelimit.aborted";
    private static final String PROPERTY_LIMIT = "ordago.ratelimit.limit";
    private static final String PROPERTY_REMAINING = "ordago.ratelimit.remaining";
    private static final String PROPERTY_RESET = "ordago.ratelimit.reset";

    @Context
    ResourceInfo resourceInfo;

    @Context
    HttpServerRequest httpRequest;

    @Inject
    RateLimitService rateLimitService;

    @Inject
    ClientIpResolver clientIpResolver;

    public RateLimitFilter() {
    }

    public RateLimitFilter(
            ResourceInfo resourceInfo,
            HttpServerRequest httpRequest,
            RateLimitService rateLimitService,
            ClientIpResolver clientIpResolver
    ) {
        this.resourceInfo = resourceInfo;
        this.httpRequest = httpRequest;
        this.rateLimitService = Objects.requireNonNull(rateLimitService);
        this.clientIpResolver = Objects.requireNonNull(clientIpResolver);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        RateLimitTier tier = resolveTier();
        String clientIp = clientIpResolver.resolve(httpRequest, requestContext);
        long capacity = rateLimitService.getCapacity(tier);

        ConsumptionProbe probe = rateLimitService.consumeToken(clientIp, tier);

        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
            long resetEpochSeconds = (System.currentTimeMillis() / 1000L) + retryAfterSeconds;
            String path = extractPath(requestContext.getUriInfo());

            ApiError apiError = ApiError.of(
                    "TOO_MANY_REQUESTS",
                    "Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds.",
                    path
            );

            Response response = Response.status(Response.Status.TOO_MANY_REQUESTS)
                    .header("Retry-After", retryAfterSeconds)
                    .header("X-RateLimit-Limit", capacity)
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", resetEpochSeconds)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(apiError)
                    .build();

            requestContext.setProperty(PROPERTY_ABORTED, Boolean.TRUE);
            requestContext.abortWith(response);
            return;
        }

        requestContext.setProperty(PROPERTY_LIMIT, capacity);
        requestContext.setProperty(PROPERTY_REMAINING, probe.getRemainingTokens());
        long nanosToReset = probe.getNanosToWaitForRefill();
        long resetSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanosToReset) + 1);
        requestContext.setProperty(PROPERTY_RESET, (System.currentTimeMillis() / 1000L) + resetSeconds);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (Boolean.TRUE.equals(requestContext.getProperty(PROPERTY_ABORTED))) {
            return;
        }

        Object limit = requestContext.getProperty(PROPERTY_LIMIT);
        Object remaining = requestContext.getProperty(PROPERTY_REMAINING);

        if (limit != null) {
            MultivaluedMap<String, Object> headers = responseContext.getHeaders();
            headers.putSingle("X-RateLimit-Limit", limit);
            if (remaining != null) {
                headers.putSingle("X-RateLimit-Remaining", remaining);
            }
            Object reset = requestContext.getProperty(PROPERTY_RESET);
            if (reset != null) {
                headers.putSingle("X-RateLimit-Reset", reset);
            }
        }
    }

    private RateLimitTier resolveTier() {
        if (resourceInfo != null) {
            Method method = resourceInfo.getResourceMethod();
            if (method != null) {
                RateLimited rateLimited = method.getAnnotation(RateLimited.class);
                if (rateLimited != null) {
                    return rateLimited.value();
                }
            }
            Class<?> resourceClass = resourceInfo.getResourceClass();
            if (resourceClass != null) {
                RateLimited rateLimited = resourceClass.getAnnotation(RateLimited.class);
                if (rateLimited != null) {
                    return rateLimited.value();
                }
            }
        }
        return RateLimitTier.STANDARD;
    }

    private String extractPath(UriInfo uriInfo) {
        if (uriInfo == null) {
            return "";
        }
        try {
            String path = uriInfo.getPath();
            if (path == null) {
                return "";
            }
            path = path.replaceAll("[\\r\\n]", "");
            return path.startsWith("/") ? path : "/" + path;
        } catch (Exception ignored) {
            return "";
        }
    }
}
