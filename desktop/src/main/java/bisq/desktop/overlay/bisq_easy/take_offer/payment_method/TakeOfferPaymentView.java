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

package bisq.desktop.overlay.bisq_easy.take_offer.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.ChipToggleButton;
import bisq.i18n.Res;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TakeOfferPaymentView extends View<VBox, TakeOfferPaymentModel, TakeOfferPaymentController> {
    private final FlowPane flowPane;
    private final ToggleGroup toggleGroup = new ToggleGroup();

    public TakeOfferPaymentView(TakeOfferPaymentModel model, TakeOfferPaymentController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("bisqEasy.takeOffer.method.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.takeOffer.method.subtitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(450);

        flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER);
        flowPane.setVgap(20);
        flowPane.setHgap(20);

        VBox.setMargin(headLineLabel, new Insets(-30, 0, 0, 0));
        VBox.setMargin(flowPane, new Insets(10, 65, 30, 65));
        root.getChildren().addAll(Spacer.fillVBox(), headLineLabel, subtitleLabel, flowPane, Spacer.fillVBox());

        root.setOnMousePressed(e -> root.requestFocus());
    }

    @Override
    protected void onViewAttached() {
        flowPane.getChildren().clear();
        for (FiatPaymentMethodSpec spec : model.getSortedSpecs()) {
            FiatPaymentMethod paymentMethod = spec.getPaymentMethod();
            ChipToggleButton chipToggleButton = new ChipToggleButton(paymentMethod.getShortDisplayString(), toggleGroup);
            if (!paymentMethod.isCustomPaymentMethod()) {
                ImageView icon = ImageUtil.getImageViewById(paymentMethod.getName());
                chipToggleButton.setLeftIcon(icon);
            }
            chipToggleButton.setOnAction(() -> controller.onTogglePaymentMethod(spec, chipToggleButton.isSelected()));
            chipToggleButton.setSelected(spec.equals(model.getSelectedSpec().get()));
            flowPane.getChildren().add(chipToggleButton);
        }
    }

    @Override
    protected void onViewDetached() {
        flowPane.getChildren().stream()
                .filter(e -> e instanceof ChipToggleButton)
                .map(e -> (ChipToggleButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));
    }
}
