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

package bisq.desktop.primary.main.content.swap.create.components;

import bisq.offer.Direction;
import bisq.common.monetary.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectionSelection {
    public static class DirectionController implements Controller {
        private final DirectionModel model;
        @Getter
        private final AmountPriceView view;
        private final ChangeListener<Market> selectedMarketListener;

        public DirectionController(OfferPreparationModel offerPreparationModel) {
            model = new DirectionModel(offerPreparationModel);
            view = new AmountPriceView(model, this);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    model.baseCode.set(newValue.baseCurrencyCode());
                }
            };
        }

        public void onViewAttached() {
            model.selectedMarketProperty().addListener(selectedMarketListener);
        }

        public void onViewDetached() {
            model.selectedMarketProperty().removeListener(selectedMarketListener);
        }

        public void onBuySelected() {
            model.setDirection(Direction.BUY);
        }

        public void onSellSelected() {
            model.setDirection(Direction.SELL);
        }
    }

    private static class DirectionModel implements Model {
        public final StringProperty baseCode = new SimpleStringProperty();
        @Delegate
        private final OfferPreparationModel offerPreparationModel;

        public DirectionModel(OfferPreparationModel offerPreparationModel) {
            this.offerPreparationModel = offerPreparationModel;
        }
    }

    @Slf4j
    public static class AmountPriceView extends View<VBox, DirectionModel, DirectionController> {
        private final BisqButton buy, sell;
        private final ChangeListener<String> baseCodeListener;
        private final ChangeListener<Direction> directionListener;

        public AmountPriceView(DirectionModel model, DirectionController controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            Label headline = new BisqLabel(Res.offerbook.get("createOffer.selectOfferType"));
            headline.getStyleClass().add("titled-group-bg-label-active");

            buy = new BisqButton();
            ImageView buyIcon = new ImageView();
            buyIcon.setId("image-buy-white");
            buy.setGraphic(buyIcon);

            ImageView sellIcon = new ImageView();
            sellIcon.setId("image-sell-white");
            sell = new BisqButton();
            sell.setGraphic(sellIcon);

            HBox hBox = new HBox();
            hBox.getChildren().addAll(buy, sell);

            root.getChildren().addAll(headline, hBox);

            baseCodeListener = (observable, oldValue, newValue) -> {
                if (newValue == null) return;
                buy.setText(Res.offerbook.get("direction.label.buy", newValue));
                sell.setText(Res.offerbook.get("direction.label.sell", newValue));
            };
            directionListener = (observable, oldValue, newValue) -> {
                if (newValue == null) return;
                if (newValue == Direction.BUY) {
                    buy.setId("buy-button");
                    sell.setId("button-inactive");
                } else {
                    buy.setId("button-inactive");
                    sell.setId("sell-button");
                }
            };
        }

        public void onViewAttached() {
            buy.setOnAction(e -> controller.onBuySelected());
            sell.setOnAction(e -> controller.onSellSelected());
            model.baseCode.addListener(baseCodeListener);
            model.directionProperty().addListener(directionListener);
        }

        public void onViewDetached() {
            buy.setOnAction(null);
            sell.setOnAction(null);
            model.baseCode.removeListener(baseCodeListener);
            model.directionProperty().removeListener(directionListener);
        }
    }
}