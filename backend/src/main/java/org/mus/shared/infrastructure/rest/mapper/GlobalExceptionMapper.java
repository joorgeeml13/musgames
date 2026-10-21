package org.mus.shared.infrastructure.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.mus.shared.domain.exception.DomainException;
import org.mus.shared.domain.i18n.I18nMessageService;
import org.mus.shared.infrastructure.rest.response.ApiError;

import java.util.List;

@Provider
@ApplicationScoped
@Priority(Priorities.USER + 100)
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Inject
    I18nMessageService i18nMessageService;

    @Inject
    DomainExceptionMapper domainExceptionMapper;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof DomainException domainException) {
            return domainExceptionMapper.toResponse(domainException);
        }

        String path = "";
        if (uriInfo != null) {
            try {
                path = uriInfo.getPath();
                if (path != null) {
                    path = path.replaceAll("[\\r\\n]", "");
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (isJsonParseException(exception)) {
            String message = i18nMessageService.getMessage("error.bad_request");
            ApiError apiError = new ApiError(
                    400,
                    "Bad Request",
                    "BAD_REQUEST",
                    message,
                    path,
                    List.of()
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(apiError).build();
        }

        if (exception instanceof WebApplicationException webAppEx) {
            int status = webAppEx.getResponse() != null ? webAppEx.getResponse().getStatus() : 500;
            Response.Status responseStatus = Response.Status.fromStatusCode(status);
            String reasonPhrase = (responseStatus != null) ? responseStatus.getReasonPhrase() : "HTTP Error";

            String code = switch (status) {
                case 400 -> "BAD_REQUEST";
                case 404 -> "NOT_FOUND";
                case 405 -> "METHOD_NOT_ALLOWED";
                case 409 -> "CONFLICT";
                default -> "HTTP_ERROR_" + status;
            };

            String msgKey = switch (status) {
                case 400 -> "error.bad_request";
                case 404 -> "error.not_found";
                case 405 -> "error.method_not_allowed";
                case 409 -> "error.conflict";
                default -> null;
            };

            // Security: Never leak internal exception details or stack traces on 5xx errors
            String message;
            if (status >= 500) {
                LOG.errorf(webAppEx, "WebApplicationException 5xx caught: %s", webAppEx.getMessage());
                message = i18nMessageService.getMessage("error.internal");
            } else if (msgKey != null) {
                message = i18nMessageService.getMessage(msgKey);
            } else {
                message = (webAppEx.getMessage() != null && !webAppEx.getMessage().isBlank())
                        ? webAppEx.getMessage().replaceAll("[\\r\\n]", " ")
                        : reasonPhrase;
            }

            ApiError apiError = new ApiError(
                    status,
                    reasonPhrase,
                    code,
                    message,
                    path,
                    List.of()
            );
            return Response.status(status).entity(apiError).build();
        }

        LOG.error("Unhandled server exception", exception);

        String internalErrorMessage = i18nMessageService.getMessage("error.internal");
        ApiError apiError = new ApiError(
                500,
                "Internal Server Error",
                "INTERNAL_SERVER_ERROR",
                internalErrorMessage,
                path,
                List.of()
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(apiError).build();
    }

    private boolean isJsonParseException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof JsonProcessingException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
