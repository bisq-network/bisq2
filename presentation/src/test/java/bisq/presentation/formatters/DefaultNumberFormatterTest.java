package bisq.presentation.formatters;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class DefaultNumberFormatterTest {
    @Test
    public void formatDoubleWithDefaultNumberFormat() {
        DecimalFormat numberFormat = DefaultNumberFormatter.DEFAULT_NUMBER_FORMAT;
        assertEquals("-1", DefaultNumberFormatter.formatDouble(-1, numberFormat));
        assertEquals("0", DefaultNumberFormatter.formatDouble(0, numberFormat));
        assertEquals("0.1", DefaultNumberFormatter.formatDouble(.1, numberFormat));
        // rounding mode is set to DOWN
        assertEquals("0.12345678", DefaultNumberFormatter.formatDouble(.123456789, numberFormat));
        assertEquals("0.12345678", DefaultNumberFormatter.formatDouble(.123456781, numberFormat));
        assertEquals("123", DefaultNumberFormatter.formatDouble(123, numberFormat));
        assertEquals("1 234", DefaultNumberFormatter.formatDouble(1234, numberFormat));
        assertEquals("123 456 789", DefaultNumberFormatter.formatDouble(123456789, numberFormat));
        assertEquals("12 345 678 901 234", DefaultNumberFormatter.formatDouble(12345678901234d, numberFormat));
        assertEquals("123 456.789", DefaultNumberFormatter.formatDouble(123456.789, numberFormat));
    }

    @Test
    public void parseToDoubleWithNumberFormat() {
        assertEquals(123456.789, DefaultNumberFormatter.parseToDoubleWithNumberFormat("123.456,789", Locale.GERMAN));
        assertEquals(123456.789, DefaultNumberFormatter.parseToDoubleWithNumberFormat("123,456.789", Locale.US));

        assertEquals(-1.0, DefaultNumberFormatter.parseToDoubleWithNumberFormat("-01", Locale.US));
        assertEquals(1.0, DefaultNumberFormatter.parseToDoubleWithNumberFormat("01", Locale.US));
        assertEquals(0.1, DefaultNumberFormatter.parseToDoubleWithNumberFormat(",1", Locale.GERMAN));
        assertEquals(0.1, DefaultNumberFormatter.parseToDoubleWithNumberFormat(".1", Locale.US));

        // Invalid input parts get ignored
        assertEquals(123456.789, DefaultNumberFormatter.parseToDoubleWithNumberFormat("123,456.789,012", Locale.US));
        assertEquals(123456.789, DefaultNumberFormatter.parseToDoubleWithNumberFormat("123.456,789.012", Locale.GERMAN));
        assertEquals(123456.789, DefaultNumberFormatter.parseToDoubleWithNumberFormat("123.456,789a", Locale.GERMAN));
        assertEquals(1.0, DefaultNumberFormatter.parseToDoubleWithNumberFormat(",1", Locale.US));
        assertEquals(1.0, DefaultNumberFormatter.parseToDoubleWithNumberFormat(".1", Locale.GERMAN));

        assertEquals(1.1, DefaultNumberFormatter.parseToDoubleWithNumberFormat(",1.1", Locale.US));
        assertEquals(0.1, DefaultNumberFormatter.parseToDoubleWithNumberFormat(".1.1", Locale.US));
        assertEquals(1.0, DefaultNumberFormatter.parseToDoubleWithNumberFormat("1%", Locale.US));
        assertEquals(1.0, DefaultNumberFormatter.parseToDoubleWithNumberFormat("1 %", Locale.US));
        assertEquals(1.0, DefaultNumberFormatter.parseToDoubleWithNumberFormat("1 1", Locale.US));
        assertNotNull(assertThrows(NumberFormatException.class,
                () -> DefaultNumberFormatter.parseToDoubleWithNumberFormat("a123,456.789", Locale.US)));
        assertNotNull(assertThrows(NumberFormatException.class,
                () -> DefaultNumberFormatter.parseToDoubleWithNumberFormat("..123", Locale.US)));
        assertNotNull(assertThrows(NumberFormatException.class,
                () -> DefaultNumberFormatter.parseToDoubleWithNumberFormat("-", Locale.US)));
        assertNotNull(assertThrows(NumberFormatException.class,
                () -> DefaultNumberFormatter.parseToDoubleWithNumberFormat("-.", Locale.US)));
    }

    @Test
    public void clearAndParseToDouble() {
        assertEquals(0, DefaultNumberFormatter.clearAndParseToDouble("0"));
        assertEquals(0.1, DefaultNumberFormatter.clearAndParseToDouble("0.1"));
        assertEquals(1, DefaultNumberFormatter.clearAndParseToDouble("01"));
        assertEquals(0.1, DefaultNumberFormatter.clearAndParseToDouble("0.10"));
        assertEquals(1234567890, DefaultNumberFormatter.clearAndParseToDouble("1234567890"));
        assertEquals(123456.789, DefaultNumberFormatter.clearAndParseToDouble("123 456.789"));
        assertEquals(123456.789, DefaultNumberFormatter.clearAndParseToDouble("123 456.789"));
        assertEquals(123456.789, DefaultNumberFormatter.clearAndParseToDouble("123 456,789"));

        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.clearAndParseToDouble("")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.clearAndParseToDouble("111,222.333")));
        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.clearAndParseToDouble("111.222,333")));
    }

    @Test
    public void reformat() {
        Locale inputLocale = Locale.US;
        assertEquals("-1", DefaultNumberFormatter.reformat("-1", inputLocale));
        assertEquals("0", DefaultNumberFormatter.reformat("0", inputLocale));
        assertEquals("0.1", DefaultNumberFormatter.reformat(".1", inputLocale));
        // rounding mode is set to DOWN
        assertEquals("0.12345678", DefaultNumberFormatter.reformat(".123456789", inputLocale));
        assertEquals("0.12345678", DefaultNumberFormatter.reformat(".123456781", inputLocale));
        assertEquals("123", DefaultNumberFormatter.reformat("123", inputLocale));
        assertEquals("1 234", DefaultNumberFormatter.reformat("1234", inputLocale));
        assertEquals("123 456 789", DefaultNumberFormatter.reformat("123456789", inputLocale));
        assertEquals("12 345 678 901 234", DefaultNumberFormatter.reformat("12345678901234d", inputLocale));
        assertEquals("123 456.789", DefaultNumberFormatter.reformat("123456.789", inputLocale));

        // US -> Default
        assertEquals("123 456.789", DefaultNumberFormatter.reformat("123,456.789", Locale.US));
        // German -> Default
        assertEquals("123 456.789", DefaultNumberFormatter.reformat("123.456,789", Locale.GERMAN));

        assertNotNull(assertThrows(NumberFormatException.class, () -> DefaultNumberFormatter.reformat("", inputLocale)));
        assertNotNull(assertThrows(NullPointerException.class, () -> DefaultNumberFormatter.reformat(null, inputLocale)));
    }
}
