package bisq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class StringUtilsTest {

    @Test
    public void deriveWordStartingWith() {
        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello jo", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("@jo x", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello@jo", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello @jo!", '@'), null);
        assert Objects.equals(StringUtils.deriveWordStartingWith("Go to#chan", '@'), null);

        assert Objects.equals(StringUtils.deriveWordStartingWith("Hello @jo", '@'), "jo");
        assert Objects.equals(StringUtils.deriveWordStartingWith("@john", '@'), "john");
        assert Objects.equals(StringUtils.deriveWordStartingWith("Go to #chann", '#'), "chann");
        assert Objects.equals(StringUtils.deriveWordStartingWith("#chann", '#'), "chann");
    }

    @Test
    public void testTruncate() {
        assertEquals("1", StringUtils.truncate("1", 4));
        assertEquals("123", StringUtils.truncate("123", 4));
        assertEquals("1234", StringUtils.truncate("1234", 4));
        assertEquals("1...", StringUtils.truncate("12345", 4));
        assertEquals("12345...", StringUtils.truncate("1234567890", 8));
    }
}
