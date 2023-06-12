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

package bisq.desktop.primary.overlay.bisq_easy.create_offer.method;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class CreateOfferPaymentMethodView extends View<VBox, CreateOfferPaymentMethodModel, CreateOfferPaymentMethodController> {

    private final MaterialTextField custom;
    private final ListChangeListener<String> allSettlementMethodsListener;
    private final FlowPane flowPane;
    private final Label nonFoundLabel;
    private final BisqIconButton addButton;
    private Subscription addCustomMethodIconEnabledPin;

    public CreateOfferPaymentMethodView(CreateOfferPaymentMethodModel model, CreateOfferPaymentMethodController controller) {
        super(new VBox(10), model, controller);

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

        VBox.setMargin(headLineLabel, new Insets(-30, 0, 0, 0));
        VBox.setMargin(flowPane, new Insets(10, 65, 30, 65));
        root.getChildren().addAll(Spacer.fillVBox(), headLineLabel, subtitleLabel, nonFoundLabel, flowPane, custom, Spacer.fillVBox());

        allSettlementMethodsListener = c -> {
            c.next();
            fillSettlementMethods();
        };
        root.setOnMousePressed(e -> root.requestFocus());
    }

    @Override
    protected void onViewAttached() {
        custom.textProperty().bindBidirectional(model.getCustomMethodName());
        nonFoundLabel.visibleProperty().bind(model.getIsSettlementMethodsEmpty());
        nonFoundLabel.managedProperty().bind(model.getIsSettlementMethodsEmpty());
        flowPane.visibleProperty().bind(model.getIsSettlementMethodsEmpty().not());
        flowPane.managedProperty().bind(model.getIsSettlementMethodsEmpty().not());
        addButton.disableProperty().bind(model.getIsAddCustomMethodIconEnabled().not());

        addButton.setOnAction(e -> controller.onAddCustomMethod());

        addCustomMethodIconEnabledPin = EasyBind.subscribe(model.getIsAddCustomMethodIconEnabled(), enabled -> {
            custom.setIcon(enabled ? "add" : "add-white");
            addButton.setOpacity(enabled ? 1 : 0.15);
        });

        model.getAllMethodNames().addListener(allSettlementMethodsListener);
        fillSettlementMethods();
    }

    @Override
    protected void onViewDetached() {
        custom.textProperty().unbindBidirectional(model.getCustomMethodName());
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        flowPane.visibleProperty().unbind();
        flowPane.managedProperty().unbind();
        addButton.disableProperty().unbind();

        addButton.setOnAction(null);

        addCustomMethodIconEnabledPin.unsubscribe();

        model.getAllMethodNames().removeListener(allSettlementMethodsListener);
    }

    private void fillSettlementMethods() {
        flowPane.getChildren().clear();
        List<String> allSettlementMethodNames = new ArrayList<>(model.getAllMethodNames());
        allSettlementMethodNames.sort(Comparator.comparing(e -> Res.has(e) ? Res.get(e) : e));

        for (String methodName : allSettlementMethodNames) {
            // enum name or custom name
            String displayString = methodName;
            if (Res.has(methodName)) {
                String shortName = methodName + "_SHORT";
                if (Res.has(shortName)) {
                    displayString = Res.get(shortName);
                } else {
                    displayString = Res.get(methodName);
                }
            }
            ChipButton chipButton = new ChipButton(displayString);
            if (model.getSelectedMethodNames().contains(methodName)) {
                chipButton.setSelected(true);
            }
            chipButton.setOnAction(() -> controller.onToggleSettlementMethod(methodName, chipButton.isSelected()));
            String finalDisplayString = displayString;
            model.getAddedCustomMethodNames().stream()
                    .filter(customMethod -> customMethod.equals(methodName))
                    .findAny()
                    .ifPresentOrElse(
                            customMethod -> {
                                ImageView closeIcon = chipButton.setRightIcon("remove-white");
                                closeIcon.setOnMousePressed(e -> controller.onRemoveCustomMethod(methodName));
                                if (methodName.length() > 13) {
                                    chipButton.setTooltip(new BisqTooltip(finalDisplayString));
                                }
                            },
                            () -> {
                                // A provided method
                                ImageView icon = ImageUtil.getImageViewById(methodName);
                                chipButton.setLeftIcon(icon);
                            });
            flowPane.getChildren().add(chipButton);
        }
    }
}
