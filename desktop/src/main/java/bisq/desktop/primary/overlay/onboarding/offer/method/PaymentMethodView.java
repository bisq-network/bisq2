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

package bisq.desktop.primary.overlay.onboarding.offer.method;

import bisq.desktop.common.utils.Icons;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.TextInputBox;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentMethodView extends View<VBox, PaymentMethodModel, PaymentMethodController> {

    private final TextInputBox customMethodTextInputBox;
    private final ListChangeListener<String> allPaymentMethodsListener;
    private final FlowPane flowPane;
    private final Label nonFoundLabel;
    private final Label addCustomMethodIcon;

    public PaymentMethodView(PaymentMethodModel model, PaymentMethodController controller) {
        super(new VBox(), model, controller);

        root.setAlignment(Pos.TOP_CENTER);

        Label headLineLabel = new Label(Res.get("onboarding.method.headline"));
        headLineLabel.getStyleClass().add("bisq-text-headline-2");

        Label subtitleLabel = new Label(Res.get("onboarding.method.subTitle"));
        subtitleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.getStyleClass().addAll("bisq-text-3", "wrap-text");

        nonFoundLabel = new Label(Res.get("onboarding.method.noneFound"));
        nonFoundLabel.getStyleClass().add("bisq-text-6");
        nonFoundLabel.setAlignment(Pos.CENTER);

        flowPane = new FlowPane();
        flowPane.setAlignment(Pos.CENTER);
        flowPane.setVgap(20);
        flowPane.setHgap(20);

        customMethodTextInputBox = new TextInputBox(Res.get("onboarding.method.customMethod"),
                Res.get("onboarding.method.customMethod.prompt"));
        customMethodTextInputBox.setPrefWidth(230);
        addCustomMethodIcon = Icons.getIcon(AwesomeIcon.PLUS_SIGN, "15");
        addCustomMethodIcon.setTooltip(new Tooltip(Res.get("onboarding.method.customMethod.add")));
        StackPane.setMargin(addCustomMethodIcon, new Insets(5,0,0,203));
        StackPane stackPane = new StackPane( customMethodTextInputBox, addCustomMethodIcon);
        stackPane.setAlignment(Pos.TOP_CENTER);

        VBox.setMargin(headLineLabel, new Insets(44, 0, 2, 0));
        VBox.setMargin(flowPane, new Insets(80, 65, 33, 65));
        VBox.setMargin(nonFoundLabel, new Insets(80, 0, 20, 0));
        root.getChildren().addAll(headLineLabel, subtitleLabel, nonFoundLabel, flowPane, stackPane);

        allPaymentMethodsListener = c -> {
            c.next();
            fillPaymentMethods();
        };
    }

    @Override
    protected void onViewAttached() {
        customMethodTextInputBox.textProperty().bindBidirectional(model.getCustomMethod());
        addCustomMethodIcon.visibleProperty().bind(model.getAddCustomMethodIconVisible());
        nonFoundLabel.visibleProperty().bind(model.getPaymentMethodsEmpty());
        nonFoundLabel.managedProperty().bind(model.getPaymentMethodsEmpty());
        flowPane.visibleProperty().bind(model.getPaymentMethodsEmpty().not());
        flowPane.managedProperty().bind(model.getPaymentMethodsEmpty().not());

        addCustomMethodIcon.setOnMouseClicked(e -> controller.onAddCustomMethod());

        model.getAllPaymentMethods().addListener(allPaymentMethodsListener);
        fillPaymentMethods();
    }

    @Override
    protected void onViewDetached() {
        customMethodTextInputBox.textProperty().unbindBidirectional(model.getCustomMethod());
        addCustomMethodIcon.visibleProperty().unbind();
        nonFoundLabel.visibleProperty().unbind();
        nonFoundLabel.managedProperty().unbind();
        flowPane.visibleProperty().unbind();
        flowPane.managedProperty().unbind();

        addCustomMethodIcon.setOnMouseClicked(null);

        model.getAllPaymentMethods().removeListener(allPaymentMethodsListener);
    }

    private void fillPaymentMethods() {
        flowPane.getChildren().clear();
        for (int i = 0; i < model.getAllPaymentMethods().size(); i++) {
            String paymentMethod = model.getAllPaymentMethods().get(i);
            String displayString = Res.has("paymentMethod." + paymentMethod) ? Res.get("paymentMethod." + paymentMethod) : paymentMethod;
            ImageView icon = ImageUtil.getImageViewById(paymentMethod);
            ToggleButton button = new ToggleButton(displayString, icon);
            if (paymentMethod.length() > 13) {
                button.setTooltip(new Tooltip(displayString));
            }
            button.setGraphicTextGap(10);
            button.setAlignment(Pos.CENTER_LEFT);
            button.getStyleClass().setAll("bisq-border-icon-button");
            button.setSelected(model.getSelectedPaymentMethods().contains(paymentMethod));
            button.setOnAction(e -> controller.onTogglePaymentMethod(paymentMethod, button.isSelected()));
            button.setMinHeight(35);
            button.setMaxHeight(35);
            button.setMinWidth(142);
            button.setPrefWidth(142);
            button.setMaxWidth(142);
            StackPane stackPane = new StackPane(button);

            model.getAddedCustomMethods().stream()
                    .filter(customMethod -> customMethod.equals(paymentMethod))
                    .forEach(customMethod -> {
                        button.setPadding(new Insets(0, 0, 0, 0));
                        Label closeCustomIcon = Icons.getIcon(AwesomeIcon.MINUS_SIGN, "15");
                        closeCustomIcon.setCursor(Cursor.HAND);
                        closeCustomIcon.setOnMousePressed(e -> controller.onRemoveCustomMethod(paymentMethod));
                        StackPane.setMargin(closeCustomIcon, new Insets(-14, 0, 0, 120));
                        stackPane.getChildren().add(closeCustomIcon);
                    });
            flowPane.getChildren().add(stackPane);
        }
    }
}
