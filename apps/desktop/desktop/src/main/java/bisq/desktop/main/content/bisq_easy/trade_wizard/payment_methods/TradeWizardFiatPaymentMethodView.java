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
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeWizardFiatPaymentMethodView extends View<VBox, TradeWizardFiatPaymentMethodModel, TradeWizardFiatPaymentMethodController> {
    private final MaterialTextField custom;
    private final ListChangeListener<FiatPaymentMethod> fiatPaymentMethodListener;
    private final ListChangeListener<BitcoinPaymentMethod> bitcoinPaymentMethodListener;
    private final FlowPane fiatMethodsFlowPane, bitcoinMethodsFlowPane;
    private final Label headlineLabel, nonFoundLabel;
    private final BisqIconButton addButton;
    private Subscription addCustomMethodIconEnabledPin;

    public TradeWizardFiatPaymentMethodView(TradeWizardFiatPaymentMethodModel model, TradeWizardFiatPaymentMethodController controller) {
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

        bitcoinMethodsFlowPane = new FlowPane();
        bitcoinMethodsFlowPane.setAlignment(Pos.CENTER);
        bitcoinMethodsFlowPane.setVgap(20);
        bitcoinMethodsFlowPane.setHgap(20);

        VBox.setMargin(headlineLabel, new Insets(-10, 0, 0, 0));
        VBox.setMargin(fiatMethodsFlowPane, new Insets(20, 60, 25, 60));
        VBox.setMargin(bitcoinMethodsFlowPane, new Insets(10, 60, 10, 60));
        root.getChildren().addAll(Spacer.fillVBox(), headlineLabel, subtitleLabel, nonFoundLabel, fiatMethodsFlowPane,
                custom, bitcoinMethodsFlowPane, Spacer.fillVBox());

        fiatPaymentMethodListener = c -> {
            c.next();
            fillFiatPaymentMethods();
        };

        bitcoinPaymentMethodListener = c -> {
            c.next();
            fillBitcoinPaymentMethods();
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

        addCustomMethodIconEnabledPin = EasyBind.subscribe(model.getIsAddCustomMethodIconEnabled(), enabled -> {
            custom.setIcon(enabled ? "add" : "add-white");
            addButton.setOpacity(enabled ? 1 : 0.15);
        });

        model.getFiatPaymentMethods().addListener(fiatPaymentMethodListener);
        model.getBitcoinPaymentMethods().addListener(bitcoinPaymentMethodListener);

        addButton.setOnAction(e -> controller.onAddCustomMethod());
        root.setOnMousePressed(e -> root.requestFocus());

        fillFiatPaymentMethods();
        fillBitcoinPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        custom.textProperty().unbindBidirectional(model.getCustomFiatPaymentMethodName());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        fiatMethodsFlowPane.visibleProperty().unbind();
        fiatMethodsFlowPane.managedProperty().unbind();
        addButton.disableProperty().unbind();

        fiatMethodsFlowPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        bitcoinMethodsFlowPane.getChildren().stream()
                .filter(e -> e instanceof ChipButton)
                .map(e -> (ChipButton) e)
                .forEach(chipToggleButton -> chipToggleButton.setOnAction(null));

        addCustomMethodIconEnabledPin.unsubscribe();

        model.getFiatPaymentMethods().removeListener(fiatPaymentMethodListener);
        model.getBitcoinPaymentMethods().removeListener(bitcoinPaymentMethodListener);

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
                            },
                            () -> {
                                // Lookup for an image with the id of the enum name (REVOLUT)
                                ImageView icon = ImageUtil.getImageViewById(fiatPaymentMethod.getName());
                                chipButton.setLeftIcon(icon);
                            });
            fiatMethodsFlowPane.getChildren().add(chipButton);
        }
    }

    private void fillBitcoinPaymentMethods() {
        bitcoinMethodsFlowPane.getChildren().clear();
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
                                // Lookup for an image with the id of the BitcoinPaymentRail enum name (MAIN_CHAIN)
                                ImageView icon = ImageUtil.getImageViewById(bitcoinPaymentMethod.getName());
                                chipButton.setLeftIcon(icon);
                            });
            bitcoinMethodsFlowPane.getChildren().add(chipButton);
        }
    }
}
