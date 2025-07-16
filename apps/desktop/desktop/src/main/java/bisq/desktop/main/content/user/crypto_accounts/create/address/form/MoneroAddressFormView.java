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

package bisq.desktop.main.content.user.crypto_accounts.create.address.form;

import bisq.common.util.StringUtils;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MoneroAddressFormView extends AddressFormView<MoneroAddressFormModel, MoneroAddressFormController> {
    private Switch useSubAddressesSwitch;
    private MaterialTextField privateViewKey, accountIndex, initialSubAddressIndex;
    private HBox subAddressesHBox;

    public MoneroAddressFormView(MoneroAddressFormModel model,
                                 MoneroAddressFormController controller) {
        super(model, controller);

        address.setDescription(Res.get("paymentAccounts.crypto.address.xmr.mainAddresses"));
        address.setPromptText(Res.get("paymentAccounts.crypto.address.xmr.mainAddresses.prompt"));
    }

    @Override
    protected void addContent() {
        useSubAddressesSwitch = new Switch(Res.get("paymentAccounts.crypto.address.xmr.useSubAddresses"));
        HBox useSubAddressesSwitchHBox = new HBox(useSubAddressesSwitch, Spacer.fillHBox());

        privateViewKey = new MaterialTextField(Res.get("paymentAccounts.crypto.address.xmr.privateViewKey"),
                Res.get("paymentAccounts.crypto.address.xmr.privateViewKey.prompt"));
        privateViewKey.setMinWidth(FIELD_WIDTH_HALF);
        privateViewKey.setMaxWidth(FIELD_WIDTH_HALF);
        privateViewKey.setValidator(model.getPrivateViewKeyValidator());

        accountIndex = new MaterialTextField(Res.get("paymentAccounts.crypto.address.xmr.accountIndex"),
                Res.get("paymentAccounts.crypto.address.xmr.accountIndex.prompt"));
        accountIndex.setMinWidth(FIELD_WIDTH_QUARTER);
        accountIndex.setMaxWidth(FIELD_WIDTH_QUARTER);
        accountIndex.setValidator(model.getAccountIndexValidator());

        initialSubAddressIndex = new MaterialTextField(Res.get("paymentAccounts.crypto.address.xmr.initialSubAddressIndex"),
                Res.get("paymentAccounts.crypto.address.xmr.initialSubAddressIndex.prompt"));
        initialSubAddressIndex.setMinWidth(FIELD_WIDTH_QUARTER);
        initialSubAddressIndex.setMaxWidth(FIELD_WIDTH_QUARTER);
        initialSubAddressIndex.setValidator(model.getInitialSubAddressIndexValidator());

        HBox.setHgrow(privateViewKey, Priority.ALWAYS);
        HBox.setHgrow(autoConfNumConfirmations, Priority.ALWAYS);
        HBox.setHgrow(autoConfMaxTradeAmount, Priority.ALWAYS);
        subAddressesHBox = new HBox(10, privateViewKey, accountIndex, initialSubAddressIndex);

        VBox.setMargin(subAddressesHBox, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(address,
                isInstantSwitchHBox,
                isAutoConfSwitchHBox,
                autoConfHBox,
                useSubAddressesSwitchHBox,
                subAddressesHBox);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        if (StringUtils.isNotEmpty(model.getPrivateViewKey().get())) {
            privateViewKey.setText(model.getPrivateViewKey().get());
            privateViewKey.validate();
        }
        useSubAddressesSwitch.setSelected(model.getUseSubAddresses().get());

        subAddressesHBox.visibleProperty().bind(model.getUseSubAddresses());
        subAddressesHBox.managedProperty().bind(model.getUseSubAddresses());

        privateViewKey.textProperty().bindBidirectional(model.getPrivateViewKey());
        accountIndex.textProperty().bindBidirectional(model.getAccountIndex());
        initialSubAddressIndex.textProperty().bindBidirectional(model.getInitialSubAddressIndex());

        useSubAddressesSwitch.setOnAction(e -> controller.onUseSubAddressesToggled(useSubAddressesSwitch.isSelected()));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        subAddressesHBox.visibleProperty().unbind();
        subAddressesHBox.managedProperty().unbind();

        privateViewKey.textProperty().unbindBidirectional(model.getPrivateViewKey());
        accountIndex.textProperty().unbindBidirectional(model.getAccountIndex());
        initialSubAddressIndex.textProperty().unbindBidirectional(model.getInitialSubAddressIndex());

        useSubAddressesSwitch.setOnAction(null);
    }
}