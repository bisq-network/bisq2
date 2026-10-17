package bisq.common.util;

import bisq.common.data.Pair;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testTruncate() {
        assertEquals("1", StringUtils.truncate("1", 4));
        assertEquals("123", StringUtils.truncate("123", 4));
        assertEquals("1234", StringUtils.truncate("1234", 4));
        assertEquals("1...", StringUtils.truncate("12345", 4, StringUtils.DOT_ELLIPSIS));
        assertEquals("123…", StringUtils.truncate("12345", 4));
        assertEquals("12345...", StringUtils.truncate("1234567890", 8, StringUtils.DOT_ELLIPSIS));
    }

    @Test
    public void truncateDoesNotSplitASurrogatePair() {
        // One code point stored as two chars, a high and a low surrogate
        String emoji = "😀";

        // The cut falls between the two chars of the emoji, so the emoji is dropped as a whole
        assertEquals("1…", StringUtils.truncate("1" + emoji + "45", 3));
        assertEquals("1...", StringUtils.truncate("1" + emoji + "4567", 5, StringUtils.DOT_ELLIPSIS));
        assertEquals(emoji + "…", StringUtils.truncate(emoji + emoji + "5", 4));
        assertEquals("…", StringUtils.truncate(emoji + "345", 2));

        // The cut falls right after the emoji, so the emoji is kept
        assertEquals("1" + emoji + "…", StringUtils.truncate("1" + emoji + "456", 4));
        assertEquals(emoji + "…", StringUtils.truncate(emoji + "345", 3));
    }

    @Test
    public void truncateNeverOutputsAnUnpairedSurrogate() {
        String text = "a😀bc😀😀d🇩🇪";
        for (String ellipsis : List.of(StringUtils.UNICODE_ELLIPSIS, StringUtils.DOT_ELLIPSIS)) {
            for (int maxLength = ellipsis.length() + 1; maxLength <= text.length() + 1; maxLength++) {
                String truncated = StringUtils.truncate(text, maxLength, ellipsis);

                assertTrue(truncated.length() <= maxLength, "maxLength=" + maxLength);
                // An unpaired surrogate does not survive encoding, it comes back as '?'
                String decoded = new String(truncated.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                assertEquals(truncated, decoded, "maxLength=" + maxLength);
            }
        }
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

    @Test
    void testRemovesBiDiCharacters() {
        String input = "\u202D+1 415 9604264\u202C"; // LRO ... PDF
        String expected = "+1 415 9604264";
        assertEquals(expected, StringUtils.cleanUserInput(input));
    }

    @Test
    void testRemovesZeroWidthSpace() {
        String input = "+1\u200B415\u200B9604264"; // zero-width spaces
        String expected = "+14159604264";
        assertEquals(expected, StringUtils.cleanUserInput(input));
    }

    @Test
    void testRemovesRLMandLRM() {
        String input = "\u200E+1 415\u200F 9604264"; // LRM and RLM
        String expected = "+1 415 9604264";
        assertEquals(expected, StringUtils.cleanUserInput(input));
    }

    @Test
    void testReturnsNullOnNullInput() {
        assertEquals("", StringUtils.cleanUserInput(null));
    }

    @Test
    void testEmptyString() {
        assertEquals("", StringUtils.cleanUserInput(""));
    }

    @Test
    void testOnlyInvisibleCharacters() {
        String input = "\u200E\u202D\u202C\u200F\u200B";
        assertEquals("", StringUtils.cleanUserInput(input));
    }

    @Test
    void testMixedValidAndInvisibleCharacters() {
        String input = "  \u202Duser@example.com\u202C  ";
        String expected = "user@example.com";
        assertEquals(expected, StringUtils.cleanUserInput(input));
    }

    @Test
    void extractHyperlinksReplacesEachMarkerWithItsLinkAndNumber() {
        List<String> hyperlinks = new ArrayList<>();
        String message = StringUtils.extractHyperlinks(
                "Read [HYPERLINK:https://bisq.wiki/Reputation] and [HYPERLINK:https://bisq.network].", hyperlinks);

        assertEquals("Read 'https://bisq.wiki/Reputation' [1] and 'https://bisq.network' [2].", message);
        assertEquals(List.of("https://bisq.wiki/Reputation", "https://bisq.network"), hyperlinks);
    }

    // Only the application's own text templates may create a link in a popup. Text of another user is shown as plain
    // text, also when the application inserts it into a template. See docs/specifications/desktop/links.md.
    @Test
    void neutralizedTextOfAnotherUserCreatesNoLink() {
        String report = "Look at [HYPERLINK:\\\\attacker.example\\share\\report.exe]";

        List<String> hyperlinks = new ArrayList<>();
        String message = StringUtils.extractHyperlinks(StringUtils.neutralizeHyperlinkMarkers(report), hyperlinks);

        assertEquals(List.of(), hyperlinks);
        assertEquals("Look at [hyperlink:\\\\attacker.example\\share\\report.exe]", message);
    }

    @Test
    void templateMarkerStillCreatesLinkWhenTextOfAnotherUserIsInserted() {
        String template = "Client ''{0}'' wrote: {1}. Read more: [HYPERLINK:https://bisq.wiki/Reputation]";
        String text = MessageFormat.format(template,
                StringUtils.neutralizeHyperlinkMarkers("[HYPERLINK:smb://attacker.example/share]"),
                StringUtils.neutralizeHyperlinkMarkers("[HYPERLINK:https://attacker.example]"));

        List<String> hyperlinks = new ArrayList<>();
        String message = StringUtils.extractHyperlinks(text, hyperlinks);

        assertEquals(List.of("https://bisq.wiki/Reputation"), hyperlinks);
        assertEquals("Client '[hyperlink:smb://attacker.example/share]' wrote: [hyperlink:https://attacker.example]. " +
                "Read more: 'https://bisq.wiki/Reputation' [1]", message);
    }

    // A value can complete a marker together with a character of the template. Breaking the marker word, and not
    // only a complete marker, covers these cases too.
    @Test
    void neutralizedValueCannotCompleteMarkerWithCharactersOfTemplate() {
        // The value ends with "[HYPERLINK", and the template continues with ":".
        assertNoHyperlink(MessageFormat.format("{0}: {1}]",
                StringUtils.neutralizeHyperlinkMarkers("[HYPERLINK"),
                StringUtils.neutralizeHyperlinkMarkers("\\\\attacker.example\\x.exe")));
        // The template ends with "[", and the value starts with "HYPERLINK:".
        assertNoHyperlink(MessageFormat.format("[{0}]",
                StringUtils.neutralizeHyperlinkMarkers("HYPERLINK:\\\\attacker.example\\x.exe")));
    }

    @Test
    void neutralizationCannotBeBypassedWithNestedMarkerWord() {
        String neutralized = StringUtils.neutralizeHyperlinkMarkers("[HYPERHYPERLINKLINK:\\\\attacker.example\\x.exe]");

        assertEquals("[HYPERhyperlinkLINK:\\\\attacker.example\\x.exe]", neutralized);
        assertNoHyperlink(neutralized);
    }

    // extractHyperlinks passes the link to String.replaceFirst as part of the replacement, where "$" starts a group
    // reference. A marker with the link "$" made extractHyperlinks throw, so the popup could not be created.
    @Test
    void neutralizedMarkerWithDollarSignDoesNotBreakPopupText() {
        String neutralized = StringUtils.neutralizeHyperlinkMarkers("[HYPERLINK:$]");

        List<String> hyperlinks = new ArrayList<>();
        assertEquals("[hyperlink:$]", StringUtils.extractHyperlinks(neutralized, hyperlinks));
        assertEquals(List.of(), hyperlinks);
    }

    @Test
    void neutralizationKeepsOtherTextAndChangesNothingTheSecondTime() {
        String text = "Price [EUR]: 1.000 per BTC, see https://bisq.network and [hyperlink:x] or HyperLink";
        assertEquals(text, StringUtils.neutralizeHyperlinkMarkers(text));

        String neutralized = StringUtils.neutralizeHyperlinkMarkers("[HYPERLINK:x] HYPERLINK");
        assertEquals("[hyperlink:x] hyperlink", neutralized);
        assertEquals(neutralized, StringUtils.neutralizeHyperlinkMarkers(neutralized));

        assertNull(StringUtils.neutralizeHyperlinkMarkers(null));
    }

    private static void assertNoHyperlink(String text) {
        List<String> hyperlinks = new ArrayList<>();
        StringUtils.extractHyperlinks(text, hyperlinks);
        assertEquals(List.of(), hyperlinks, "text: " + text);
    }

    @Test
    void sanitizeForLogStripsControlFormatAndBidiCharacters() {
        // \p{Cntrl} misses U+0085 (NEL, a C1 control) and the format/bidi category (U+202E,
        // U+200B); each stripped character becomes '?'. A crafted id must not carry a line
        // break or a reordering control into a log line.
        String injected = "x\u0085WARN forged\u2028line reversed\u202Etext\u0007bell\u200Bzwsp";
        assertEquals("x?WARN forged?line reversed?text?bell?zwsp", StringUtils.sanitizeForLog(injected));
        assertEquals("", StringUtils.sanitizeForLog(null));
    }
}
