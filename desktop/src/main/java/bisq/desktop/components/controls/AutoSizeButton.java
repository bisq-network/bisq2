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

package bisq.desktop.components.controls;

import com.sun.javafx.scene.control.LabeledText;
import com.sun.javafx.scene.control.behavior.TextBinding;
import com.sun.javafx.scene.control.skin.Utils;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Labeled;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

import static javafx.scene.control.ContentDisplay.LEFT;
import static javafx.scene.control.ContentDisplay.RIGHT;

/**
 * Button which never truncates the label text.
 * It sets it's minWidth to the required width of the label text to not truncate.
 * The OverrunStyle enum does not support a NONE case and setting the OverrunStyle to null will
 * fall back to the default ELLIPSIS enum entry. So we cannot use that property for telling the button to never truncate.
 */
@Slf4j
public class AutoSizeButton extends Button {
    public AutoSizeButton() {
    }

    public AutoSizeButton(String text) {
        super(text);
    }

    public AutoSizeButton(String text, Node graphic) {
        super(text, graphic);
    }

    @Override
    protected javafx.scene.control.Skin<?> createDefaultSkin() {
        return new AutoSizeButtonSkin(this);
    }

    /**
     * We copied the private methods from LabeledSkinBase and adjusted it for our needs.
     */
    private static class AutoSizeButtonSkin extends ButtonSkin {
        private final LabeledText text;
        private TextBinding bindings;
        private final Node graphic;

        public AutoSizeButtonSkin(Button control) {
            super(control);

            text = getChildren().stream()
                    .filter(e -> e instanceof LabeledText)
                    .map(e -> (LabeledText) e).findAny()
                    .orElse(new LabeledText(control));
            graphic = getSkinnable().getGraphic();
        }

        @Override
        protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
            return computeMinLabeledPartWidth(height, topInset, rightInset, bottomInset, leftInset);
        }

        private double computeMinLabeledPartWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
            final Labeled labeled = getSkinnable();
            final ContentDisplay contentDisplay = labeled.getContentDisplay();
            final double gap = labeled.getGraphicTextGap();
            double minTextWidth = 0;
            final Font font = text.getFont();
            final String cleanText = getCleanText();
            final boolean emptyText = cleanText == null || cleanText.isEmpty();

            if (!emptyText) {
                minTextWidth = Utils.computeTextWidth(font, cleanText, 0);
            }

            // Now inspect the graphic and the hpos to determine the the minWidth
            final Node graphic = labeled.getGraphic();
            double width;
            boolean ignoreText = isIgnoreText();
            if (isIgnoreGraphic()) {
                width = minTextWidth;
            } else if (ignoreText) {
                width = graphic.minWidth(-1);
            } else if (contentDisplay == LEFT || contentDisplay == RIGHT) {
                width = (minTextWidth + graphic.minWidth(-1) + gap);
            } else {
                width = Math.max(minTextWidth, graphic.minWidth(-1));
            }

            double padding = leftInset + rightInset;
            if (!ignoreText) {
                padding += leftLabelPadding() + rightLabelPadding();
            }
            return width + padding;
        }

        private String getCleanText() {
            Labeled labeled = getSkinnable();
            String sourceText = labeled.getText();

            if (sourceText != null && labeled.isMnemonicParsing()) {
                if (bindings == null) {
                    bindings = new TextBinding(sourceText);
                } else {
                    bindings.update(sourceText);
                }

                return bindings.getText();
            }

            return sourceText;
        }

        private boolean isIgnoreGraphic() {
            return (graphic == null ||
                    !graphic.isManaged() ||
                    getSkinnable().getContentDisplay() == ContentDisplay.TEXT_ONLY);
        }

        private boolean isIgnoreText() {
            final Labeled labeled = getSkinnable();
            final String txt = getCleanText();
            return (txt == null ||
                    txt.equals("") ||
                    labeled.getContentDisplay() == ContentDisplay.GRAPHIC_ONLY);
        }

        private double leftLabelPadding() {
            return snapSizeX(getSkinnable().getLabelPadding().getLeft());
        }

        private double rightLabelPadding() {
            return snapSizeX(getSkinnable().getLabelPadding().getRight());
        }
    }
}