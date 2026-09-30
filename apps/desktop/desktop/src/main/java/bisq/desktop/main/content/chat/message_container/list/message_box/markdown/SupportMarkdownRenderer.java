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

import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.ImageUtil;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.util.Locale;

public final class SupportMarkdownRenderer {
    private SupportMarkdownRenderer() {
    }

    public static VBox render(SupportMarkdownDocument document, Pos alignment) {
        VBox root = new VBox(3);
        root.getStyleClass().add("chat-markdown-root");
        root.setFillWidth(true);

        for (SupportMarkdownDocument.Line line : document.lines()) {
            if (line.type() == SupportMarkdownDocument.LineType.HORIZONTAL_RULE) {
                Region rule = new Region();
                rule.getStyleClass().add("chat-markdown-rule");
                rule.setMaxWidth(Double.MAX_VALUE);
                root.getChildren().add(rule);
                continue;
            }

            TextFlow textFlow = new TextFlow();
            textFlow.getStyleClass().add("chat-markdown-line");
            if (line.type() == SupportMarkdownDocument.LineType.HEADING_3) {
                textFlow.getStyleClass().add("chat-markdown-heading-3-line");
            }
            textFlow.prefWidthProperty().bind(root.maxWidthProperty());
            textFlow.maxWidthProperty().bind(root.maxWidthProperty());

            for (SupportMarkdownDocument.Segment segment : line.segments()) {
                textFlow.getChildren().add(createNode(segment, line.type()));
            }

            root.getChildren().add(textFlow);
        }

        applyAlignment(root, alignment);
        return root;
    }

    public static void applyAlignment(VBox root, Pos alignment) {
        root.setAlignment(alignment);
        TextAlignment textAlignment = switch (alignment.getHpos()) {
            case RIGHT -> TextAlignment.RIGHT;
            case CENTER -> TextAlignment.CENTER;
            default -> TextAlignment.LEFT;
        };

        for (Node child : root.getChildren()) {
            if (child instanceof TextFlow textFlow) {
                textFlow.setTextAlignment(textAlignment);
            }
        }
    }

    private static Node createNode(SupportMarkdownDocument.Segment segment,
                                   SupportMarkdownDocument.LineType lineType) {
        if (segment.type() == SupportMarkdownDocument.SegmentType.LINK) {
            Hyperlink hyperlink = new Hyperlink(segment.text());
            hyperlink.getStyleClass().add("chat-markdown-link");
            if (lineType == SupportMarkdownDocument.LineType.HEADING_3) {
                hyperlink.getStyleClass().add("chat-markdown-heading-3");
            }
            hyperlink.setOnAction(e -> {
                if (MarkdownSecurityUtils.isSafeOpenUrl(segment.url())) {
                    Browser.open(segment.url());
                }
            });
            return hyperlink;
        }
        if (segment.type() == SupportMarkdownDocument.SegmentType.IMAGE) {
            return createImageNode(segment, lineType);
        }

        Text text = new Text(segment.text());
        text.getStyleClass().add("chat-markdown-text");
        if (lineType == SupportMarkdownDocument.LineType.HEADING_3) {
            text.getStyleClass().add("chat-markdown-heading-3");
        }
        switch (segment.type()) {
            case BOLD -> text.getStyleClass().add("chat-markdown-bold");
            case ITALIC -> text.getStyleClass().add("chat-markdown-italic");
            case STRIKETHROUGH -> text.setStrikethrough(true);
            case CODE -> text.getStyleClass().add("chat-markdown-code");
            default -> {
            }
        }
        return text;
    }

    private static Node createImageNode(SupportMarkdownDocument.Segment segment,
                                        SupportMarkdownDocument.LineType lineType) {
        String iconId = getIconIdForSourceUrl(segment.url());
        if (iconId == null) {
            Text text = new Text(segment.text());
            text.getStyleClass().add("chat-markdown-text");
            if (lineType == SupportMarkdownDocument.LineType.HEADING_3) {
                text.getStyleClass().add("chat-markdown-heading-3");
            }
            return text;
        }

        ImageView imageView = ImageUtil.getImageViewById(iconId);
        imageView.getStyleClass().add("chat-markdown-image");
        imageView.setFitWidth(14);
        imageView.setFitHeight(14);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    static String getIconIdForSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        return switch (sourceUrl.toLowerCase(Locale.ROOT)) {
            case "bisq-icon://faq" -> "open-p-chat-grey";
            case "bisq-icon://wiki" -> "nav-learn";
            default -> null;
        };
    }
}
