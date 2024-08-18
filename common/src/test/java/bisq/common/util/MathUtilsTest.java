package bisq.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class MathUtilsTest {
    @Test
    public void parseToDouble() {
        assertEquals(1, MathUtils.parseToDouble("1"));
        assertEquals(1, MathUtils.parseToDouble("01"));
        assertEquals(-1, MathUtils.parseToDouble("-01"));
        assertEquals(0.1, MathUtils.parseToDouble(",1"));
        assertEquals(0.1, MathUtils.parseToDouble(".1"));
        assertEquals(-0.1, MathUtils.parseToDouble("-.1"));
        assertEquals(1.23, MathUtils.parseToDouble("1,23"));
        assertEquals(967295.123, MathUtils.parseToDouble("967 295,123"));
        assertEquals(11, MathUtils.parseToDouble("1 1"));

        assertNotNull(assertThrows(NullPointerException.class, () -> MathUtils.parseToDouble(null)));
        assertNotNull(assertThrows(NumberFormatException.class, () -> MathUtils.parseToDouble("")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> MathUtils.parseToDouble("123.456,789")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> MathUtils.parseToDouble("a123,456.789")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> MathUtils.parseToDouble("..123")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> MathUtils.parseToDouble("-")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> MathUtils.parseToDouble("-.")));
    }
}
