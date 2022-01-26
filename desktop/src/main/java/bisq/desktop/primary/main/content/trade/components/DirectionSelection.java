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

package bisq.desktop.primary.main.content.trade.components;

import bisq.common.monetary.Market;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.offer.Direction;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectionSelection {
    private final DirectionController controller;

    public DirectionSelection(ReadOnlyObjectProperty<Market> selectedMarket) {
        controller = new DirectionController(selectedMarket);
    }

    public DirectionView getView() {
        return controller.view;
    }

    public ReadOnlyObjectProperty<Direction> directionProperty() {
        return controller.model.direction;
    }

    public void setDirection(Direction direction) {
        controller.model.direction.set(direction);
    }

    public void setIsTakeOffer() {
        controller.model.isCreateOffer = false;
    }

    private static class DirectionController implements Controller {

        private final DirectionModel model;
        @Getter
        private final DirectionView view;
        private final ChangeListener<Market> selectedMarketListener;

        private DirectionController(ReadOnlyObjectProperty<Market> selectedMarket) {
            model = new DirectionModel(selectedMarket);
            view = new DirectionView(model, this);

            selectedMarketListener = (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    model.baseCode.set(newValue.baseCurrencyCode());
                }
            };
        }

        @Override
        public void onViewAttached() {
            model.selectedMarket.addListener(selectedMarketListener);
            if (model.selectedMarket.get() != null) {
                model.baseCode.set(model.selectedMarket.get().baseCurrencyCode());
            }
            if (model.direction.get() == null) {
                model.direction.set(Direction.BUY);
            }
        }

        @Override
        public void onViewDetached() {
            model.selectedMarket.removeListener(selectedMarketListener);
        }

        private void onBuySelected() {
            model.direction.set(Direction.BUY);
        }

        private void onSellSelected() {
            model.direction.set(Direction.SELL);
        }
    }

    private static class DirectionModel implements Model {
        private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>();
        private final ReadOnlyObjectProperty<Market> selectedMarket;
        private final StringProperty baseCode = new SimpleStringProperty();
        private boolean isCreateOffer = true;

        private DirectionModel(ReadOnlyObjectProperty<Market> selectedMarket) {
            this.selectedMarket = selectedMarket;
        }
    }

    @Slf4j
    public static class DirectionView extends View<VBox, DirectionModel, DirectionController> {
        private final BisqButton buy, sell;
        private final ChangeListener<String> baseCodeListener;
        private final ChangeListener<Direction> directionListener;
        private final BisqLabel headline;

        private DirectionView(DirectionModel model, DirectionController controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            headline = new BisqLabel(Res.offerbook.get("createOffer.selectOfferType"));
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

            baseCodeListener = (observable, oldValue, newValue) -> applyBaseCodeChange();
            directionListener = (observable, oldValue, newValue) -> applyDirectionChange();
        }


        @Override
        public void onViewAttached() {
            if (model.isCreateOffer) {
                buy.setOnAction(e -> controller.onBuySelected());
                sell.setOnAction(e -> controller.onSellSelected());
            } else {
                // disable changes style
                // will be used anyway diff design later. current solution is just for prototype dev
                buy.setMouseTransparent(true);
                sell.setMouseTransparent(true);
                headline.setText(Res.offerbook.get("takeOffer.offerType"));
            }
            model.baseCode.addListener(baseCodeListener);
            model.direction.addListener(directionListener);
            applyBaseCodeChange();
            applyDirectionChange();

        }

        @Override
        public void onViewDetached() {
            if (model.isCreateOffer) {
                buy.setOnAction(null);
                sell.setOnAction(null);
            }
            model.baseCode.removeListener(baseCodeListener);
            model.direction.removeListener(directionListener);

        }

        private void applyBaseCodeChange() {
            String baseCode = model.baseCode.get();
            if (baseCode == null) return;
            buy.setText(Res.offerbook.get("direction.label.buy", baseCode));
            sell.setText(Res.offerbook.get("direction.label.sell", baseCode));
        }

        private void applyDirectionChange() {
            Direction direction = model.direction.get();
            if (direction == null) return;
            if (direction.isBuy()) {
                buy.setId("buy-button");
                sell.setId("button-inactive");
            } else {
                buy.setId("button-inactive");
                sell.setId("sell-button");
            }
        }
    }
}