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

package bisq.desktop.main.content.mu_sig.create_offer.payment_methods;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ChipButton;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.i18n.Res;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigCreateOfferPaymentMethodsView extends View<VBox, MuSigCreateOfferPaymentMethodsModel, MuSigCreateOfferPaymentMethodsController> {
    private static final double TWO_COLUMN_WIDTH = 20.75;

    private final ListChangeListener<FiatPaymentMethod> paymentMethodListener;
    private final Label subtitleLabel, nonFoundLabel;
    private final MuSigCreateOfferAddCustomPaymentMethodBox muSigCreateOfferAddCustomPaymentMethodBox;
    private final GridPane gridPane;
    private final Set<ImageView> closeIcons = new HashSet<>();

    public MuSigCreateOfferPaymentMethodsView(MuSigCreateOfferPaymentMethodsModel model,
                                              MuSigCreateOfferPaymentMethodsController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bisq-easy-trade-wizard-payment-methods-step");

        Label headlineLabel = new Label(Res.get("muSig.createOffer.paymentMethods.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        subtitleLabel = new Label();
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().add("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(600);

        nonFoundLabel = new Label(Res.get("bisqEasy.tradeWizard.paymentMethods.noneFound"));
        nonFoundLabel.getStyleClass().add("bisq-text-6");
        nonFoundLabel.setAlignment(Pos.CENTER);

        gridPane = GridPaneUtil.getGridPane(10, 10, new Insets(0));
        gridPane.getStyleClass().add("fiat-methods-grid-pane");

        muSigCreateOfferAddCustomPaymentMethodBox = new MuSigCreateOfferAddCustomPaymentMethodBox();


        VBox vBox = new VBox(20, subtitleLabel, nonFoundLabel, gridPane);
        vBox.setAlignment(Pos.CENTER);

        VBox.setMargin(headlineLabel, new Insets(0, 0, -5, 0));
        VBox.setMargin(gridPane, new Insets(0, 60, 0, 60));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, Spacer.fillVBox(), vBox, Spacer.fillVBox());

        paymentMethodListener = c -> setUpAndFillPaymentMethods();
    }

    @Override
    protected void onViewAttached() {
        subtitleLabel.setText(model.getSubtitleLabel());
        muSigCreateOfferAddCustomPaymentMethodBox.getCustomPaymentMethodField().textProperty().bindBidirectional(model.getCustomPaymentMethodName());
        nonFoundLabel.visibleProperty().bind(model.getIsPaymentMethodsEmpty());
        nonFoundLabel.managedProperty().bind(model.getIsPaymentMethodsEmpty());
        gridPane.visibleProperty().bind(model.getIsPaymentMethodsEmpty().not());
        gridPane.managedProperty().bind(model.getIsPaymentMethodsEmpty().not());

        model.getPaymentMethods().addListener(paymentMethodListener);

        muSigCreateOfferAddCustomPaymentMethodBox.getAddIconButton().setOnAction(e -> controller.onAddCustomPaymentMethod());
        muSigCreateOfferAddCustomPaymentMethodBox.initialize();
        root.setOnMousePressed(e -> root.requestFocus());

        setUpAndFillPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        muSigCreateOfferAddCustomPaymentMethodBox.getCustomPaymentMethodField().textProperty().unbindBidirectional(model.getCustomPaymentMethodName());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        gridPane.visibleProperty().unbind();
        gridPane.managedProperty().unbind();

        gridPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        model.getPaymentMethods().removeListener(paymentMethodListener);

        muSigCreateOfferAddCustomPaymentMethodBox.getAddIconButton().setOnAction(null);
        muSigCreateOfferAddCustomPaymentMethodBox.dispose();

        closeIcons.forEach(imageView -> imageView.setOnMousePressed(null));
        closeIcons.clear();

        root.setOnMousePressed(null);
    }

    private void setUpAndFillPaymentMethods() {
        closeIcons.forEach(imageView -> imageView.setOnMousePressed(null));
        closeIcons.clear();
        gridPane.getChildren().clear();
        gridPane.getColumnConstraints().clear();

        int paymentMethodsCount = model.getSortedPaymentMethods().size();
        int numColumns = paymentMethodsCount < 10 ? 3 : 4;
        GridPaneUtil.setGridPaneMultiColumnsConstraints(gridPane, numColumns);

        int i = 0;
        int col, row;
        for (; i < paymentMethodsCount; ++i) {
            FiatPaymentMethod paymentMethod = model.getSortedPaymentMethods().get(i);

            // enum name or custom name
            ChipButton chipButton = new ChipButton(paymentMethod.getShortDisplayString());
            if (!paymentMethod.getShortDisplayString().equals(paymentMethod.getDisplayString())) {
                chipButton.setTooltip(new BisqTooltip(paymentMethod.getDisplayString()));
            }
            if (model.getSelectedPaymentMethods().contains(paymentMethod)) {
                chipButton.setSelected(true);
            }
            chipButton.setOnAction(() -> {
                boolean wasAdded = controller.onTogglePaymentMethod(paymentMethod, chipButton.isSelected());
                if (!wasAdded) {
                    UIThread.runOnNextRenderFrame(() -> chipButton.setSelected(false));
                }
            });
            model.getAddedCustomPaymentMethods().stream()
                    .filter(customMethod -> customMethod.equals(paymentMethod))
                    .findAny()
                    .ifPresentOrElse(
                            customMethod -> {
                                ImageView closeIcon = chipButton.setRightIcon("remove-white");
                                closeIcon.setOnMousePressed(e -> controller.onRemoveCustomMethod(paymentMethod));
                                closeIcons.add(closeIcon);
                                StackPane icon = BisqEasyViewUtils.getCustomPaymentMethodIcon(customMethod.getDisplayString());
                                chipButton.setLeftIcon(icon);
                            },
                            () -> {
                                // Lookup for an image with the id of the enum name (REVOLUT)
                                ImageView icon = ImageUtil.getImageViewById(paymentMethod.getName());
                                chipButton.setLeftIcon(icon);
                            });

            col = i % numColumns;
            row = i / numColumns;
            gridPane.add(chipButton, col, row);
        }

        if (model.getCanAddCustomPaymentMethod().get()) {
            col = i % numColumns;
            row = i / numColumns;
            gridPane.add(muSigCreateOfferAddCustomPaymentMethodBox, col, row);
        }
    }
}
