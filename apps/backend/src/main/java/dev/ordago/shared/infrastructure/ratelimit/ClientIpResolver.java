package dev.ordago.shared.infrastructure.ratelimit;

import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Resolves client IP address from proxy headers with fallback to remote address.
 * Protects against IP spoofing chains, trims whitespace, handles IPv4/IPv6,
 * and guards against length overflow.
 */
@ApplicationScoped
public class ClientIpResolver {

    private static final String DEFAULT_FALLBACK_IP = "127.0.0.1";
    private static final int MAX_IP_LENGTH = 45;

    public String resolve(HttpServerRequest request, ContainerRequestContext context) {
        String forwardedFor = null;
        String realIp = null;
        String remoteAddress = null;

        if (context != null) {
            forwardedFor = context.getHeaderString("X-Forwarded-For");
            realIp = context.getHeaderString("X-Real-IP");
        }
        if ((forwardedFor == null || forwardedFor.isBlank()) && request != null) {
            forwardedFor = request.getHeader("X-Forwarded-For");
        }
        if ((realIp == null || realIp.isBlank()) && request != null) {
            realIp = request.getHeader("X-Real-IP");
        }
        if (request != null && request.remoteAddress() != null) {
            remoteAddress = request.remoteAddress().host();
        }

        return resolve(forwardedFor, realIp, remoteAddress);
    }

    public String resolve(HttpServerRequest request) {
        return resolve(request, null);
    }

    public String resolve(ContainerRequestContext context) {
        return resolve(null, context);
    }

    public String resolve(String forwardedFor, String realIp, String remoteAddress) {
        String ip = null;

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String first = forwardedFor.split(",")[0].trim();
            if (!first.isBlank()) {
                ip = first;
            }
        }

        if (ip == null && realIp != null && !realIp.isBlank()) {
            String trimmed = realIp.trim();
            if (!trimmed.isBlank()) {
                ip = trimmed;
            }
        }

        if (ip == null && remoteAddress != null && !remoteAddress.isBlank()) {
            String trimmed = remoteAddress.trim();
            if (!trimmed.isBlank()) {
                ip = trimmed;
            }
        }

        if (ip == null || ip.isBlank()) {
            ip = DEFAULT_FALLBACK_IP;
        }

        ip = ip.trim();

        if (ip.startsWith("[") && ip.contains("]")) {
            ip = ip.substring(1, ip.indexOf("]"));
        } else if (ip.contains(":") && ip.indexOf(":") == ip.lastIndexOf(":")) {
            ip = ip.substring(0, ip.indexOf(":"));
        }

        if (ip.length() > MAX_IP_LENGTH) {
            ip = ip.substring(0, MAX_IP_LENGTH);
        }

        return ip;
    }
}
