package bisq.common.util;

import bisq.common.data.Pair;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings("SpellCheckingInspection")
@Slf4j
public class StringUtilsTest {
    @Test
    public void splitStringsByTags() {
        List<Pair<String, List<String>>> textWithStyleAndRest;

        textWithStyleAndRest = StringUtils.getTextStylePairs("<EUR/BTC style=text-color-light>");
        assertEquals("EUR/BTC", textWithStyleAndRest.get(0).getFirst());
        assertEquals("text-color-light", textWithStyleAndRest.get(0).getSecond().get(0));
        assertEquals(1, textWithStyleAndRest.size());

        textWithStyleAndRest = StringUtils.getTextStylePairs("You are <SELLING style=text-color-light> Bitcoin with <SEPA style=text-color-mid, text-underline> and <SEPA INSTANT style=text-color-mid,text-underline>");
        assertEquals(6, textWithStyleAndRest.size());

        assertEquals("You are ", textWithStyleAndRest.get(0).getFirst());
        assertNull(textWithStyleAndRest.get(0).getSecond());

        assertEquals("SELLING", textWithStyleAndRest.get(1).getFirst());
        assertEquals("text-color-light", textWithStyleAndRest.get(1).getSecond().get(0));

        assertEquals(" Bitcoin with ", textWithStyleAndRest.get(2).getFirst());
        assertNull(textWithStyleAndRest.get(2).getSecond());

        assertEquals("SEPA", textWithStyleAndRest.get(3).getFirst());
        assertEquals("text-color-mid", textWithStyleAndRest.get(3).getSecond().get(0));
        assertEquals("text-underline", textWithStyleAndRest.get(3).getSecond().get(1));

        assertEquals(" and ", textWithStyleAndRest.get(4).getFirst());
        assertNull(textWithStyleAndRest.get(4).getSecond());

        assertEquals("SEPA INSTANT", textWithStyleAndRest.get(5).getFirst());
        assertEquals("text-color-mid", textWithStyleAndRest.get(5).getSecond().get(0));
        assertEquals("text-underline", textWithStyleAndRest.get(5).getSecond().get(1));
    }

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

    @Test
    public void testSnakeCaseToCamelCase() {
        assertEquals("camelCase", StringUtils.snakeCaseToCamelCase("camel_case"));
        assertEquals("CamelCase", StringUtils.snakeCaseToCamelCase("_camel_case"));
        assertEquals("camelCase", StringUtils.snakeCaseToCamelCase("CAMEL_CASE"));
        assertEquals("camelcase", StringUtils.snakeCaseToCamelCase("camelcase"));
        assertEquals("camel-case", StringUtils.snakeCaseToCamelCase("camel-case"));
        assertEquals("", StringUtils.snakeCaseToCamelCase(""));
    }

    @Test
    public void testKebapCaseToCamelCase() {
        assertEquals("camelCase", StringUtils.kebapCaseToCamelCase("camel-case"));
        assertEquals("CamelCase", StringUtils.kebapCaseToCamelCase("-camel-case"));
        assertEquals("camelCase", StringUtils.kebapCaseToCamelCase("CAMEL-CASE"));
        assertEquals("camelcase", StringUtils.kebapCaseToCamelCase("camelcase"));
        assertEquals("camel_case", StringUtils.kebapCaseToCamelCase("camel_case"));
        assertEquals("", StringUtils.kebapCaseToCamelCase(""));
    }

    @Test
    public void testCamelCaseToSnakeCase() {
        assertEquals("camel_case", StringUtils.camelCaseToSnakeCase("camelCase"));
        assertEquals("camel_case", StringUtils.camelCaseToSnakeCase("CamelCase"));
        assertEquals("c_a_m_e_l__c_a_s_e", StringUtils.camelCaseToSnakeCase("CAMEL_CASE"));
        assertEquals("camelcase", StringUtils.camelCaseToSnakeCase("camelcase"));
        assertEquals("camel-case", StringUtils.camelCaseToSnakeCase("camel-case"));
        assertEquals("camel_case", StringUtils.camelCaseToSnakeCase("camel_case"));
        assertEquals("", StringUtils.camelCaseToSnakeCase(""));
    }

    @Test
    public void testCamelCaseToKebapCase() {
        assertEquals("camel-case", StringUtils.camelCaseToKebapCase("camelCase"));
        assertEquals("camel-case", StringUtils.camelCaseToKebapCase("CamelCase"));
        assertEquals("c-a-m-e-l_-c-a-s-e", StringUtils.camelCaseToKebapCase("CAMEL_CASE"));
        assertEquals("camelcase", StringUtils.camelCaseToKebapCase("camelcase"));
        assertEquals("camel-case", StringUtils.camelCaseToKebapCase("camel-case"));
        assertEquals("camel_case", StringUtils.camelCaseToKebapCase("camel_case"));
        assertEquals("", StringUtils.camelCaseToKebapCase(""));
    }

    @Test
    public void testSnakeCaseToKebapCase() {
        assertEquals("camel-case", StringUtils.snakeCaseToKebapCase("camel_case"));
        assertEquals("-camel-case", StringUtils.snakeCaseToKebapCase("_camel_case"));
        assertEquals("camel-case", StringUtils.snakeCaseToKebapCase("CAMEL_CASE"));
        assertEquals("camelcase", StringUtils.snakeCaseToKebapCase("camelcase"));
        assertEquals("camel-case", StringUtils.snakeCaseToKebapCase("camel-case"));
        assertEquals("", StringUtils.snakeCaseToKebapCase(""));
    }


}
