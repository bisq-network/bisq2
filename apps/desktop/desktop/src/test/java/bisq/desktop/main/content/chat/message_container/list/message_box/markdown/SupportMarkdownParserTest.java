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
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SupportMarkdownParserTest {
    @Test
    @DisplayName("plain text has no markdown formatting")
    void plain_text_has_no_markdown_formatting() {
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
    @DisplayName("parses inline markdown syntax")
    void parses_inline_markdown_syntax() {
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
    @DisplayName("rejects unsupported link schemes")
    void rejects_unsupported_link_schemes() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("[pwn](javascript:alert(1))");

        assertThat(document.hasMarkdownFormatting()).isFalse();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "[pwn](javascript:alert(1))", null));
    }

    @Test
    @DisplayName("auto links http and https urls")
    void auto_links_http_and_https_urls() {
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
    @DisplayName("supports escaped markdown delimiters")
    void supports_escaped_markdown_delimiters() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("\\*not italic\\* and \\`not code\\`");

        assertThat(document.hasMarkdownFormatting()).isFalse();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "*not italic* and `not code`", null));
    }

    @Test
    @DisplayName("parses horizontal rule markdown line")
    void parses_horizontal_rule_markdown_line() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("before\n---\nafter");

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(document.lines()).hasSize(3);
        assertThat(document.lines().get(0).type()).isEqualTo(SupportMarkdownDocument.LineType.TEXT);
        assertThat(document.lines().get(1).type()).isEqualTo(SupportMarkdownDocument.LineType.HORIZONTAL_RULE);
        assertThat(document.lines().get(1).segments()).isEmpty();
        assertThat(document.lines().get(2).type()).isEqualTo(SupportMarkdownDocument.LineType.TEXT);
    }

    @Test
    @DisplayName("parses level three heading markdown line")
    void parses_level_three_heading_markdown_line() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("### Answer quality");

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(document.lines()).hasSize(1);
        assertThat(document.lines().get(0).type()).isEqualTo(SupportMarkdownDocument.LineType.HEADING_3);
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "Answer quality", null));
    }

    @Test
    @DisplayName("parses bisq icon image markdown")
    void parses_bisq_icon_image_markdown() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("![Wiki](bisq-icon://wiki) source");
        List<SupportMarkdownDocument.Segment> segments = document.lines().get(0).segments();

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(segments).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.IMAGE, "Wiki", "bisq-icon://wiki"),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, " source", null));
    }

    @Test
    @DisplayName("parses source line with icon type and link")
    void parses_source_line_with_icon_type_and_link() {
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
    @DisplayName("removes wiki prefix after source icon case insensitive")
    void removes_wiki_prefix_after_source_icon_case_insensitive() {
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
    @DisplayName("rejects remote markdown image urls")
    void rejects_remote_markdown_image_urls() {
        SupportMarkdownDocument document = SupportMarkdownParser.parse("![Wiki](https://example.com/wiki.png)");

        assertThat(document.hasMarkdownFormatting()).isTrue();
        assertThat(document.lines().get(0).segments()).containsExactly(
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.TEXT, "!", null),
                new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.LINK, "Wiki", "https://example.com/wiki.png"));
    }

    @Test
    @DisplayName("rejects urls containing bidi control characters")
    void rejects_urls_containing_bidi_control_characters() {
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
