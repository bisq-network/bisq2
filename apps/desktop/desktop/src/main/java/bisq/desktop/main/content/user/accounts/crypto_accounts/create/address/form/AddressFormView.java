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

package bisq.desktop.main.content.user.accounts.crypto_accounts.create.address.form;

import bisq.common.util.StringUtils;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public abstract class AddressFormView<M extends AddressFormModel, C extends AddressFormController<?, ?, ?>> extends View<VBox, M, C> {
    protected final static double FIELD_WIDTH_QUARTER = 187.5; // 780 is available width
    protected final static double FIELD_WIDTH_HALF = 385;
    protected final MaterialTextField address, autoConfNumConfirmations, autoConfMaxTradeAmount, autoConfExplorerUrls;
    protected final Switch isInstantSwitch, isAutoConfSwitch;
    protected final HBox isInstantSwitchHBox, isAutoConfSwitchHBox, autoConfHBox;
    protected Subscription runValidationPin;

    protected AddressFormView(M model, C controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(0, 30, 0, 30));

        address = new MaterialTextField(Res.get("paymentAccounts.crypto.address.address"), Res.get("paymentAccounts.crypto.address.address.prompt"));

        isInstantSwitch = new Switch(Res.get("paymentAccounts.crypto.address.isInstant"));
        isInstantSwitchHBox = new HBox(isInstantSwitch, Spacer.fillHBox());
        isAutoConfSwitch = new Switch(Res.get("paymentAccounts.crypto.address.autoConf.use"));
        isAutoConfSwitchHBox = new HBox(isAutoConfSwitch, Spacer.fillHBox());

        autoConfNumConfirmations = new MaterialTextField(Res.get("paymentAccounts.crypto.address.autoConf.numConfirmations"), Res.get("paymentAccounts.crypto.address.autoConf.numConfirmations.prompt"));
        autoConfNumConfirmations.setMinWidth(FIELD_WIDTH_QUARTER);
        autoConfNumConfirmations.setMaxWidth(FIELD_WIDTH_QUARTER);

        autoConfMaxTradeAmount = new MaterialTextField(Res.get("paymentAccounts.crypto.address.autoConf.maxTradeAmount"), Res.get("paymentAccounts.crypto.address.autoConf.maxTradeAmount.prompt"));
        autoConfMaxTradeAmount.setMinWidth(FIELD_WIDTH_QUARTER);
        autoConfMaxTradeAmount.setMaxWidth(FIELD_WIDTH_QUARTER);

        autoConfExplorerUrls = new MaterialTextField(Res.get("paymentAccounts.crypto.address.autoConf.explorerUrls"), Res.get("paymentAccounts.crypto.address.autoConf.explorerUrls.prompt"), Res.get("paymentAccounts.crypto.address.autoConf.explorerUrls.help"));
        autoConfExplorerUrls.setMinWidth(FIELD_WIDTH_HALF);
        autoConfExplorerUrls.setMaxWidth(FIELD_WIDTH_HALF);

        HBox.setHgrow(autoConfNumConfirmations, Priority.ALWAYS);
        HBox.setHgrow(autoConfMaxTradeAmount, Priority.ALWAYS);
        HBox.setHgrow(autoConfExplorerUrls, Priority.ALWAYS);
        autoConfHBox = new HBox(10, autoConfNumConfirmations, autoConfMaxTradeAmount, autoConfExplorerUrls);

        VBox.setMargin(isInstantSwitchHBox, new Insets(5, 0, 0, 0));
        VBox.setMargin(autoConfHBox, new Insets(10, 0, 0, 0));
        addContent();
    }

    protected void addContent() {
        root.getChildren().addAll(address,
                isInstantSwitchHBox,
                isAutoConfSwitchHBox,
                autoConfHBox);
    }

    @Override
    protected void onViewAttached() {
        address.setValidator(model.getAddressValidator());
        autoConfNumConfirmations.setValidator(model.getAutoConfNumConfirmationsValidator());
        autoConfMaxTradeAmount.setValidator(model.getAutoConfMaxTradeAmountValidator());
        autoConfExplorerUrls.setValidator(model.getAutoConfExplorerUrlsValidator());

        if (StringUtils.isNotEmpty(model.getAddress().get())) {
            address.setText(model.getAddress().get());
            address.validate();
        }
        if (StringUtils.isNotEmpty(model.getAutoConfNumConfirmations().get())) {
            autoConfNumConfirmations.setText(model.getAutoConfNumConfirmations().get());
            autoConfNumConfirmations.validate();
        }
        if (StringUtils.isNotEmpty(model.getAutoConfMaxTradeAmount().get())) {
            autoConfMaxTradeAmount.setText(model.getAutoConfMaxTradeAmount().get());
            autoConfMaxTradeAmount.validate();
        }
        if (StringUtils.isNotEmpty(model.getAutoConfExplorerUrls().get())) {
            autoConfExplorerUrls.setText(model.getAutoConfExplorerUrls().get());
            autoConfExplorerUrls.validate();
        }
        isAutoConfSwitch.setVisible(model.isAutoConfSupported());
        isAutoConfSwitch.setManaged(model.isAutoConfSupported());

        isInstantSwitch.setSelected(model.getIsInstant().get());
        isAutoConfSwitch.setSelected(model.getIsAutoConf().get());

        address.textProperty().bindBidirectional(model.getAddress());
        autoConfNumConfirmations.textProperty().bindBidirectional(model.getAutoConfNumConfirmations());
        autoConfMaxTradeAmount.textProperty().bindBidirectional(model.getAutoConfMaxTradeAmount());
        autoConfExplorerUrls.textProperty().bindBidirectional(model.getAutoConfExplorerUrls());

        autoConfHBox.visibleProperty().bind(model.getIsAutoConf());
        autoConfHBox.managedProperty().bind(model.getIsAutoConf());

        isInstantSwitch.setOnAction(e -> controller.onIsInstantToggled(isInstantSwitch.isSelected()));
        isAutoConfSwitch.setOnAction(e -> controller.onIsAutoConfToggled(isAutoConfSwitch.isSelected()));

        runValidationPin = EasyBind.subscribe(model.getRunValidation(), runValidation -> {
            if (runValidation) {
                address.validate();
                controller.onValidationDone();
            }
        });
    }

    @Override
    protected void onViewDetached() {
        address.textProperty().unbindBidirectional(model.getAddress());
        address.resetValidation();
        autoConfNumConfirmations.textProperty().unbindBidirectional(model.getAutoConfNumConfirmations());
        autoConfNumConfirmations.resetValidation();
        autoConfMaxTradeAmount.textProperty().unbindBidirectional(model.getAutoConfMaxTradeAmount());
        autoConfMaxTradeAmount.resetValidation();
        autoConfExplorerUrls.textProperty().unbindBidirectional(model.getAutoConfExplorerUrls());
        autoConfExplorerUrls.resetValidation();

        autoConfHBox.visibleProperty().unbind();
        autoConfHBox.managedProperty().unbind();

        isInstantSwitch.setOnAction(null);
        isAutoConfSwitch.setOnAction(null);

        runValidationPin.unsubscribe();
    }
}