package org.mus.player.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreatePlayerCommand(
        String username,
        Integer elo,
        Integer wins,
        Integer losses
) {}
