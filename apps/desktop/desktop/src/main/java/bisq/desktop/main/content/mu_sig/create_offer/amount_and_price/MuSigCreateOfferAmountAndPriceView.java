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

package bisq.desktop.main.content.mu_sig.create_offer.amount_and_price;

import bisq.desktop.common.ManagedDuration;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.view.View;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferView;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class MuSigCreateOfferAmountAndPriceView extends View<VBox, MuSigCreateOfferAmountAndPriceModel, MuSigCreateOfferAmountAndPriceController> {
    private final Label headline, amountAtPriceSymbol;
    private final VBox priceSelection, priceOverlay, content, amountOverlay;
    private Subscription isAmountOverlayVisiblePin, isPriceOverlayVisiblePin;

    public MuSigCreateOfferAmountAndPriceView(MuSigCreateOfferAmountAndPriceModel model,
                                              MuSigCreateOfferAmountAndPriceController controller,
                                              VBox amountSelection,
                                              VBox amountOverlay,
                                              VBox priceSelection,
                                              VBox priceOverlay) {
        super(new VBox(), model, controller);

        this.amountOverlay = amountOverlay;
        this.priceSelection = priceSelection;
        this.priceOverlay = priceOverlay;
        headline = new Label();
        headline.getStyleClass().add("bisq-text-headline-2");
        amountAtPriceSymbol = new Label("@");
        amountAtPriceSymbol.getStyleClass().add("amount-at-price-symbol");

        HBox amountAndPriceHBox = new HBox(20, amountSelection, amountAtPriceSymbol, priceSelection);
        amountAndPriceHBox.getStyleClass().add("amount-and-price-box");

        content = new VBox(20, headline, amountAndPriceHBox);
        content.getStyleClass().add("content-box");

        StackPane layeredContent = new StackPane(content, amountOverlay, priceOverlay);
        StackPane.setMargin(amountOverlay, new Insets(-MuSigCreateOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        // For unclear reasons the priceOverlay is not centered
        StackPane.setMargin(priceOverlay, new Insets(-MuSigCreateOfferView.TOP_PANE_HEIGHT, 0, 0, 70));
        root.getChildren().addAll(layeredContent);
        root.getStyleClass().add("amount-and-price-step");
    }

    @Override
    protected void onViewAttached() {
        headline.textProperty().set(model.getHeadline());

        isAmountOverlayVisiblePin = EasyBind.subscribe(model.getIsAmountOverlayVisible(), isAmountOverlayVisible -> {
            if (isAmountOverlayVisible) {
                amountOverlay.setVisible(true);
                amountOverlay.setOpacity(1);
                Transitions.blurStrong(content, 0);
                Transitions.slideInTop(amountOverlay, 450);
            } else {
                Transitions.removeEffect(content);
                if (amountOverlay.isVisible()) {
                    Transitions.fadeOut(amountOverlay, ManagedDuration.getHalfOfDefaultDurationMillis(),
                            () -> amountOverlay.setVisible(false));
                }
            }
        });

        isPriceOverlayVisiblePin = EasyBind.subscribe(model.getIsPriceOverlayVisible(), isPriceOverlayVisible -> {
            if (isPriceOverlayVisible) {
                priceOverlay.setVisible(true);
                priceOverlay.setOpacity(1);
                Transitions.blurStrong(content, 0);
                Transitions.slideInTop(priceOverlay, 450);
            } else {
                Transitions.removeEffect(content);
                if (priceOverlay.isVisible()) {
                    Transitions.fadeOut(priceOverlay, ManagedDuration.getHalfOfDefaultDurationMillis(),
                            () -> priceOverlay.setVisible(false));
                }
            }
        });
    }

    @Override
    protected void onViewDetached() {
        isAmountOverlayVisiblePin.unsubscribe();
        isPriceOverlayVisiblePin.unsubscribe();
    }
}
