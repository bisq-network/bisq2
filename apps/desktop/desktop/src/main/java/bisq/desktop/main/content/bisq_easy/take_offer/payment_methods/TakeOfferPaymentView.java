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

package bisq.desktop.main.content.bisq_easy.take_offer.payment_methods;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.ChipToggleButton;
import bisq.i18n.Res;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
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
    private final FlowPane fiatFlowPane, bitcoinFlowPane;
    private final ToggleGroup bitcoinToggleGroup, fiatToggleGroup;
    private final Label headlineLabel, fiatSubtitleLabel, bitcoinSubtitleLabel;
    private final VBox fiatVBox, btcVBox;

    public TakeOfferPaymentView(TakeOfferPaymentModel model, TakeOfferPaymentController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        fiatSubtitleLabel = new Label();
        fiatSubtitleLabel.setTextAlignment(TextAlignment.CENTER);
        fiatSubtitleLabel.setAlignment(Pos.CENTER);
        fiatSubtitleLabel.getStyleClass().addAll("bisq-text-3");
        fiatSubtitleLabel.setWrapText(true);
        fiatSubtitleLabel.setMaxWidth(600);

        fiatToggleGroup = new ToggleGroup();
        bitcoinToggleGroup = new ToggleGroup();

        fiatFlowPane = new FlowPane();
        fiatFlowPane.setAlignment(Pos.CENTER);
        fiatFlowPane.setVgap(20);
        fiatFlowPane.setHgap(20);

        bitcoinSubtitleLabel = new Label();
        bitcoinSubtitleLabel.setTextAlignment(TextAlignment.CENTER);
        bitcoinSubtitleLabel.setAlignment(Pos.CENTER);
        bitcoinSubtitleLabel.getStyleClass().addAll("bisq-text-3");
        bitcoinSubtitleLabel.setWrapText(true);
        bitcoinSubtitleLabel.setMaxWidth(600);

        bitcoinFlowPane = new FlowPane();
        bitcoinFlowPane.setAlignment(Pos.CENTER);
        bitcoinFlowPane.setVgap(20);
        bitcoinFlowPane.setHgap(20);

        fiatVBox = new VBox(25, fiatSubtitleLabel, fiatFlowPane);
        fiatVBox.setAlignment(Pos.CENTER);
        btcVBox = new VBox(25, bitcoinSubtitleLabel, bitcoinFlowPane);
        btcVBox.setAlignment(Pos.CENTER);

        VBox.setMargin(headlineLabel, new Insets(0, 0, 40, 0));
        VBox.setMargin(fiatFlowPane, new Insets(0, 60, 45, 60));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, fiatVBox, btcVBox, Spacer.fillVBox());

        root.setOnMousePressed(e -> root.requestFocus());
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());

        fiatVBox.setVisible(model.isFiatMethodVisible());
        fiatVBox.setManaged(model.isFiatMethodVisible());
        if (model.isFiatMethodVisible()) {
            fiatSubtitleLabel.setText(model.getFiatSubtitle());
            for (FiatPaymentMethodSpec spec : model.getSortedFiatPaymentMethodSpecs()) {
                FiatPaymentMethod paymentMethod = spec.getPaymentMethod();
                ChipToggleButton chipToggleButton = new ChipToggleButton(paymentMethod.getShortDisplayString(), fiatToggleGroup);
                if (!paymentMethod.isCustomPaymentMethod()) {
                    ImageView icon = ImageUtil.getImageViewById(paymentMethod.getName());
                    chipToggleButton.setLeftIcon(icon);
                }
                chipToggleButton.setOnAction(() -> controller.onToggleFiatPaymentMethod(spec, chipToggleButton.isSelected()));
                chipToggleButton.setSelected(spec.equals(model.getSelectedFiatPaymentMethodSpec().get()));
                fiatFlowPane.getChildren().add(chipToggleButton);
            }
        }

        btcVBox.setVisible(model.isBitcoinMethodVisible());
        btcVBox.setManaged(model.isBitcoinMethodVisible());
        if (model.isBitcoinMethodVisible()) {
            bitcoinSubtitleLabel.setText(model.getBitcoinSubtitle());
            for (BitcoinPaymentMethodSpec spec : model.getSortedBitcoinPaymentMethodSpecs()) {
                BitcoinPaymentMethod paymentMethod = spec.getPaymentMethod();
                ChipToggleButton chipToggleButton = new ChipToggleButton(paymentMethod.getShortDisplayString(), bitcoinToggleGroup);
                if (!paymentMethod.isCustomPaymentMethod()) {
                    ImageView icon = ImageUtil.getImageViewById(paymentMethod.getName());
                    chipToggleButton.setLeftIcon(icon);
                }
                chipToggleButton.setOnAction(() -> controller.onToggleBitcoinPaymentMethod(spec, chipToggleButton.isSelected()));
                chipToggleButton.setSelected(spec.equals(model.getSelectedBitcoinPaymentMethodSpec().get()));
                bitcoinFlowPane.getChildren().add(chipToggleButton);
            }
        }
    }

    @Override
    protected void onViewDetached() {
        fiatFlowPane.getChildren().clear();
        fiatFlowPane.getChildren().stream()
                .filter(e -> e instanceof ChipToggleButton)
                .map(e -> (ChipToggleButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));
        bitcoinFlowPane.getChildren().clear();
        bitcoinFlowPane.getChildren().stream()
                .filter(e -> e instanceof ChipToggleButton)
                .map(e -> (ChipToggleButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));
    }
}
