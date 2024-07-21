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

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.ChipButton;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeWizardPaymentMethodsView extends View<VBox, TradeWizardPaymentMethodsModel, TradeWizardPaymentMethodsController> {
    private final MaterialTextField custom;
    private final ListChangeListener<FiatPaymentMethod> fiatPaymentMethodListener;
    private final FlowPane fiatMethodsFlowPane;
    private final Label headlineLabel, nonFoundLabel;
    private final BisqIconButton addButton;
    private final CheckBox allowLNMethodSwitch;
    private Subscription addCustomMethodIconEnabledPin;

    public TradeWizardPaymentMethodsView(TradeWizardPaymentMethodsModel model, TradeWizardPaymentMethodsController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label();
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("bisqEasy.tradeWizard.paymentMethod.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().add("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(600);

        nonFoundLabel = new Label(Res.get("bisqEasy.tradeWizard.paymentMethod.noneFound"));
        nonFoundLabel.getStyleClass().add("bisq-text-6");
        nonFoundLabel.setAlignment(Pos.CENTER);

        fiatMethodsFlowPane = new FlowPane();
        fiatMethodsFlowPane.setAlignment(Pos.CENTER);
        fiatMethodsFlowPane.setVgap(20);
        fiatMethodsFlowPane.setHgap(20);

        custom = new MaterialTextField(Res.get("bisqEasy.tradeWizard.paymentMethod.customMethod"),
                Res.get("bisqEasy.tradeWizard.paymentMethod.customMethod.prompt"));
        custom.setPrefWidth(300);
        custom.setIcon("add-white");
        addButton = custom.getIconButton();
        addButton.setOpacity(0.15);
        addButton.setDisable(true);
        addButton.setAlignment(Pos.CENTER);
        custom.setMaxWidth(300);

        Label allowLNNetworkMethodLabel = new Label(Res.get("bisqEasy.tradeWizard.paymentMethods.allowLN"), ImageUtil.getImageViewById("LN"));
        allowLNNetworkMethodLabel.setContentDisplay(ContentDisplay.RIGHT);
        allowLNNetworkMethodLabel.setGraphicTextGap(12);
        allowLNMethodSwitch = new CheckBox();
        HBox allowLNHBox = new HBox(10, allowLNMethodSwitch, allowLNNetworkMethodLabel);
        allowLNHBox.getStyleClass().add("offerbook-subheader-checkbox");
        allowLNHBox.setAlignment(Pos.CENTER);

        VBox.setMargin(headlineLabel, new Insets(-10, 0, 0, 0));
        VBox.setMargin(fiatMethodsFlowPane, new Insets(20, 60, 10, 60));
        VBox.setMargin(allowLNHBox, new Insets(20, 0, 0, 0));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, subtitleLabel, nonFoundLabel, fiatMethodsFlowPane,
                custom, allowLNHBox, Spacer.fillVBox());

        fiatPaymentMethodListener = c -> {
            c.next();
            fillFiatPaymentMethods();
        };
    }

    @Override
    protected void onViewAttached() {
        headlineLabel.setText(model.getHeadline());
        custom.textProperty().bindBidirectional(model.getCustomFiatPaymentMethodName());
        nonFoundLabel.visibleProperty().bind(model.getIsPaymentMethodsEmpty());
        nonFoundLabel.managedProperty().bind(model.getIsPaymentMethodsEmpty());
        fiatMethodsFlowPane.visibleProperty().bind(model.getIsPaymentMethodsEmpty().not());
        fiatMethodsFlowPane.managedProperty().bind(model.getIsPaymentMethodsEmpty().not());
        addButton.disableProperty().bind(model.getIsAddCustomMethodIconEnabled().not());
        allowLNMethodSwitch.selectedProperty().bindBidirectional(model.getIsLNMethodAllowed());

        addCustomMethodIconEnabledPin = EasyBind.subscribe(model.getIsAddCustomMethodIconEnabled(), enabled -> {
            custom.setIcon(enabled ? "add" : "add-white");
            addButton.setOpacity(enabled ? 1 : 0.15);
        });

        model.getFiatPaymentMethods().addListener(fiatPaymentMethodListener);

        addButton.setOnAction(e -> controller.onAddCustomFiatMethod());
        root.setOnMousePressed(e -> root.requestFocus());

        fillFiatPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        custom.textProperty().unbindBidirectional(model.getCustomFiatPaymentMethodName());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        fiatMethodsFlowPane.visibleProperty().unbind();
        fiatMethodsFlowPane.managedProperty().unbind();
        addButton.disableProperty().unbind();
        allowLNMethodSwitch.selectedProperty().unbindBidirectional(model.getIsLNMethodAllowed());

        fiatMethodsFlowPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        addCustomMethodIconEnabledPin.unsubscribe();

        model.getFiatPaymentMethods().removeListener(fiatPaymentMethodListener);

        addButton.setOnAction(null);
        root.setOnMousePressed(null);
    }

    private void fillFiatPaymentMethods() {
        fiatMethodsFlowPane.getChildren().clear();
        for (FiatPaymentMethod fiatPaymentMethod : model.getSortedFiatPaymentMethods()) {
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
            fiatMethodsFlowPane.getChildren().add(chipButton);
        }
    }
}
