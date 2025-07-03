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
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.GridPaneUtil;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ChipButton;
import bisq.desktop.main.content.bisq_easy.BisqEasyViewUtils;
import bisq.desktop.main.content.mu_sig.create_offer.MuSigCreateOfferView;
import bisq.i18n.Res;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MuSigCreateOfferPaymentMethodsView extends View<StackPane, MuSigCreateOfferPaymentMethodsModel, MuSigCreateOfferPaymentMethodsController> {
    private final static int FEEDBACK_WIDTH = 700;
    private static final double TWO_COLUMN_WIDTH = 20.75;

    private final ListChangeListener<FiatPaymentMethod> paymentMethodListener;
    private final Label subtitleLabel, nonFoundLabel;
    private final MuSigCreateOfferAddCustomPaymentMethodBox muSigCreateOfferAddCustomPaymentMethodBox;
    private final GridPane gridPane;
    private final Set<ImageView> closeIcons = new HashSet<>();
    private final VBox overlay;
    private final Button closeOverlayButton, createAccountButton;
    private final Label overlayHeadlineLabel;
    private final VBox content;
    private Subscription hasNoAccountForPaymentMethodPin;

    public MuSigCreateOfferPaymentMethodsView(MuSigCreateOfferPaymentMethodsModel model,
                                              MuSigCreateOfferPaymentMethodsController controller) {
        super(new StackPane(), model, controller);

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

         content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        VBox.setMargin(vBox, new Insets(0, 60, 0, 60));
        VBox.setVgrow(headlineLabel, Priority.ALWAYS);
        VBox.setVgrow(vBox, Priority.ALWAYS);
        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, vBox, Spacer.fillVBox());

        // Overlay
        overlayHeadlineLabel = new Label();
        closeOverlayButton = new Button(Res.get("muSig.offerbook.createOffer.paymentMethod.closeOverlay"));
        createAccountButton = new Button(Res.get("muSig.offerbook.createOffer.paymentMethod.createAccount"));
        overlay = new VBox(20);
        configOverlay();

        StackPane.setMargin(content, new Insets(40));
        StackPane.setMargin(overlay, new Insets(-MuSigCreateOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, overlay);

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

        hasNoAccountForPaymentMethodPin = EasyBind.subscribe(model.getNoAccountForPaymentMethod(),
                paymentMethod -> {
                    if (paymentMethod != null) {
                        overlay.setVisible(true);
                        overlayHeadlineLabel.setText(Res.get("muSig.offerbook.createOffer.paymentMethod.headline", paymentMethod.getShortDisplayString()));
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(overlay, 450);
                    } else {
                        overlay.setVisible(false);
                        Transitions.removeEffect(content);
                    }
                });

        model.getPaymentMethods().addListener(paymentMethodListener);

        createAccountButton.setOnAction(e -> controller.onCreateAccount());
        closeOverlayButton.setOnAction(e -> controller.onCloseOverlay());

        muSigCreateOfferAddCustomPaymentMethodBox.getAddIconButton().setOnAction(e -> controller.onAddCustomPaymentMethod());
        root.setOnMousePressed(e -> root.requestFocus());

        muSigCreateOfferAddCustomPaymentMethodBox.initialize();
        setUpAndFillPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        muSigCreateOfferAddCustomPaymentMethodBox.getCustomPaymentMethodField().textProperty().unbindBidirectional(model.getCustomPaymentMethodName());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        gridPane.visibleProperty().unbind();
        gridPane.managedProperty().unbind();

        hasNoAccountForPaymentMethodPin.unsubscribe();

        gridPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        model.getPaymentMethods().removeListener(paymentMethodListener);

        createAccountButton.setOnAction(null);
        closeOverlayButton.setOnAction(null);
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

    private void configOverlay() {
        VBox overlayContent = new VBox(20);
        overlayContent.setAlignment(Pos.TOP_CENTER);
        overlayContent.getStyleClass().setAll("trade-wizard-feedback-bg");
        overlayContent.setPadding(new Insets(30));
        overlayContent.setMaxWidth(FEEDBACK_WIDTH);

        overlay.setVisible(false);
        overlay.setAlignment(Pos.TOP_CENTER);

        overlayHeadlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("muSig.offerbook.createOffer.paymentMethod.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMinWidth(FEEDBACK_WIDTH - 200);
        subtitleLabel.setMaxWidth(subtitleLabel.getMinWidth());
        subtitleLabel.setWrapText(true);
        subtitleLabel.getStyleClass().add("bisq-text-21");

        createAccountButton.setDefaultButton(true);
        HBox buttonsBox = new HBox(10, closeOverlayButton, createAccountButton);
        buttonsBox.setAlignment(Pos.CENTER);
        VBox.setMargin(buttonsBox, new Insets(10, 0, 0, 0));
        overlayContent.getChildren().addAll(overlayHeadlineLabel, subtitleLabel, buttonsBox);
        overlay.getChildren().addAll(overlayContent, Spacer.fillVBox());
    }
}
