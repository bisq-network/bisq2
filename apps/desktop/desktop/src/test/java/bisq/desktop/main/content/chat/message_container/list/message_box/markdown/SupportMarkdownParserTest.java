/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.chat.message_container.list.message_box.markdown;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupportMarkdownParserTest {
    @Test
    void plainTextHasNoMarkdownFormatting() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("hello support");

        assertThat(document.hasMarkdownFormatting()).isFalse();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).segments())
                .containsExactly(new SupportMarkdownDocument.Segment(
                        SupportMarkdownDocument.SegmentType.TEXT,
                        "hello support",
                        null));
    }

    @Test
    void parsesInlineMarkdownSyntax() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("**bold** *italic* `code` [docs](https://bisq.network)");

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.BOLD, "bold", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " ", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.ITALIC, "italic", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " ", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.CODE, "code", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " ", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.LINK, "docs", "https://bisq.network"));
    }

    @Test
    void rejectsUnsupportedLinkSchemes() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("[pwn](javascript:alert(1))");

        assertThat(document.hasMarkdownFormatting()).isFalse();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "[pwn](javascript:alert(1))", null));
    }

    @Test
    void autoLinksHttpAndHttpsUrls() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("Use https://bisq.network and http://localhost:8090/api");
        List<SupportMarkdownDocument.Segment> segments = document.lines().get(0).segments();

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(segments).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "Use ", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.LINK, "https://bisq.network", "https://bisq.network"),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " and ", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.LINK, "http://localhost:8090/api", "http://localhost:8090/api"));
    }

    @Test
    void supportsEscapedMarkdownDelimiters() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("\\*not italic\\* and \\`not code\\`");

        assertThat(document.hasMarkdownFormatting()).isFalse();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "*not italic* and `not code`", null));
    }

    @Test
    void parsesHorizontalRuleMarkdownLine() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("before\n---\nafter");

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(document.lines()).hasSize(3);
        assertThat(document.lines().get(0).type()).isEqualTo(SupportMarkdownDocument.LineType.TEXT);
        assertThat(document.lines().get(1).type()).isEqualTo(SupportMarkdownDocument.LineType.HORIZONTAL_RULE);
        assertThat(document.lines().get(1).segments()).isEmpty();
        assertThat(document.lines().get(2).type()).isEqualTo(SupportMarkdownDocument.LineType.TEXT);
    }

    @Test
    void parsesLevelThreeHeadingMarkdownLine() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("### Answer quality");

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).type()).isEqualTo(SupportMarkdownDocument.LineType.HEADING_3);
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "Answer quality", null));
    }

    @Test
    void parsesBisqIconImageMarkdown() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("![Wiki](bisq-icon://wiki) source");
        List<SupportMarkdownDocument.Segment> segments = document.lines().get(0).segments();

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(segments).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.IMAGE, "Wiki", "bisq-icon://wiki"),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " source", null));
    }

    @Test
    void parsesSourceLineWithIconTypeAndLink() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse(
                "- ![FAQ](bisq-icon://faq) [FAQ] [What is Bisq Easy?](https://bisq.network/faq/what-is-bisq-easy)");
        List<SupportMarkdownDocument.Segment> segments = document.lines().get(0).segments();

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(segments).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "- ", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.IMAGE, "FAQ", "bisq-icon://faq"),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " ", null),
                new SupportMarkdownDocument.Segment(
                        SupportMarkdownDocument.SegmentType.LINK,
                        "What is Bisq Easy?",
                        "https://bisq.network/faq/what-is-bisq-easy"));
    }

    @Test
    void removesWikiPrefixAfterSourceIconCaseInsensitive() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse(
                "- ![Wiki](bisq-icon://wiki) [wIkI] [Bisq Easy](https://bisq.wiki/Bisq_Easy)");
        List<SupportMarkdownDocument.Segment> segments = document.lines().get(0).segments();

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(segments).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "- ", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.IMAGE, "Wiki", "bisq-icon://wiki"),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " ", null),
                new SupportMarkdownDocument.Segment(
                        SupportMarkdownDocument.SegmentType.LINK,
                        "Bisq Easy",
                        "https://bisq.wiki/Bisq_Easy"));
    }

    @Test
    void rejectsRemoteMarkdownImageUrls() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("![Wiki](https://example.com/wiki.png)");

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "!", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.LINK, "Wiki", "https://example.com/wiki.png"));
    }

    @Test
    void rejectsUrlsContainingBidiControlCharacters() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse(
                "[safe](https://bisq.network/\u202Eattack)");

        assertThat(document.hasMarkdownFormatting()).isFalse();
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(
                        SupportMarkdownDocument.SegmentType.TEXT,
                        "[safe](https://bisq.network/\u202Eattack)",
                        null));
    }
}
