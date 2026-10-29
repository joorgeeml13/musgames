package dev.ordago.profile.domain.model;

import org.junit.jupiter.api.Test;
import dev.ordago.profile.domain.exception.InvalidRiotIdException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiotIdTest {

    @Test
    void shouldCreateValidRiotId() {
        RiotId riotId = new RiotId("ElReyDelMus", "ESP1");
        assertThat(riotId.gameName()).isEqualTo("ElReyDelMus");
        assertThat(riotId.tagLine()).isEqualTo("ESP1");
        assertThat(riotId.formatted()).isEqualTo("ElReyDelMus#ESP1");
        assertThat(riotId.toString()).isEqualTo("ElReyDelMus#ESP1");
        assertThat(riotId.canonicalGameName()).isEqualTo("elreydelmus");
        assertThat(riotId.canonicalTagLine()).isEqualTo("esp1");
    }

    @Test
    void shouldCreateRiotIdUsingStaticFactory() {
        RiotId riotId = RiotId.of("Mano1", "01");
        assertThat(riotId.formatted()).isEqualTo("Mano1#01");
    }

    @Test
    void shouldTrimWhitespace() {
        RiotId riotId = new RiotId("  ManoDeMus  ", " 01 ");
        assertThat(riotId.gameName()).isEqualTo("ManoDeMus");
        assertThat(riotId.tagLine()).isEqualTo("01");
    }

    @Test
    void shouldCompareCaseInsensitively() {
        RiotId id1 = new RiotId("ElTonto", "MAD1");
        RiotId id2 = new RiotId("eltonto", "mad1");
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void shouldHandleEqualsContract() {
        RiotId id1 = new RiotId("ElTonto", "MAD1");
        assertThat(id1.equals(id1)).isTrue();
        assertThat(id1.equals(null)).isFalse();
        assertThat(id1.equals("ElTonto#MAD1")).isFalse();
    }

    @Test
    void shouldRejectNullInputs() {
        assertThatThrownBy(() -> new RiotId(null, "ESP"))
                .isInstanceOf(InvalidRiotIdException.class);
        assertThatThrownBy(() -> new RiotId("ValidName", null))
                .isInstanceOf(InvalidRiotIdException.class);
        assertThatThrownBy(() -> new RiotId(null, null))
                .isInstanceOf(InvalidRiotIdException.class);
    }

    @Test
    void shouldRejectInvalidGameName() {
        // Too short (< 2 chars)
        assertThatThrownBy(() -> new RiotId("A", "ESP"))
                .isInstanceOf(InvalidRiotIdException.class);
        // Too long (> 24 chars)
        assertThatThrownBy(() -> new RiotId("WayTooLongOfAGameNameToFitInTheTwentyFourCharLimitHere", "ESP"))
                .isInstanceOf(InvalidRiotIdException.class);
        // Disallowed special characters
        assertThatThrownBy(() -> new RiotId("Invalid!Chars?", "ESP"))
                .isInstanceOf(InvalidRiotIdException.class);
        // Blank
        assertThatThrownBy(() -> new RiotId("   ", "ESP"))
                .isInstanceOf(InvalidRiotIdException.class);
    }

    @Test
    void shouldRejectInvalidTagLine() {
        // Empty
        assertThatThrownBy(() -> new RiotId("ValidName", ""))
                .isInstanceOf(InvalidRiotIdException.class);
        // Too long (> 6 chars)
        assertThatThrownBy(() -> new RiotId("ValidName", "TOOLONGTAG"))
                .isInstanceOf(InvalidRiotIdException.class);
        // Disallowed special characters
        assertThatThrownBy(() -> new RiotId("ValidName", "SP!"))
                .isInstanceOf(InvalidRiotIdException.class);
        // Blank
        assertThatThrownBy(() -> new RiotId("ValidName", "   "))
                .isInstanceOf(InvalidRiotIdException.class);
    }
}
