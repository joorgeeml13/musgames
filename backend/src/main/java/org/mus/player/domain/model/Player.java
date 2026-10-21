package org.mus.player.domain.model;

public class Player {
    private Long id;
    private String username;
    private int elo;
    private int wins;
    private int losses;

    public Player() {
        this.elo = 1200;
        this.wins = 0;
        this.losses = 0;
    }

    public Player(String username) {
        this.username = username;
        this.elo = 1200;
        this.wins = 0;
        this.losses = 0;
    }

    public Player(Long id, String username, int elo, int wins, int losses) {
        this.id = id;
        this.username = username;
        this.elo = elo;
        this.wins = wins;
        this.losses = losses;
    }

    public Player(String username, int elo, int wins, int losses) {
        this(null, username, elo, wins, losses);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }
}
