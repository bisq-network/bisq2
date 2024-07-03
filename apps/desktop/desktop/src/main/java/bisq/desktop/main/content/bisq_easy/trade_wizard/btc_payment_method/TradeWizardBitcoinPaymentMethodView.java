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

package bisq.desktop.main.content.bisq_easy.trade_wizard.btc_payment_method;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ChipButton;
import bisq.i18n.Res;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeWizardBitcoinPaymentMethodView extends View<VBox, TradeWizardBitcoinPaymentMethodModel, TradeWizardBitcoinPaymentMethodController> {
    private final ListChangeListener<BitcoinPaymentMethod> paymentMethodListener;
    private final FlowPane flowPane;
    private final Label headlineLabel;

    public TradeWizardBitcoinPaymentMethodView(TradeWizardBitcoinPaymentMethodModel model, TradeWizardBitcoinPaymentMethodController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.tradeWizard.paymentMethod.btc.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().add("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(600);

        flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER);
        flowPane.setVgap(20);
        flowPane.setHgap(20);

        VBox.setMargin(headlineLabel, new Insets(-10, 0, 0, 0));
        VBox.setMargin(flowPane, new Insets(20, 90, 25, 90));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, subtitleLabel, flowPane, Spacer.fillVBox());

        paymentMethodListener = c -> {
            c.next();
            fillPaymentMethods();
        };
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());

        model.getBitcoinPaymentMethods().addListener(paymentMethodListener);

        root.setOnMousePressed(e -> root.requestFocus());

        fillPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        flowPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        model.getBitcoinPaymentMethods().removeListener(paymentMethodListener);

        root.setOnMousePressed(null);
    }

    private void fillPaymentMethods() {
        flowPane.getChildren().clear();
        for (BitcoinPaymentMethod bitcoinPaymentMethod : model.getSortedBitcoinPaymentMethods()) {
            // enum name or custom name
            ChipButton chipButton = new ChipButton(bitcoinPaymentMethod.getShortDisplayString());
            if (!bitcoinPaymentMethod.getShortDisplayString().equals(bitcoinPaymentMethod.getDisplayString())) {
                chipButton.setTooltip(new BisqTooltip(bitcoinPaymentMethod.getDisplayString()));
            }
            if (model.getSelectedBitcoinPaymentMethods().contains(bitcoinPaymentMethod)) {
                chipButton.setSelected(true);
            }
            chipButton.setOnAction(() -> {
                boolean wasAdded = controller.onTogglePaymentMethod(bitcoinPaymentMethod, chipButton.isSelected());
                if (!wasAdded) {
                    UIThread.runOnNextRenderFrame(() -> chipButton.setSelected(false));
                }
            });
            model.getAddedCustomBitcoinPaymentMethods().stream()
                    .filter(customMethod -> customMethod.equals(bitcoinPaymentMethod))
                    .findAny()
                    .ifPresentOrElse(
                            customMethod -> {
                                ImageView closeIcon = chipButton.setRightIcon("remove-white");
                                closeIcon.setOnMousePressed(e -> controller.onRemoveCustomMethod(bitcoinPaymentMethod));
                            },
                            () -> {
                                // Lookup for an image with the id of the BitcoinPaymentRail enum name (ONCHAIN)
                                ImageView icon = ImageUtil.getImageViewById(bitcoinPaymentMethod.getName());
                                chipButton.setLeftIcon(icon);
                            });
            flowPane.getChildren().add(chipButton);
        }
    }
}
