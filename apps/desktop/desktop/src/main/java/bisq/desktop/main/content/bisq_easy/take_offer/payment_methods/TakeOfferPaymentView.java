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
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.ChipToggleButton;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.offer.payment_method.BitcoinPaymentMethodSpec;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TakeOfferPaymentView extends View<VBox, TakeOfferPaymentModel, TakeOfferPaymentController> {
    private static final double MULTIPLE_COLUMN_WIDTH = 21.30;
    private static final double TWO_COLUMN_WIDTH = 20.75;

    private final GridPane fiatGridPane, bitcoinGridPane;
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
        fiatGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        fiatGridPane.setAlignment(Pos.CENTER);

        bitcoinSubtitleLabel = new Label();
        bitcoinSubtitleLabel.setTextAlignment(TextAlignment.CENTER);
        bitcoinSubtitleLabel.setAlignment(Pos.CENTER);
        bitcoinSubtitleLabel.getStyleClass().addAll("bisq-text-3");
        bitcoinSubtitleLabel.setWrapText(true);
        bitcoinSubtitleLabel.setMaxWidth(600);
        bitcoinToggleGroup = new ToggleGroup();
        bitcoinGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        bitcoinGridPane.setAlignment(Pos.CENTER);

        fiatVBox = new VBox(25, fiatSubtitleLabel, fiatGridPane);
        fiatVBox.setAlignment(Pos.CENTER);
        btcVBox = new VBox(25, bitcoinSubtitleLabel, bitcoinGridPane);
        btcVBox.setAlignment(Pos.CENTER);

        VBox.setMargin(headlineLabel, new Insets(0, 0, 40, 0));
        VBox.setMargin(fiatGridPane, new Insets(0, 0, 45, 0));
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
            fiatGridPane.getChildren().clear();
            fiatGridPane.getColumnConstraints().clear();
            int numColumns = model.getSortedFiatPaymentMethodSpecs().size();
            GridPaneUtil.setGridPaneMultiColumnsConstraints(fiatGridPane, numColumns, numColumns == 2 ? TWO_COLUMN_WIDTH : MULTIPLE_COLUMN_WIDTH);
            int col = 0;
            int row = 0;
            for (FiatPaymentMethodSpec spec : model.getSortedFiatPaymentMethodSpecs()) {
                FiatPaymentMethod paymentMethod = spec.getPaymentMethod();
                ChipToggleButton chipToggleButton = new ChipToggleButton(paymentMethod.getShortDisplayString(), fiatToggleGroup);
                Node icon = !paymentMethod.isCustomPaymentMethod()
                        ? ImageUtil.getImageViewById(paymentMethod.getName())
                        : BisqEasyViewUtils.getCustomPaymentMethodIcon(paymentMethod.getDisplayString());
                chipToggleButton.setLeftIcon(icon);
                chipToggleButton.setOnAction(() -> controller.onToggleFiatPaymentMethod(spec, chipToggleButton.isSelected()));
                chipToggleButton.setSelected(spec.equals(model.getSelectedFiatPaymentMethodSpec().get()));
                fiatGridPane.add(chipToggleButton, col++, row);
            }
        }

        btcVBox.setVisible(model.isBitcoinMethodVisible());
        btcVBox.setManaged(model.isBitcoinMethodVisible());
        if (model.isBitcoinMethodVisible()) {
            bitcoinSubtitleLabel.setText(model.getBitcoinSubtitle());
            bitcoinGridPane.getChildren().clear();
            bitcoinGridPane.getColumnConstraints().clear();
            int numColumns = model.getSortedBitcoinPaymentMethodSpecs().size();
            GridPaneUtil.setGridPaneMultiColumnsConstraints(bitcoinGridPane, numColumns, numColumns == 2 ? TWO_COLUMN_WIDTH : MULTIPLE_COLUMN_WIDTH);
            int col = 0;
            int row = 0;
            for (BitcoinPaymentMethodSpec spec : model.getSortedBitcoinPaymentMethodSpecs()) {
                BitcoinPaymentMethod paymentMethod = spec.getPaymentMethod();
                ChipToggleButton chipToggleButton = new ChipToggleButton(paymentMethod.getShortDisplayString(), bitcoinToggleGroup);
                if (!paymentMethod.isCustomPaymentMethod()) {
                    ImageView icon = ImageUtil.getImageViewById(paymentMethod.getName());
                    ColorAdjust colorAdjust = new ColorAdjust();
                    colorAdjust.setBrightness(-0.2);
                    icon.setEffect(colorAdjust);
                    chipToggleButton.setLeftIcon(icon);
                }
                chipToggleButton.setOnAction(() -> controller.onToggleBitcoinPaymentMethod(spec, chipToggleButton.isSelected()));
                chipToggleButton.setSelected(spec.equals(model.getSelectedBitcoinPaymentMethodSpec().get()));
                bitcoinGridPane.add(chipToggleButton, col++, row);
            }
        }
    }

    @Override
    protected void onViewDetached() {
        fiatGridPane.getChildren().clear();
        fiatGridPane.getChildren().stream()
                .filter(e -> e instanceof ChipToggleButton)
                .map(e -> (ChipToggleButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));
        bitcoinGridPane.getChildren().clear();
        bitcoinGridPane.getChildren().stream()
                .filter(e -> e instanceof ChipToggleButton)
                .map(e -> (ChipToggleButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));
    }
}
