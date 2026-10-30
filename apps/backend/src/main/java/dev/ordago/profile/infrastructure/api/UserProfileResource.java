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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.quarkus.security.Authenticated;
import dev.ordago.profile.domain.model.UserProfile;
import dev.ordago.profile.domain.port.in.GetUserProfileUseCase;
import dev.ordago.profile.domain.port.in.UpdateUserProfileUseCase;
import dev.ordago.profile.infrastructure.api.dto.PublicUserProfileResponse;
import dev.ordago.profile.infrastructure.api.dto.UpdateUserProfileRequest;
import dev.ordago.profile.infrastructure.api.dto.UserProfileResponse;
import dev.ordago.shared.infrastructure.api.response.ApiError;

@Path("/api/v1/profiles")
@Tag(name = "User Profiles", description = "Operations for user profiles and player tag public identities")
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
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Get current user profile", description = "Retrieves the complete profile of the authenticated user")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserProfileResponse.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized - invalid or missing Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response getMyProfile() {
        UUID accountId = extractAccountId();
        UserProfile profile = getUserProfileUseCase.getProfileById(accountId);
        return Response.ok(UserProfileResponse.fromDomain(profile)).build();
    }

    @PUT
    @Path("/me")
    @Authenticated
    @SecurityRequirement(name = "jwtAuth")
    @Operation(summary = "Update current user profile", description = "Updates customizable profile settings for the authenticated user")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "User profile updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = UserProfileResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid request payload or constraint violation",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "401", description = "Unauthorized - invalid or missing Bearer token",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
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
    @Operation(summary = "Get public user profile by player tag", description = "Retrieves public profile information and statistics using a player tag")
    @APIResponses(value = {
            @APIResponse(responseCode = "200", description = "Public profile retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PublicUserProfileResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid game name or tag line format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class))),
            @APIResponse(responseCode = "404", description = "Profile not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiError.class)))
    })
    public Response getPublicProfile(
            @Parameter(description = "Game name portion of the player tag", example = "ManoMaestro", required = true)
            @PathParam("gameName") String gameName,
            @Parameter(description = "Tag line portion of the player tag (without '#')", example = "ESP1", required = true)
            @PathParam("tagLine") String tagLine
    ) {
        UserProfile profile = getUserProfileUseCase.getProfileByPlayerTag(gameName, tagLine);
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
