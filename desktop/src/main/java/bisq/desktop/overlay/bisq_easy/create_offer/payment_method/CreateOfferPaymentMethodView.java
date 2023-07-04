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
public class CreateOfferPaymentMethodView extends View<VBox, CreateOfferPaymentMethodModel, CreateOfferPaymentMethodController> {

    private final MaterialTextField custom;
    private final ListChangeListener<FiatPaymentMethod> paymentMethodListener;
    private final FlowPane flowPane;
    private final Label nonFoundLabel;
    private final BisqIconButton addButton;
    private Subscription addCustomMethodIconEnabledPin;

    public CreateOfferPaymentMethodView(CreateOfferPaymentMethodModel model, CreateOfferPaymentMethodController controller) {
        super(new VBox(10), model, controller);

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
        root.getChildren().addAll(Spacer.fillVBox(), headLineLabel, subtitleLabel, nonFoundLabel, flowPane, custom, Spacer.fillVBox());

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

        addCustomMethodIconEnabledPin = EasyBind.subscribe(model.getIsAddCustomMethodIconEnabled(), enabled -> {
            custom.setIcon(enabled ? "add" : "add-white");
            addButton.setOpacity(enabled ? 1 : 0.15);
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
}
