package dev.ordago.profile.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import dev.ordago.profile.domain.exception.InvalidPlayerTagException;

public record PlayerTag(String gameName, String tagLine) {

    private static final Pattern GAME_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]{2,24}$");
    private static final Pattern TAG_LINE_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,6}$");

    public PlayerTag {
        if (gameName == null || tagLine == null) {
            throw new InvalidPlayerTagException("Game name and tag line must not be null");
        }
        String trimmedGameName = gameName.trim();
        String trimmedTagLine = tagLine.trim();

        if (trimmedGameName.length() < 2 || trimmedGameName.length() > 24 || !GAME_NAME_PATTERN.matcher(trimmedGameName).matches()) {
            throw new InvalidPlayerTagException("Game name must be 2-24 characters containing only alphanumeric characters, spaces, hyphens, and underscores");
        }
        if (trimmedTagLine.isEmpty() || trimmedTagLine.length() > 6 || !TAG_LINE_PATTERN.matcher(trimmedTagLine).matches()) {
            throw new InvalidPlayerTagException("Tag line must be 1-6 alphanumeric characters");
        }
        gameName = trimmedGameName;
        tagLine = trimmedTagLine;
    }

    public static PlayerTag of(String gameName, String tagLine) {
        return new PlayerTag(gameName, tagLine);
    }

    public String canonicalGameName() {
        return gameName.toLowerCase(Locale.ROOT);
    }

    public String canonicalTagLine() {
        return tagLine.toLowerCase(Locale.ROOT);
    }

    public String formatted() {
        return gameName + "#" + tagLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerTag playerTag = (PlayerTag) o;
        return canonicalGameName().equals(playerTag.canonicalGameName()) &&
               canonicalTagLine().equals(playerTag.canonicalTagLine());
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonicalGameName(), canonicalTagLine());
    }

    @Override
    public String toString() {
        return formatted();
    }
}
