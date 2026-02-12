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
public class MoneroFormView extends FormView<MoneroFormModel, MoneroFormController> {
    private Switch useSubAddressesSwitch;
    private MaterialTextField mainAddress, privateViewKey, subAddress, accountIndex, initialSubAddressIndex;
    private HBox subAddressesHBox,mainAddressPrivateViewKeyHBox;

    public MoneroFormView(MoneroFormModel model,
                          MoneroFormController controller) {
        super(model, controller);

        address.setDescription(Res.get("paymentAccounts.crypto.address.xmr.mainAddress"));
        address.setPromptText(Res.get("paymentAccounts.crypto.address.xmr.mainAddress.prompt"));
    }

    @Override
    protected void addContent() {
        useSubAddressesSwitch = new Switch(Res.get("paymentAccounts.crypto.address.xmr.useSubAddresses.switch"));
        HBox useSubAddressesSwitchHBox = new HBox(useSubAddressesSwitch, Spacer.fillHBox());

        mainAddress = new MaterialTextField(Res.get("paymentAccounts.crypto.address.xmr.mainAddress"),
                Res.get("paymentAccounts.crypto.address.xmr.mainAddress.prompt"));
        mainAddress.setMinWidth(FIELD_WIDTH_HALF);
        mainAddress.setMaxWidth(FIELD_WIDTH_HALF);
        mainAddress.setValidator(model.getMainAddressValidator());

        privateViewKey = new MaterialTextField(Res.get("paymentAccounts.crypto.address.xmr.privateViewKey"),
                Res.get("paymentAccounts.crypto.address.xmr.privateViewKey.prompt"));
        privateViewKey.setMinWidth(FIELD_WIDTH_HALF);
        privateViewKey.setMaxWidth(FIELD_WIDTH_HALF);
        privateViewKey.setValidator(model.getPrivateViewKeyValidator());

        subAddress = new MaterialTextField(Res.get("paymentAccounts.crypto.address.xmr.subAddress"));
        subAddress.setEditable(false);
        subAddress.setMinWidth(FIELD_WIDTH_HALF);
        subAddress.setMaxWidth(FIELD_WIDTH_HALF);

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

        HBox.setHgrow(mainAddress, Priority.ALWAYS);
        HBox.setHgrow(privateViewKey, Priority.ALWAYS);
        HBox.setHgrow(autoConfNumConfirmations, Priority.ALWAYS);
        HBox.setHgrow(autoConfMaxTradeAmount, Priority.ALWAYS);
        subAddressesHBox = new HBox(10, accountIndex, initialSubAddressIndex, subAddress);
        mainAddressPrivateViewKeyHBox = new HBox(10, mainAddress, privateViewKey);

        VBox.setMargin(subAddressesHBox, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(address,
                isInstantSwitchHBox,
                isAutoConfSwitchHBox,
                autoConfHBox,
                useSubAddressesSwitchHBox,
                mainAddressPrivateViewKeyHBox,
                subAddressesHBox
        );
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        if (StringUtils.isNotEmpty(model.getMainAddress().get())) {
            mainAddress.setText(model.getMainAddress().get());
            mainAddress.validate();
        }
        if (StringUtils.isNotEmpty(model.getPrivateViewKey().get())) {
            privateViewKey.setText(model.getPrivateViewKey().get());
            privateViewKey.validate();
        }
        if (StringUtils.isNotEmpty(model.getAccountIndex().get())) {
            accountIndex.setText(model.getAccountIndex().get());
            accountIndex.validate();
        }
        if (StringUtils.isNotEmpty(model.getInitialSubAddressIndex().get())) {
            initialSubAddressIndex.setText(model.getInitialSubAddressIndex().get());
            initialSubAddressIndex.validate();
        }

        useSubAddressesSwitch.setSelected(model.getUseSubAddresses().get());

        mainAddressPrivateViewKeyHBox.visibleProperty().bind(model.getUseSubAddresses());
        mainAddressPrivateViewKeyHBox.managedProperty().bind(model.getUseSubAddresses());
        subAddressesHBox.visibleProperty().bind(model.getUseSubAddresses());
        subAddressesHBox.managedProperty().bind(model.getUseSubAddresses());

        mainAddress.textProperty().bindBidirectional(model.getMainAddress());
        privateViewKey.textProperty().bindBidirectional(model.getPrivateViewKey());
        subAddress.textProperty().bind(model.getSubAddress());
        accountIndex.textProperty().bindBidirectional(model.getAccountIndex());
        initialSubAddressIndex.textProperty().bindBidirectional(model.getInitialSubAddressIndex());

        useSubAddressesSwitch.setOnAction(e -> controller.onUseSubAddressesToggled(useSubAddressesSwitch.isSelected()));
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();

        mainAddressPrivateViewKeyHBox.visibleProperty().unbind();
        mainAddressPrivateViewKeyHBox.managedProperty().unbind();
        subAddressesHBox.visibleProperty().unbind();
        subAddressesHBox.managedProperty().unbind();

        mainAddress.textProperty().unbindBidirectional(model.getMainAddress());
        privateViewKey.textProperty().unbindBidirectional(model.getPrivateViewKey());
        subAddress.textProperty().unbind();
        accountIndex.textProperty().unbindBidirectional(model.getAccountIndex());
        initialSubAddressIndex.textProperty().unbindBidirectional(model.getInitialSubAddressIndex());

        useSubAddressesSwitch.setOnAction(null);
    }
}