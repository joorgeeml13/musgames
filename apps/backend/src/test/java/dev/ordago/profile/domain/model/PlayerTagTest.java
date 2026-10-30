package dev.ordago.profile.domain.model;

import org.junit.jupiter.api.Test;
import dev.ordago.profile.domain.exception.InvalidPlayerTagException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerTagTest {

    @Test
    void shouldCreateValidPlayerTag() {
        PlayerTag playerTag = new PlayerTag("ElReyDelMus", "ESP1");
        assertThat(playerTag.gameName()).isEqualTo("ElReyDelMus");
        assertThat(playerTag.tagLine()).isEqualTo("ESP1");
        assertThat(playerTag.formatted()).isEqualTo("ElReyDelMus#ESP1");
        assertThat(playerTag.toString()).isEqualTo("ElReyDelMus#ESP1");
        assertThat(playerTag.canonicalGameName()).isEqualTo("elreydelmus");
        assertThat(playerTag.canonicalTagLine()).isEqualTo("esp1");
    }

    @Test
    void shouldCreatePlayerTagUsingStaticFactory() {
        PlayerTag playerTag = PlayerTag.of("Mano1", "01");
        assertThat(playerTag.formatted()).isEqualTo("Mano1#01");
    }

    @Test
    void shouldTrimWhitespace() {
        PlayerTag playerTag = new PlayerTag("  ManoDeMus  ", " 01 ");
        assertThat(playerTag.gameName()).isEqualTo("ManoDeMus");
        assertThat(playerTag.tagLine()).isEqualTo("01");
    }

    @Test
    void shouldCompareCaseInsensitively() {
        PlayerTag id1 = new PlayerTag("ElTonto", "MAD1");
        PlayerTag id2 = new PlayerTag("eltonto", "mad1");
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void shouldHandleEqualsContract() {
        PlayerTag id1 = new PlayerTag("ElTonto", "MAD1");
        assertThat(id1.equals(id1)).isTrue();
        assertThat(id1.equals(null)).isFalse();
        assertThat(id1.equals("ElTonto#MAD1")).isFalse();
    }

    @Test
    void shouldRejectNullInputs() {
        assertThatThrownBy(() -> new PlayerTag(null, "ESP"))
                .isInstanceOf(InvalidPlayerTagException.class);
        assertThatThrownBy(() -> new PlayerTag("ValidName", null))
                .isInstanceOf(InvalidPlayerTagException.class);
        assertThatThrownBy(() -> new PlayerTag(null, null))
                .isInstanceOf(InvalidPlayerTagException.class);
    }

    @Test
    void shouldRejectInvalidGameName() {
        // Too short (< 2 chars)
        assertThatThrownBy(() -> new PlayerTag("A", "ESP"))
                .isInstanceOf(InvalidPlayerTagException.class);
        // Too long (> 24 chars)
        assertThatThrownBy(() -> new PlayerTag("WayTooLongOfAGameNameToFitInTheTwentyFourCharLimitHere", "ESP"))
                .isInstanceOf(InvalidPlayerTagException.class);
        // Disallowed special characters
        assertThatThrownBy(() -> new PlayerTag("Invalid!Chars?", "ESP"))
                .isInstanceOf(InvalidPlayerTagException.class);
        // Blank
        assertThatThrownBy(() -> new PlayerTag("   ", "ESP"))
                .isInstanceOf(InvalidPlayerTagException.class);
    }

    @Test
    void shouldRejectInvalidTagLine() {
        // Empty
        assertThatThrownBy(() -> new PlayerTag("ValidName", ""))
                .isInstanceOf(InvalidPlayerTagException.class);
        // Too long (> 6 chars)
        assertThatThrownBy(() -> new PlayerTag("ValidName", "TOOLONGTAG"))
                .isInstanceOf(InvalidPlayerTagException.class);
        // Disallowed special characters
        assertThatThrownBy(() -> new PlayerTag("ValidName", "SP!"))
                .isInstanceOf(InvalidPlayerTagException.class);
        // Blank
        assertThatThrownBy(() -> new PlayerTag("ValidName", "   "))
                .isInstanceOf(InvalidPlayerTagException.class);
    }
}
