package dev.ordago.profile.infrastructure.api;

import java.util.Objects;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.security.Authenticated;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.GetUserProfileUseCase;
import dev.ordago.profile.domain.port.in.UpdateUserProfileUseCase;
import dev.ordago.profile.infrastructure.api.dto.PublicUserProfileResponse;
import dev.ordago.profile.infrastructure.api.dto.UpdateUserProfileRequest;
import dev.ordago.profile.infrastructure.api.dto.UserProfileResponse;

@Path("/api/v1/profiles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class UserProfileResource {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final JsonWebToken jwt;

    public UserProfileResource(
            GetUserProfileUseCase getUserProfileUseCase,
            UpdateUserProfileUseCase updateUserProfileUseCase,
            JsonWebToken jwt
    ) {
        this.getUserProfileUseCase = Objects.requireNonNull(getUserProfileUseCase, "GetUserProfileUseCase cannot be null");
        this.updateUserProfileUseCase = Objects.requireNonNull(updateUserProfileUseCase, "UpdateUserProfileUseCase cannot be null");
        this.jwt = Objects.requireNonNull(jwt, "JsonWebToken cannot be null");
    }

    @GET
    @Path("/me")
    @Authenticated
    public Response getMyProfile() {
        UUID accountId = extractAccountId();
        UserProfile profile = getUserProfileUseCase.getProfileById(accountId);
        return Response.ok(UserProfileResponse.fromDomain(profile)).build();
    }

    @PUT
    @Path("/me")
    @Authenticated
    public Response updateMyProfile(@Valid UpdateUserProfileRequest request) {
        UUID accountId = extractAccountId();
        UpdateUserProfileUseCase.UpdateProfileCommand command = new UpdateUserProfileUseCase.UpdateProfileCommand(
                accountId,
                request.displayName(),
                request.avatarUrl(),
                request.avatarFrameId(),
                request.bio(),
                request.preferredCardBackId(),
                request.forceClassicDeckView(),
                request.soundEffectsVolume(),
                request.locale(),
                request.allowDirectChallenges()
        );
        UserProfile updatedProfile = updateUserProfileUseCase.updateProfile(command);
        return Response.ok(UserProfileResponse.fromDomain(updatedProfile)).build();
    }

    @GET
    @Path("/{gameName}/{tagLine}")
    public Response getPublicProfile(
            @PathParam("gameName") String gameName,
            @PathParam("tagLine") String tagLine
    ) {
        UserProfile profile = getUserProfileUseCase.getProfileByRiotId(gameName, tagLine);
        return Response.ok(PublicUserProfileResponse.fromDomain(profile)).build();
    }

    private UUID extractAccountId() {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalStateException("Authenticated user subject claim is missing");
        }
        return UUID.fromString(subject);
    }
}
