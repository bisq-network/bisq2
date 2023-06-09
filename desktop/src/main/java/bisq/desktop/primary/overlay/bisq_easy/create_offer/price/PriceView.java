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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.price;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class PriceView extends View<VBox, PriceModel, PriceController> {
    private final MaterialTextField percentage, marketPrice;
    private final Switch priceTypeSwitch;
    private Subscription percentageFocussedPin;

    public PriceView(PriceModel model, PriceController controller, Pane priceInputRoot) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        root.setPadding(new Insets(40, 250, 0, 250));

        Label headLine = new Label(Res.get("onboarding.price.headline"));
        headLine.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.price.subtitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(450);

        percentage = new MaterialTextField(Res.get("onboarding.price.percentage"));
        marketPrice = new MaterialTextField();
        marketPrice.setEditable(false);

        priceTypeSwitch = new Switch();
        // VBox.setMargin(percentage, new Insets(5, 0, 0, 0));
        root.getChildren().addAll(headLine, subtitleLabel, percentage, priceInputRoot, marketPrice, priceTypeSwitch);
    }

    @Override
    protected void onViewAttached() {
        percentage.textProperty().bindBidirectional(model.getPercentageAsString());
        marketPrice.descriptionProperty().bind(model.getMarketPriceDescription());
        marketPrice.textProperty().bind(model.getMarketPriceAsString());

        percentageFocussedPin = EasyBind.subscribe(percentage.inputTextFieldFocusedProperty(), controller::onPercentageFocussed);

        //  priceTypeSwitch.setOnAction(e->controller.onChange);
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
        marketPrice.descriptionProperty().unbind();
        marketPrice.textProperty().unbind();

        percentageFocussedPin.unsubscribe();

        Parent node = root;
        while (node.getParent() != null) {
            node.setOnMousePressed(null);
            node = node.getParent();
        }
    }
}
