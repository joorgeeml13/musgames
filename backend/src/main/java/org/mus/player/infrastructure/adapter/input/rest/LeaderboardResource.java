package org.mus.player.infrastructure.adapter.input.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mus.player.application.dto.CreatePlayerCommand;
import org.mus.player.application.dto.PlayerDto;
import org.mus.player.application.port.input.CreatePlayerUseCase;
import org.mus.player.application.port.input.GetLeaderboardUseCase;
import org.mus.player.application.port.input.GetPlayerUseCase;
import org.mus.shared.domain.i18n.I18nMessageService;
import org.mus.shared.infrastructure.rest.response.ApiResponse;

import java.util.List;

@Path("/api/leaderboard")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LeaderboardResource {

    @Inject
    GetLeaderboardUseCase getLeaderboardUseCase;

    @Inject
    GetPlayerUseCase getPlayerUseCase;

    @Inject
    CreatePlayerUseCase createPlayerUseCase;

    @Inject
    I18nMessageService i18nMessageService;

    @GET
    public ApiResponse<List<PlayerDto>> getLeaderboard(@QueryParam("limit") @DefaultValue("50") int limit) {
        List<PlayerDto> players = getLeaderboardUseCase.getLeaderboard(limit);
        String message = i18nMessageService.getMessage("leaderboard.retrieved");
        return ApiResponse.of(players, message);
    }

    @GET
    @Path("/{username}")
    public ApiResponse<PlayerDto> getPlayerByUsername(@PathParam("username") String username) {
        PlayerDto player = getPlayerUseCase.getPlayer(username);
        String message = i18nMessageService.getMessage("player.retrieved");
        return ApiResponse.of(player, message);
    }

    @POST
    public Response createPlayer(CreatePlayerCommand command) {
        PlayerDto created = createPlayerUseCase.createPlayer(command);
        String message = i18nMessageService.getMessage("player.created");
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.of(created, message))
                .build();
    }
}
