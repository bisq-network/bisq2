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

package bisq.desktop.primary.overlay.createOffer.method;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
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
public class PaymentMethodView extends View<VBox, PaymentMethodModel, PaymentMethodController> {

    private final MaterialTextField custom;
    private final ListChangeListener<String> allPaymentMethodsListener;
    private final FlowPane flowPane;
    private final Label nonFoundLabel;
    private final BisqIconButton addButton;
    private Subscription addCustomMethodIconEnabledPin;

    public PaymentMethodView(PaymentMethodModel model, PaymentMethodController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.method.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");


        Label subtitleLabel = new Label(Res.get("onboarding.method.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3");
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(450);

        nonFoundLabel = new Label(Res.get("onboarding.method.noneFound"));
        nonFoundLabel.getStyleClass().add("bisq-text-6");
        nonFoundLabel.setAlignment(Pos.CENTER);

        flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER);
        flowPane.setVgap(20);
        flowPane.setHgap(20);

        custom = new MaterialTextField(Res.get("onboarding.method.customMethod"),
                null,
                Res.get("onboarding.method.customMethod.prompt"));
        custom.setPrefWidth(300);
        custom.setIcon("add-white");
        addButton = custom.getIconButton();
        addButton.setOpacity(0.15);
        addButton.setDisable(true);
        addButton.setAlignment(Pos.CENTER);
        custom.setMaxWidth(300);

        VBox.setMargin(headLineLabel, new Insets(44, 0, 2, 0));
        VBox.setMargin(flowPane, new Insets(50, 65, 50, 65));
        VBox.setMargin(nonFoundLabel, new Insets(50, 0, 50, 0));
        root.getChildren().addAll(headLineLabel, subtitleLabel, nonFoundLabel, flowPane, custom);

        allPaymentMethodsListener = c -> {
            c.next();
            fillPaymentMethods();
        };
        root.setOnMousePressed(e -> root.requestFocus());
    }

    @Override
    protected void onViewAttached() {
        custom.textProperty().bindBidirectional(model.getCustomMethod());
        nonFoundLabel.visibleProperty().bind(model.getPaymentMethodsEmpty());
        nonFoundLabel.managedProperty().bind(model.getPaymentMethodsEmpty());
        flowPane.visibleProperty().bind(model.getPaymentMethodsEmpty().not());
        flowPane.managedProperty().bind(model.getPaymentMethodsEmpty().not());
        addButton.disableProperty().bind(model.getAddCustomMethodIconEnabled().not());

        addButton.setOnAction(e -> controller.onAddCustomMethod());

        addCustomMethodIconEnabledPin = EasyBind.subscribe(model.getAddCustomMethodIconEnabled(), enabled -> {
            custom.setIcon(enabled ? "add" : "add-white");
            addButton.setOpacity(enabled ? 1 : 0.15);
        });

        model.getAllPaymentMethods().addListener(allPaymentMethodsListener);
        fillPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        custom.textProperty().unbindBidirectional(model.getCustomMethod());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        flowPane.visibleProperty().unbind();
        flowPane.managedProperty().unbind();
        addButton.disableProperty().unbind();

        addButton.setOnAction(null);

        addCustomMethodIconEnabledPin.unsubscribe();

        model.getAllPaymentMethods().removeListener(allPaymentMethodsListener);
    }

    private void fillPaymentMethods() {
        flowPane.getChildren().clear();
        for (int i = 0; i < model.getAllPaymentMethods().size(); i++) {
            String paymentMethod = model.getAllPaymentMethods().get(i);
            String displayString = Res.has("paymentMethod." + paymentMethod) ? Res.get("paymentMethod." + paymentMethod) : paymentMethod;
            ChipButton chipButton = new ChipButton(displayString);
            if (model.getSelectedPaymentMethods().contains(paymentMethod)) {
                chipButton.setSelected(true);
            }
            chipButton.setOnAction(() -> controller.onTogglePaymentMethod(paymentMethod, chipButton.isSelected()));
            model.getAddedCustomMethods().stream()
                    .filter(customMethod -> customMethod.equals(paymentMethod))
                    .findAny()
                    .ifPresentOrElse(customMethod -> {
                        ImageView closeIcon = chipButton.setRightIcon("remove-white");
                        closeIcon.setOnMousePressed(e -> controller.onRemoveCustomMethod(paymentMethod));
                        if (paymentMethod.length() > 13) {
                            chipButton.setTooltip(new BisqTooltip(displayString));
                        }
                    }, () -> {
                        ImageView icon = ImageUtil.getImageViewById(paymentMethod);
                        chipButton.setLeftIcon(icon);
                    });
            flowPane.getChildren().add(chipButton);
        }
    }
}
