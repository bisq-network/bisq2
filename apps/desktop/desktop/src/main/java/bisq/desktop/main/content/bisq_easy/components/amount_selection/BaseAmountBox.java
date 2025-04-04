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

package bisq.desktop.main.content.bisq_easy.components.amount_selection;

import bisq.common.currency.Market;
import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.Monetary;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.BitcoinAmountDisplay;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BaseAmountBox {
    private static final double ICON_SCALE = 0.85;
    private static final double ICON_OPACITY = 0.5;
    private static final String DEFAULT_TOOLTIP = "bisqEasy.component.amount.baseSide.tooltip.btcAmount.marketPrice";
    private static final String AMOUNT_TEXT_ID = "quote-amount-text-field";

    private final Controller controller;

    public BaseAmountBox(boolean showCurrencyCode) {
        controller = new Controller(showCurrencyCode);
    }

    public ReadOnlyObjectProperty<Monetary> amountProperty() {
        return controller.model.amount;
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    public void setAmount(Monetary value) {
        controller.model.amount.set(value);
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public void reset() {
        controller.model.reset();
    }

    public void setTooltip(String tooltip) {
        controller.model.tooltip.set(tooltip);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {
        private final Model model;
        @Getter
        private final View view;

        private Controller(boolean showCurrencyCode) {
            model = new Model(showCurrencyCode);
            view = new View(model, this);
        }

        @Override
        public void onActivate() {
            model.amount.set(null);
            updateModel();
        }

        @Override
        public void onDeactivate() {
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            model.amount.set(null);
            updateModel();
        }

        private void updateModel() {
            if (model.selectedMarket == null) {
                model.code.set("");
                return;
            }
            model.code.set(model.selectedMarket.getBaseCurrencyCode());
            model.isBtc.set(TradeCurrency.isBtc(model.code.get()));
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final boolean showCurrencyCode;
        private final ObjectProperty<Monetary> amount = new SimpleObjectProperty<>();
        private final StringProperty code = new SimpleStringProperty();
        private final StringProperty tooltip = new SimpleStringProperty(Res.get(DEFAULT_TOOLTIP));
        private final BooleanProperty isBtc = new SimpleBooleanProperty(false);
        private Market selectedMarket;

        private Model(boolean showCurrencyCode) {
            this.showCurrencyCode = showCurrencyCode;
        }

        void reset() {
            amount.set(null);
            code.set(null);
            isBtc.set(false);
            selectedMarket = null;
        }
    }

    private static class View extends bisq.desktop.common.view.View<HBox, Model, Controller> {
        private final ChangeListener<Monetary> amountListener;
        private final ChangeListener<Boolean> isBtcListener;
        private final ChangeListener<String> codeListener;

        private final Label baseAmountLabel, codeLabel, baseAmountInfoIcon;
        private final BitcoinAmountDisplay bitcoinAmountDisplay;
        private final BisqTooltip tooltip;

        private View(Model model, Controller controller) {
            super(new HBox(), model, controller);

            baseAmountLabel = new Label();
            baseAmountLabel.setId(AMOUNT_TEXT_ID);
            baseAmountLabel.setPadding(new Insets(0, 7, 3, 0));
            baseAmountLabel.getStyleClass().add("base-amount-label");

            codeLabel = new Label();
            codeLabel.setId(AMOUNT_TEXT_ID);
            codeLabel.setMinWidth(Label.USE_PREF_SIZE);
            codeLabel.getStyleClass().add("currency-code");
            codeLabel.setVisible(model.showCurrencyCode);
            codeLabel.setManaged(model.showCurrencyCode);

            bitcoinAmountDisplay = new BitcoinAmountDisplay("0");
            configureBitcoinAmountDisplay(bitcoinAmountDisplay);
            bitcoinAmountDisplay.setVisible(false);
            bitcoinAmountDisplay.setManaged(false);

            tooltip = new BisqTooltip(BisqTooltip.Style.DARK);
            baseAmountInfoIcon = new Label();
            baseAmountInfoIcon.setGraphic(ImageUtil.getImageViewById("info"));
            baseAmountInfoIcon.setTooltip(tooltip);
            baseAmountInfoIcon.setScaleX(ICON_SCALE);
            baseAmountInfoIcon.setScaleY(ICON_SCALE);
            baseAmountInfoIcon.setOpacity(ICON_OPACITY);
            baseAmountInfoIcon.getStyleClass().add("base-amount-info-icon");
            baseAmountInfoIcon.setMinWidth(Label.USE_PREF_SIZE);

            HBox.setMargin(baseAmountInfoIcon, new Insets(0, 0, 0, 4));
            HBox.setMargin(bitcoinAmountDisplay, new Insets(0, 7, 0, 0));

            root.getChildren().addAll(baseAmountLabel, codeLabel, bitcoinAmountDisplay, baseAmountInfoIcon);
            root.setAlignment(Pos.CENTER);
            root.getStyleClass().add("base-amount-box");

            amountListener = this::onAmountChanged;
            isBtcListener = this::onIsBtcChanged;
            codeListener = this::onCodeChanged;
        }

        @Override
        protected void onViewAttached() {
            boolean isBtc = model.isBtc.get();

            baseAmountInfoIcon.setVisible(model.showCurrencyCode);
            baseAmountInfoIcon.setManaged(model.showCurrencyCode);

            if (isBtc) {
                baseAmountInfoIcon.setTranslateY(-2);
            }

            codeLabel.textProperty().bind(model.code);
            tooltip.textProperty().bind(model.tooltip);

            model.amount.addListener(amountListener);
            model.isBtc.addListener(isBtcListener);
            model.code.addListener(codeListener);

            updateDisplayMode(isBtc);
            applyAmount(model.amount.get());
        }

        @Override
        protected void onViewDetached() {
            codeLabel.textProperty().unbind();
            tooltip.textProperty().unbind();

            model.amount.removeListener(amountListener);
            model.isBtc.removeListener(isBtcListener);
            model.code.removeListener(codeListener);
        }

        private void onIsBtcChanged(ObservableValue<? extends Boolean> observable,
                                    Boolean oldValue,
                                    Boolean newValue) {
            updateDisplayMode(newValue);
            applyAmount(model.amount.get());
        }

        private void onCodeChanged(ObservableValue<? extends String> observable,
                                   String oldValue,
                                   String newValue) {
            model.isBtc.set(TradeCurrency.isBtc(newValue));
        }

        private void onAmountChanged(ObservableValue<? extends Monetary> observable,
                                     Monetary oldValue,
                                     Monetary newValue) {
            applyAmount(newValue);
        }

        private void updateDisplayMode(boolean isBtc) {
            baseAmountLabel.setVisible(!isBtc);
            baseAmountLabel.setManaged(!isBtc);
            codeLabel.setVisible(!isBtc && model.showCurrencyCode);
            codeLabel.setManaged(!isBtc && model.showCurrencyCode);

            bitcoinAmountDisplay.setVisible(isBtc);
            bitcoinAmountDisplay.setManaged(isBtc);
        }

        private void applyAmount(Monetary newValue) {
            if (newValue == null) {
                baseAmountLabel.setText("");
                bitcoinAmountDisplay.setBtcAmount("0");
                return;
            }

            String formattedAmount = AmountFormatter.formatBaseAmount(newValue);
            baseAmountLabel.setText(formattedAmount);

            if (model.isBtc.get()) {
                bitcoinAmountDisplay.setBtcAmount(formattedAmount);
            }
        }

        private void configureBitcoinAmountDisplay(BitcoinAmountDisplay btcText) {
            btcText.setBaselineAlignment();
            btcText.applyCompactConfig(16, 13, 28);
            btcText.getBtcCode().setVisible(model.showCurrencyCode);
            btcText.getBtcCode().setManaged(model.showCurrencyCode);
        }
    }
}