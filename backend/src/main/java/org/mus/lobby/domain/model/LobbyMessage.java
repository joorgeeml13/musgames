package org.mus.lobby.domain.model;

import java.time.Instant;

public class LobbyMessage {
    private String sender;
    private String content;
    private String type;
    private Instant timestamp;

    public LobbyMessage() {
        this.timestamp = Instant.now();
    }

    public LobbyMessage(String sender, String content, String type, Instant timestamp) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static LobbyMessage of(String content) {
        return new LobbyMessage("anonymous", content, "CHAT", Instant.now());
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
