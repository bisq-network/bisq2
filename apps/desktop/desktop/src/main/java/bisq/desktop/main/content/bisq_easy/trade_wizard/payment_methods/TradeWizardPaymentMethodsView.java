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

package bisq.desktop.main.content.bisq_easy.trade_wizard.payment_methods;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TradeWizardPaymentMethodsView extends View<VBox, TradeWizardPaymentMethodsModel, TradeWizardPaymentMethodsController> {
    private final ListChangeListener<FiatPaymentMethod> fiatPaymentMethodListener;
    private final Label fiatSubtitleLabel, bitcoinSubtitleLabel, nonFoundLabel;
    private final AddCustomPaymentMethodBox addCustomPaymentMethodBox;
    private final GridPane fiatMethodsGridPane, bitcoinMethodsGridPane;

    public TradeWizardPaymentMethodsView(TradeWizardPaymentMethodsModel model, TradeWizardPaymentMethodsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-easy-trade-wizard-payment-methods-step");

        Label headlineLabel = new Label(Res.get("bisqEasy.tradeWizard.paymentMethods.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        fiatSubtitleLabel = new Label();
        fiatSubtitleLabel.setTextAlignment(TextAlignment.CENTER);
        fiatSubtitleLabel.setAlignment(Pos.CENTER);
        fiatSubtitleLabel.getStyleClass().add("bisq-text-3");
        fiatSubtitleLabel.setWrapText(true);
        fiatSubtitleLabel.setMaxWidth(600);

        nonFoundLabel = new Label(Res.get("bisqEasy.tradeWizard.paymentMethods.noneFound"));
        nonFoundLabel.getStyleClass().add("bisq-text-6");
        nonFoundLabel.setAlignment(Pos.CENTER);

        fiatMethodsGridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        fiatMethodsGridPane.getStyleClass().add("fiat-methods-grid-pane");

        addCustomPaymentMethodBox = new AddCustomPaymentMethodBox();

        bitcoinSubtitleLabel = new Label();
        bitcoinSubtitleLabel.setTextAlignment(TextAlignment.CENTER);
        bitcoinSubtitleLabel.setAlignment(Pos.CENTER);
        bitcoinSubtitleLabel.getStyleClass().add("bisq-text-3");
        bitcoinSubtitleLabel.setWrapText(true);
        bitcoinSubtitleLabel.setMaxWidth(600);

        bitcoinMethodsGridPane = GridPaneUtil.getTwoColumnsGridPane(10, 10, new Insets(0));
        bitcoinMethodsGridPane.getStyleClass().add("bitcoin-methods-grid-pane");
        bitcoinMethodsGridPane.setAlignment(Pos.CENTER);

        VBox fiatVBox = new VBox(20, fiatSubtitleLabel, nonFoundLabel, fiatMethodsGridPane);
        fiatVBox.setAlignment(Pos.CENTER);
        VBox btcVBox = new VBox(20, bitcoinSubtitleLabel, bitcoinMethodsGridPane);
        btcVBox.setAlignment(Pos.CENTER);

        VBox.setMargin(headlineLabel, new Insets(0, 0, -5, 0));
        VBox.setMargin(fiatMethodsGridPane, new Insets(0, 60, 0, 60));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, Spacer.fillVBox(), fiatVBox, Spacer.fillVBox(),
                btcVBox, Spacer.fillVBox());

        fiatPaymentMethodListener = c -> {
            c.next();
            setUpAndFillFiatPaymentMethods();
        };
    }

    @Override
    protected void onViewAttached() {
        fiatSubtitleLabel.setText(model.getFiatSubtitleLabel());
        bitcoinSubtitleLabel.setText(model.getBitcoinSubtitleLabel());
        addCustomPaymentMethodBox.getCustomPaymentMethodField().textProperty().bindBidirectional(model.getCustomFiatPaymentMethodName());
        nonFoundLabel.visibleProperty().bind(model.getIsPaymentMethodsEmpty());
        nonFoundLabel.managedProperty().bind(model.getIsPaymentMethodsEmpty());
        fiatMethodsGridPane.visibleProperty().bind(model.getIsPaymentMethodsEmpty().not());
        fiatMethodsGridPane.managedProperty().bind(model.getIsPaymentMethodsEmpty().not());

        model.getFiatPaymentMethods().addListener(fiatPaymentMethodListener);

        addCustomPaymentMethodBox.getAddIconButton().setOnAction(e -> controller.onAddCustomFiatMethod());
        addCustomPaymentMethodBox.initialize();
        root.setOnMousePressed(e -> root.requestFocus());

        setUpAndFillFiatPaymentMethods();
        setUpAndFillBitcoinPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        addCustomPaymentMethodBox.getCustomPaymentMethodField().textProperty().unbindBidirectional(model.getCustomFiatPaymentMethodName());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        fiatMethodsGridPane.visibleProperty().unbind();
        fiatMethodsGridPane.managedProperty().unbind();

        fiatMethodsGridPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));
        bitcoinMethodsGridPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        model.getFiatPaymentMethods().removeListener(fiatPaymentMethodListener);

        addCustomPaymentMethodBox.getAddIconButton().setOnAction(null);
        addCustomPaymentMethodBox.dispose();
        root.setOnMousePressed(null);
    }

    private void setUpAndFillFiatPaymentMethods() {
        fiatMethodsGridPane.getChildren().clear();
        fiatMethodsGridPane.getColumnConstraints().clear();
        int paymentMethodsCount = model.getSortedFiatPaymentMethods().size();
        int numColumns = paymentMethodsCount < 10 ? 3 : 4;
        GridPaneUtil.setGridPaneMultiColumnsConstraints(fiatMethodsGridPane, numColumns);

        int i = 0;
        int col, row;
        for (; i < paymentMethodsCount; ++i) {
            FiatPaymentMethod fiatPaymentMethod = model.getSortedFiatPaymentMethods().get(i);

            // enum name or custom name
            ChipButton chipButton = new ChipButton(fiatPaymentMethod.getShortDisplayString());
            if (!fiatPaymentMethod.getShortDisplayString().equals(fiatPaymentMethod.getDisplayString())) {
                chipButton.setTooltip(new BisqTooltip(fiatPaymentMethod.getDisplayString()));
            }
            if (model.getSelectedFiatPaymentMethods().contains(fiatPaymentMethod)) {
                chipButton.setSelected(true);
            }
            chipButton.setOnAction(() -> {
                boolean wasAdded = controller.onToggleFiatPaymentMethod(fiatPaymentMethod, chipButton.isSelected());
                if (!wasAdded) {
                    UIThread.runOnNextRenderFrame(() -> chipButton.setSelected(false));
                }
            });
            model.getAddedCustomFiatPaymentMethods().stream()
                    .filter(customMethod -> customMethod.equals(fiatPaymentMethod))
                    .findAny()
                    .ifPresentOrElse(
                            customMethod -> {
                                ImageView closeIcon = chipButton.setRightIcon("remove-white");
                                closeIcon.setOnMousePressed(e -> controller.onRemoveFiatCustomMethod(fiatPaymentMethod));
                            },
                            () -> {
                                // Lookup for an image with the id of the enum name (REVOLUT)
                                ImageView icon = ImageUtil.getImageViewById(fiatPaymentMethod.getName());
                                chipButton.setLeftIcon(icon);
                            });

            col = i % numColumns;
            row = i / numColumns;
            fiatMethodsGridPane.add(chipButton, col, row);
        }

        if (model.getCanAddCustomFiatPaymentMethod().get()) {
            col = i % numColumns;
            row = i / numColumns;
            fiatMethodsGridPane.add(addCustomPaymentMethodBox, col, row);
        }
    }

    private void setUpAndFillBitcoinPaymentMethods() {
        bitcoinMethodsGridPane.getChildren().clear();
        bitcoinMethodsGridPane.getColumnConstraints().clear();

        checkArgument(model.getSortedBitcoinPaymentMethods().size() == 2, "Only 2 Btc settlement methods allowed for now.");
        for (int i = 0; i < model.getSortedBitcoinPaymentMethods().size(); ++i) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(20.75d);
            bitcoinMethodsGridPane.getColumnConstraints().add(col);
        }

        int row = 0;
        int col = 0;
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
                boolean wasAdded = controller.onToggleBitcoinPaymentMethod(bitcoinPaymentMethod, chipButton.isSelected());
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
                                closeIcon.setOnMousePressed(e -> controller.onRemoveCustomBitcoinMethod(bitcoinPaymentMethod));
                            },
                            () -> {
                                // Lookup for an image with the id of the enum name (REVOLUT)
                                ImageView icon = ImageUtil.getImageViewById(bitcoinPaymentMethod.getName());
                                chipButton.setLeftIcon(icon);
                            });
            bitcoinMethodsGridPane.add(chipButton, col++, row);
        }
    }
}
