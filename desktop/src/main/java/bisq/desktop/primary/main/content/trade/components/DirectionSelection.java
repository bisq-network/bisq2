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
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.components.controls.BisqLabel;
import bisq.i18n.Res;
import bisq.offer.spec.Direction;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DirectionSelection {
    private final Controller controller;

    public DirectionSelection() {
        controller = new Controller();
    }

    public Pane getRoot() {
        return controller.view.getRoot();
    }

    public ReadOnlyObjectProperty<Direction> directionProperty() {
        return controller.model.direction;
    }

    public void setDirection(Direction direction) {
        controller.model.direction.set(direction);
    }

    public void hideDirection(Direction direction) {
        controller.hideDirection(direction);
    }

    public void setIsTakeOffer() {
        controller.model.isCreateOffer = false;
    }

    public void setSelectedMarket(Market selectedMarket) {
        controller.setSelectedMarket(selectedMarket);
    }

    private static class Controller implements bisq.desktop.common.view.Controller {

        private final Model model;
        @Getter
        private final View view;

        private Controller() {
            model = new Model();
            view = new View(model, this);
        }

        private void setSelectedMarket(Market selectedMarket) {
            model.selectedMarket = selectedMarket;
            if (selectedMarket != null) {
                model.baseCode.set(selectedMarket.baseCurrencyCode());
            }
        }

        @Override
        public void onActivate() {
            if (model.selectedMarket != null) {
                model.baseCode.set(model.selectedMarket.baseCurrencyCode());
            }
            if (model.direction.get() == null) {
                model.direction.set(Direction.BUY);
            }
        }

        @Override
        public void onDeactivate() {
        }

        private void onBuySelected() {
            model.direction.set(Direction.BUY);
        }

        private void onSellSelected() {
            model.direction.set(Direction.SELL);
        }

        private void hideDirection(Direction direction) {
            model.isBuySideVisible.set(direction.isSell());
            model.isSellSideVisible.set(direction.isBuy());
        }
    }

    private static class Model implements bisq.desktop.common.view.Model {
        private final ObjectProperty<Direction> direction = new SimpleObjectProperty<>();
        private final StringProperty baseCode = new SimpleStringProperty();
        private boolean isCreateOffer = true;
        private final BooleanProperty isBuySideVisible = new SimpleBooleanProperty();
        private final BooleanProperty isSellSideVisible = new SimpleBooleanProperty();

        private Market selectedMarket;

        private Model() {
        }
    }

    @Slf4j
    public static class View extends bisq.desktop.common.view.View<VBox, Model, Controller> {
        private final BisqButton buy, sell;
        private final ChangeListener<String> baseCodeListener;
        private final ChangeListener<Direction> directionListener;
        private final BisqLabel headline;

        private View(Model model, Controller controller) {
            super(new VBox(), model, controller);

            root.setSpacing(10);
            headline = new BisqLabel(Res.get("createOffer.selectOfferType"));
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
        protected void onViewAttached() {
            if (model.isCreateOffer) {
                buy.setOnAction(e -> controller.onBuySelected());
                sell.setOnAction(e -> controller.onSellSelected());
            } else {
                // disable changes style
                // will be used anyway diff design later. current solution is just for prototype dev
                buy.setMouseTransparent(true);
                sell.setMouseTransparent(true);
                headline.setText(Res.get("takeOffer.offerType"));

                buy.visibleProperty().bind(model.isBuySideVisible);
                buy.managedProperty().bind(model.isBuySideVisible);
                sell.visibleProperty().bind(model.isSellSideVisible);
                sell.managedProperty().bind(model.isSellSideVisible);
            }
            model.baseCode.addListener(baseCodeListener);
            model.direction.addListener(directionListener);
            applyBaseCodeChange();
            applyDirectionChange();
        }

        @Override
        protected void onViewDetached() {
            if (model.isCreateOffer) {
                buy.setOnAction(null);
                sell.setOnAction(null);
            } else {
                buy.visibleProperty().unbind();
                buy.managedProperty().unbind();
                sell.visibleProperty().unbind();
                sell.managedProperty().unbind();
            }
            model.baseCode.removeListener(baseCodeListener);
            model.direction.removeListener(directionListener);
        }

        private void applyBaseCodeChange() {
            String baseCode = model.baseCode.get();
            if (baseCode == null) return;
            buy.setText(Res.get("direction.label.buy", baseCode));
            sell.setText(Res.get("direction.label.sell", baseCode));
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