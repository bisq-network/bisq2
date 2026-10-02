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

import bisq.desktop.testutil.JavaFxToolkitExtension;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JavaFxToolkitExtension.class)
class SupportMarkdownRendererTest {
    // TestFX keeps an exception which a task of an earlier test threw on the JavaFX thread, and throws it in the
    // next test which starts a task there. These tests must not fail for an exception of another test class.
    @BeforeEach
    void clearExceptionsOfEarlierTests() {
        WaitForAsyncUtils.clearExceptions();
    }

    // The label of a link comes from the sender of the message. The reader must see where a link leads, so the
    // text of a hyperlink is the URL which it opens, and the label is plain text in front of it.
    @Test
    void linkWithALabelShowsTheLabelAsTextAndTheTargetAsHyperlink() {
        assertThat(render("See [docs](https://bisq.network) please")).containsExactly(
                text("See "),
                text("docs ("),
                hyperlink("https://bisq.network"),
                text(")"),
                text(" please"));
    }

    @Test
    void labelWhichLooksLikeAnotherUrlIsNotPartOfTheHyperlink() {
        assertThat(render("[https://bisq.network/downloads](https://evil.example/Bisq.exe)")).containsExactly(
                text("https://bisq.network/downloads ("),
                hyperlink("https://evil.example/Bisq.exe"),
                text(")"));
    }

    @Test
    void linkWhoseLabelEqualsItsTargetShowsTheTargetOnce() {
        assertThat(render("[https://bisq.network](https://bisq.network)")).containsExactly(
                hyperlink("https://bisq.network"));
    }

    @Test
    void autoLinkShowsItsTarget() {
        assertThat(render("Use https://bisq.network")).containsExactly(
                text("Use "),
                hyperlink("https://bisq.network"));
    }

    // The parser creates no link without a URL, but the type of the segment allows it
    @Test
    void linkWithoutATargetIsShownAsText() {
        SupportMarkdownDocument document = new SupportMarkdownDocument(List.of(new SupportMarkdownDocument.Line(
                SupportMarkdownDocument.LineType.TEXT,
                List.of(new SupportMarkdownDocument.Segment(SupportMarkdownDocument.SegmentType.LINK, "label", null)))),
                true);

        assertThat(render(document)).containsExactly(text("label"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[docs](https://bisq.network)",
            "[https://bisq.network/downloads](https://evil.example/Bisq.exe)",
            "[bisq.network](https://evil.example)",
            "### [Download](https://evil.example/Bisq.exe) or https://bisq.network/downloads",
            "- ![FAQ](bisq-icon://faq) [FAQ] [What is Bisq Easy?](https://bisq.network/faq/what-is-bisq-easy)",
            "first [a](https://a.example)\nsecond [b](https://b.example) and [c](https://c.example)"
    })
    void everyHyperlinkShowsTheTargetOfItsLink(String markdown) {
        SupportMarkdownDocument document = SupportMarkdownParser.parse(markdown);
        List<String> targets = document.lines().stream()
                .flatMap(line -> line.segments().stream())
                .filter(segment -> segment.type() == SupportMarkdownDocument.SegmentType.LINK)
                .map(SupportMarkdownDocument.Segment::url)
                .toList();

        List<String> hyperlinkTexts = render(document).stream()
                .filter(node -> node.type().equals(Hyperlink.class.getSimpleName()))
                .map(RenderedNode::text)
                .toList();

        assertThat(targets).isNotEmpty();
        assertThat(hyperlinkTexts).isEqualTo(targets);
    }

    @Test
    void mapsFaqAndWikiSourceIconsToChatAndBookGlyphs() {
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://faq"))
                .isEqualTo("open-p-chat-grey");
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://wiki"))
                .isEqualTo("nav-learn");
    }

    @Test
    void sourceIconMappingIsCaseInsensitiveAndSafeForUnknownInput() {
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("BISQ-ICON://FAQ"))
                .isEqualTo("open-p-chat-grey");
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl("bisq-icon://unknown"))
                .isNull();
        assertThat(SupportMarkdownRenderer.getIconIdForSourceUrl(null))
                .isNull();
    }

    private static List<RenderedNode> render(String markdown) {
        return render(SupportMarkdownParser.parse(markdown));
    }

    // The text nodes and hyperlinks of all lines, in the order in which the reader sees them
    private static List<RenderedNode> render(SupportMarkdownDocument document) {
        List<RenderedNode> renderedNodes = new ArrayList<>();
        JavaFxToolkitExtension.runOnFxThread(() -> {
            VBox root = SupportMarkdownRenderer.render(document, Pos.CENTER_LEFT);
            for (Node line : root.getChildren()) {
                if (line instanceof TextFlow textFlow) {
                    for (Node node : textFlow.getChildren()) {
                        if (node instanceof Text text) {
                            renderedNodes.add(text(text.getText()));
                        } else if (node instanceof Hyperlink hyperlink) {
                            renderedNodes.add(hyperlink(hyperlink.getText()));
                        }
                    }
                }
            }
        });
        return renderedNodes;
    }

    private static RenderedNode text(String text) {
        return new RenderedNode(Text.class.getSimpleName(), text);
    }

    private static RenderedNode hyperlink(String text) {
        return new RenderedNode(Hyperlink.class.getSimpleName(), text);
    }

    private record RenderedNode(String type, String text) {
    }
}
