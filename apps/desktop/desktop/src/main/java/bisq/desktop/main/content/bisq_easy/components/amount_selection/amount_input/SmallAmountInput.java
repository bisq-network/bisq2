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

package bisq.desktop.main.content.bisq_easy.components.amount_selection.amount_input;

import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SmallAmountInput extends AmountInput {
    private static final double ICON_SCALE = 0.8;
    private static final double ICON_OPACITY = 0.5;
    private static final String DEFAULT_TOOLTIP = "bisqEasy.component.amount.baseSide.tooltip.btcAmount.marketPrice";
    private static final String QUOTE_AMOUNT_ID = "quote-amount-text-field";

    public SmallAmountInput(boolean isBaseCurrency) {
        super(isBaseCurrency);

        this.controller.setModel(new SmallAmountInputModel(isBaseCurrency));
        this.controller.setView(new SmallAmountInputView(controller.model, controller));
    }

    public void setTooltip(String tooltip) {
        ((SmallAmountInputModel) controller.model).setTooltipText(tooltip);
    }

    public void setUseLowPrecision(boolean useLowPrecision) {
        controller.model.setUseLowPrecision(useLowPrecision);
    }

    private static class SmallAmountInputView extends View {
        private BisqTooltip tooltip;
        private Button iconButton;

        protected SmallAmountInputView(Model model, Controller controller) {
            super(model, controller);
        }

        private Button createIconButton() {
            Button iconButton = BisqIconButton.createIconButton("info");
            iconButton.setScaleX(ICON_SCALE);
            iconButton.setScaleY(ICON_SCALE);
            iconButton.setOpacity(ICON_OPACITY);
            tooltip = new BisqTooltip(BisqTooltip.Style.DARK);
            iconButton.setTooltip(tooltip);
            HBox.setMargin(iconButton, new Insets(0, 0, 5, 0));
            return iconButton;
        }

        @Override
        protected void initView() {
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("small-amount-input");
            iconButton = createIconButton();
            root.getChildren().add(iconButton);
        }

        @Override
        protected TextField createTextInput() {
            var textInput = new TextField();
            textInput.setId(QUOTE_AMOUNT_ID);
            textInput.getStyleClass().add("text-input");
            textInput.setPadding(new Insets(0, 7, 3, 0));
            return textInput;
        }

        @Override
        protected Label createCodeLabel() {
            var codeLabel = new Label();
            codeLabel.setId(QUOTE_AMOUNT_ID);
            codeLabel.getStyleClass().add("currency-code");
            codeLabel.setMinWidth(Label.USE_PREF_SIZE);
            return codeLabel;
        }

        @Override
        protected void onViewAttached() {
            super.onViewAttached();

            tooltip.textProperty().bind(((SmallAmountInputModel) model).tooltipProperty());
            iconButton.visibleProperty().bind(model.showHyphenInsteadOfCurrencyCode);
            iconButton.managedProperty().bind(model.showHyphenInsteadOfCurrencyCode);
        }

        @Override
        protected void onViewDetached() {
            super.onViewDetached();

            tooltip.textProperty().unbind();
            iconButton.visibleProperty().unbind();
            iconButton.managedProperty().unbind();
        }
    }

    private static class SmallAmountInputModel extends Model {
        private final StringProperty tooltip = new SimpleStringProperty(Res.get(DEFAULT_TOOLTIP));

        protected SmallAmountInputModel(boolean isBaseCurrency) {
            super(isBaseCurrency);
        }

        public StringProperty tooltipProperty() {
            return tooltip;
        }

        public void setTooltipText(String value) {
            tooltip.set(value);
        }
    }
}
