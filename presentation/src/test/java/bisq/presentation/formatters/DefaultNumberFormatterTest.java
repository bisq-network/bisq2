package bisq.presentation.formatters;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class DefaultNumberFormatterTest {
    @Test
    public void format() {
        DecimalFormat numberFormat = DefaultNumberFormatter.DEFAULT_NUMBER_FORMAT;
        assertEquals("0", DefaultNumberFormatter.format(0, numberFormat));
        assertEquals("-1", DefaultNumberFormatter.format(-1, numberFormat));
        assertEquals("0.1", DefaultNumberFormatter.format(.1, numberFormat));
        // rounding mode is set to DOWN
        assertEquals("0.12345678", DefaultNumberFormatter.format(.123456789, numberFormat));
        assertEquals("0.12345678", DefaultNumberFormatter.format(.123456781, numberFormat));
        assertEquals("123", DefaultNumberFormatter.format(123, numberFormat));
        assertEquals("1 234", DefaultNumberFormatter.format(1234, numberFormat));
        assertEquals("123 456 789", DefaultNumberFormatter.format(123456789, numberFormat));
        assertEquals("12 345 678 901 234", DefaultNumberFormatter.format(12345678901234d, numberFormat));
        assertEquals("123 456.789", DefaultNumberFormatter.format(123456.789, numberFormat));
    }

    @Test
    public void parse() {
        assertEquals(1, DefaultNumberFormatter.parse("1"));
        assertEquals(1, DefaultNumberFormatter.parse("01"));
        assertEquals(-1, DefaultNumberFormatter.parse("-01"));
        assertEquals(0.1, DefaultNumberFormatter.parse(",1"));
        assertEquals(0.1, DefaultNumberFormatter.parse(".1"));
        assertEquals(-0.1, DefaultNumberFormatter.parse("-.1"));
        assertEquals(1.23, DefaultNumberFormatter.parse("1,23"));
        assertEquals(967295.123, DefaultNumberFormatter.parse("967 295,123"));
        assertEquals(11, DefaultNumberFormatter.parse("1 1"));

        assertNotNull(assertThrows(NullPointerException.class, () -> DefaultNumberFormatter.parse(null)));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.parse("")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.parse("123.456,789")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.parse("a123,456.789")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.parse("..123")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.parse("-")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.parse("-.")));
    }
}
