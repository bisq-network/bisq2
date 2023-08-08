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

package bisq.desktop.overlay.bisq_easy.take_offer.price;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.bisq_easy.components.PriceInput;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TakeOfferPriceView extends View<VBox, TakeOfferPriceModel, TakeOfferPriceController> {
    private final MaterialTextField percentage;
    private final ToggleButton useFixPriceToggle;
    private final VBox fieldsBox;
    private final PriceInput priceInput;
    private Subscription percentageFocussedPin, useFixPricePin;

    public TakeOfferPriceView(TakeOfferPriceModel model, TakeOfferPriceController controller, PriceInput priceInput) {
        super(new VBox(10), model, controller);
        this.priceInput = priceInput;

        root.setAlignment(Pos.TOP_CENTER);

        // root.setPadding(new Insets(40, 200, 0, 160));

        Label headLine = new Label(Res.get("bisqEasy.price.headline"));
        headLine.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.takeOffer.price.subtitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(400);

        percentage = new MaterialTextField(Res.get("bisqEasy.price.percentage"));

        useFixPriceToggle = new ToggleButton();
        useFixPriceToggle.setGraphic(ImageUtil.getImageViewById("toggle_price"));
        useFixPriceToggle.getStyleClass().add("icon-button");
        useFixPriceToggle.setTooltip(new BisqTooltip(Res.get("bisqEasy.price.toggle.tooltip")));

        fieldsBox = new VBox(20, priceInput.getRoot(), percentage);
        fieldsBox.setAlignment(Pos.TOP_CENTER);
        fieldsBox.setMinWidth(400);

        HBox.setMargin(fieldsBox, new Insets(0, 0, 0, 44));
        HBox hBox = new HBox(10, fieldsBox, useFixPriceToggle);
        hBox.setAlignment(Pos.CENTER);
        VBox.setMargin(headLine, new Insets(60, 0, 0, 0));
        root.getChildren().addAll(headLine, subtitleLabel, hBox);
    }

    @Override
    protected void onViewAttached() {
        percentage.textProperty().bindBidirectional(model.getPercentageAsString());

        percentageFocussedPin = EasyBind.subscribe(percentage.textInputFocusedProperty(), controller::onPercentageFocussed);
        useFixPriceToggle.setSelected(model.getUseFixPrice().get());
        useFixPriceToggle.setOnAction(e -> controller.onToggleUseFixPrice());

        useFixPricePin = EasyBind.subscribe(model.getUseFixPrice(), useFixPrice -> {
            if (useFixPrice) {
                priceInput.getRoot().toBack();
            } else {
                priceInput.getRoot().toFront();
            }

            if (useFixPrice) {
                percentage.setEditable(false);
                percentage.deselect();
                priceInput.setEditable(true);
                priceInput.requestFocus();
            } else {
                priceInput.setEditable(false);
                priceInput.deselect();
                percentage.setEditable(true);
                percentage.requestFocus();
            }
        });

        // Needed to trigger focusOut event on amount components
        // We handle all parents mouse events.
        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(e -> root.requestFocus());
            node = node.getParent();
        }
    }

    @Override
    protected void onViewDetached() {
        percentage.textProperty().unbindBidirectional(model.getPercentageAsString());

        percentageFocussedPin.unsubscribe();
        useFixPricePin.unsubscribe();

        useFixPriceToggle.setOnAction(null);

        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(null);
            node = node.getParent();
        }
    }
}
