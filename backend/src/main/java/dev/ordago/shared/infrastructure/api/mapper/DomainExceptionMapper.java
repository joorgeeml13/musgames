package dev.ordago.shared.infrastructure.api.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import dev.ordago.shared.domain.exception.DomainException;
import dev.ordago.shared.domain.i18n.I18nMessageService;
import dev.ordago.shared.infrastructure.api.response.ApiError;

@Provider
@ApplicationScoped
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    @Inject
    I18nMessageService i18nMessageService;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(DomainException exception) {
        int status = exception.getStatusCode();
        Response.Status responseStatus = Response.Status.fromStatusCode(status);
        String reasonPhrase = (responseStatus != null) ? responseStatus.getReasonPhrase() : "Error";

        String messageKey = exception.getMessage();
        String translatedMessage = i18nMessageService.getMessage(messageKey, exception.getArgs());

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

        ApiError apiError = new ApiError(
                status,
                reasonPhrase,
                exception.getCode(),
                translatedMessage,
                path,
                exception.getDetails()
        );

        return Response.status(status).entity(apiError).build();
    }
}
