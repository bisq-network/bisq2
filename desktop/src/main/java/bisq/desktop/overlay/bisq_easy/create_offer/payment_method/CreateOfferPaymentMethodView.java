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

package bisq.desktop.overlay.bisq_easy.create_offer.payment_method;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.common.Transitions;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ChipButton;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.overlay.bisq_easy.create_offer.CreateOfferView;
import bisq.i18n.Res;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class CreateOfferPaymentMethodView extends View<StackPane, CreateOfferPaymentMethodModel, CreateOfferPaymentMethodController> {

    private final MaterialTextField custom;
    private final ListChangeListener<FiatPaymentMethod> paymentMethodListener;
    private final FlowPane flowPane;
    private final Label nonFoundLabel;
    private final BisqIconButton addButton;
    private final VBox content;
    private final VBox overlay;
    private Subscription addCustomMethodIconEnabledPin;
    private Subscription showCustomMethodNotEmptyWarning;
    private Button closeOverlayButton;

    public CreateOfferPaymentMethodView(CreateOfferPaymentMethodModel model, CreateOfferPaymentMethodController controller) {
        super(new StackPane(), model, controller);
        // super(new VBox(10), model, controller);

        root.setAlignment(Pos.CENTER);

        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        root.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("bisqEasy.createOffer.paymentMethod.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.createOffer.paymentMethod.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(450);

        nonFoundLabel = new Label(Res.get("bisqEasy.createOffer.paymentMethod.noneFound"));
        nonFoundLabel.getStyleClass().add("bisq-text-6");
        nonFoundLabel.setAlignment(Pos.CENTER);

        flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER);
        flowPane.setVgap(20);
        flowPane.setHgap(20);

        custom = new MaterialTextField(Res.get("bisqEasy.createOffer.paymentMethod.customMethod"),
                null,
                Res.get("bisqEasy.createOffer.paymentMethod.customMethod.prompt"));
        custom.setPrefWidth(300);
        custom.setIcon("add-white");
        addButton = custom.getIconButton();
        addButton.setOpacity(0.15);
        addButton.setDisable(true);
        addButton.setAlignment(Pos.CENTER);
        custom.setMaxWidth(300);

        VBox.setMargin(headLineLabel, new Insets(-30, 0, 0, 0));
        VBox.setMargin(flowPane, new Insets(10, 65, 30, 65));
        content.getChildren().addAll(Spacer.fillVBox(), headLineLabel, subtitleLabel, nonFoundLabel, flowPane, custom, Spacer.fillVBox());

        overlay = new VBox(20);
        setupOverlay();

        StackPane.setMargin(overlay, new Insets(-CreateOfferView.TOP_PANE_HEIGHT, 0, 0, 0));
        root.getChildren().addAll(content, overlay);

        paymentMethodListener = c -> {
            c.next();
            fillPaymentMethods();
        };
        root.setOnMousePressed(e -> root.requestFocus());
    }

    @Override
    protected void onViewAttached() {
        custom.textProperty().bindBidirectional(model.getCustomFiatPaymentMethodName());
        nonFoundLabel.visibleProperty().bind(model.getIsPaymentMethodsEmpty());
        nonFoundLabel.managedProperty().bind(model.getIsPaymentMethodsEmpty());
        flowPane.visibleProperty().bind(model.getIsPaymentMethodsEmpty().not());
        flowPane.managedProperty().bind(model.getIsPaymentMethodsEmpty().not());
        addButton.disableProperty().bind(model.getIsAddCustomMethodIconEnabled().not());

        addButton.setOnAction(e -> controller.onAddCustomMethod());
        closeOverlayButton.setOnAction(e -> controller.onCloseOverlay());
        addCustomMethodIconEnabledPin = EasyBind.subscribe(model.getIsAddCustomMethodIconEnabled(), enabled -> {
            custom.setIcon(enabled ? "add" : "add-white");
            addButton.setOpacity(enabled ? 1 : 0.15);
        });

        showCustomMethodNotEmptyWarning = EasyBind.subscribe(model.getShowCustomMethodNotEmptyWarning(),
                showReputationInfo -> {
                    if (showReputationInfo) {
                        overlay.setVisible(true);
                        overlay.setOpacity(1);
                        Transitions.blurStrong(content, 0);
                        Transitions.slideInTop(overlay, 450);
                    } else {
                        Transitions.removeEffect(content);
                        if (overlay.isVisible()) {
                            Transitions.fadeOut(overlay, Transitions.DEFAULT_DURATION / 2,
                                    () -> overlay.setVisible(false));
                        }
                    }
                });

        model.getFiatPaymentMethods().addListener(paymentMethodListener);
        fillPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        custom.textProperty().unbindBidirectional(model.getCustomFiatPaymentMethodName());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        flowPane.visibleProperty().unbind();
        flowPane.managedProperty().unbind();
        addButton.disableProperty().unbind();

        addButton.setOnAction(null);

        flowPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        addCustomMethodIconEnabledPin.unsubscribe();
        showCustomMethodNotEmptyWarning.unsubscribe();

        model.getFiatPaymentMethods().removeListener(paymentMethodListener);
    }

    private void fillPaymentMethods() {
        flowPane.getChildren().clear();
        for (FiatPaymentMethod fiatPaymentMethod : model.getSortedFiatPaymentMethods()) {
            // enum name or custom name
            ChipButton chipButton = new ChipButton(fiatPaymentMethod.getShortDisplayString());
            if (model.getSelectedFiatPaymentMethods().contains(fiatPaymentMethod)) {
                chipButton.setSelected(true);
            }
            chipButton.setOnAction(() -> {
                boolean wasAdded = controller.onTogglePaymentMethod(fiatPaymentMethod, chipButton.isSelected());
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
                                closeIcon.setOnMousePressed(e -> controller.onRemoveCustomMethod(fiatPaymentMethod));
                                if (fiatPaymentMethod.getShortDisplayString().length() > 13) {
                                    chipButton.setTooltip(new BisqTooltip(fiatPaymentMethod.getDisplayString()));
                                }
                            },
                            () -> {
                                // Lookup for an image with the id of the enum name (REVOLUT)
                                ImageView icon = ImageUtil.getImageViewById(fiatPaymentMethod.getName());
                                chipButton.setLeftIcon(icon);
                            });
            flowPane.getChildren().add(chipButton);
        }
    }

    private void setupOverlay() {
        double width = 700;
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);
        contentBox.getStyleClass().setAll("create-offer-feedback-bg");
        contentBox.setPadding(new Insets(30));
        contentBox.setMaxWidth(width);

        // We don't use setManaged as the transition would not work as expected if set to false
        overlay.setVisible(false);
        overlay.setAlignment(Pos.TOP_CENTER);
        Label headLineLabel = new Label(Res.get("popup.headline.warning"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");
        headLineLabel.setTextAlignment(TextAlignment.CENTER);
        headLineLabel.setAlignment(Pos.CENTER);
        headLineLabel.setMaxWidth(width - 60);

        Label subtitleLabel = new Label(Res.get("bisqEasy.createOffer.paymentMethod.warn.customMethodNotEmpty"));
        subtitleLabel.setMaxWidth(width - 60);
        subtitleLabel.getStyleClass().addAll("bisq-text-21", "wrap-text");
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);

        closeOverlayButton = new Button(Res.get("action.close"));
        closeOverlayButton.setDefaultButton(true);
        VBox.setMargin(closeOverlayButton, new Insets(30, 0, 0, 0));
        contentBox.getChildren().addAll(headLineLabel, subtitleLabel, closeOverlayButton);
        overlay.getChildren().addAll(contentBox, Spacer.fillVBox());
    }
}
