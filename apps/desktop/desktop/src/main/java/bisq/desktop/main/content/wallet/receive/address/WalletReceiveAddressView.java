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

package bisq.desktop.main.content.wallet.receive.address;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class WalletReceiveAddressView extends View<VBox, WalletReceiveAddressModel, WalletReceiveAddressController> {
    private final MaterialTextField name;
    private final Label addressDescriptionLabel, addressLabel;
    private final BisqMenuItem createAddressButton, saveNameButton, generateQrCodeButton, copyAddressButton;
    private Subscription isAddressNameEditablePin, isNewAddressPin, isNameValidPin, isNameEditablePin;

    public WalletReceiveAddressView(WalletReceiveAddressModel model, WalletReceiveAddressController controller) {
        super(new VBox(30), model, controller);

        // Address
        addressLabel = new Label();
        addressLabel.setMinWidth(Region.USE_PREF_SIZE);
        generateQrCodeButton = new BisqMenuItem("qr-code-green", "qr-code-white");
        generateQrCodeButton.setTooltip(Res.get("wallet.receive.generateQRCode"));
        copyAddressButton = new BisqMenuItem("copy-green", "copy-white");
        copyAddressButton.setTooltip(Res.get("wallet.receive.copyAddress"));
        HBox addressBarHBox = new HBox(10, addressLabel, Spacer.fillHBox(), generateQrCodeButton, copyAddressButton);
        addressBarHBox.setPadding(new Insets(15));
        addressBarHBox.getStyleClass().add("address-bar");
        addressBarHBox.setMinWidth(440);
        addressBarHBox.setMaxWidth(440);
        addressBarHBox.setAlignment(Pos.CENTER);

        createAddressButton = new BisqMenuItem("new-address-green", "new-address-white");
        createAddressButton.setTooltip(Res.get("wallet.receive.createNew"));
        HBox addressBarAndCreateButtonHBox = new HBox(20, addressBarHBox, createAddressButton);
        addressBarAndCreateButtonHBox.setAlignment(Pos.CENTER_LEFT);

        addressDescriptionLabel = new Label();
        addressDescriptionLabel.getStyleClass().add("address-description");
        VBox.setMargin(addressDescriptionLabel, new Insets(0, 0, 0, 15));
        VBox addressVBox = new VBox(2, addressDescriptionLabel, addressBarAndCreateButtonHBox);

        // Address name
        name = new MaterialTextField();
        name.setEditable(true);
        name.setMinWidth(230);
        name.setValidators(model.getAddressNameMinMaxLengthValidator());

        saveNameButton = new BisqMenuItem("save-green", "save-white");
        saveNameButton.setTooltip(Res.get("wallet.receive.save"));

        HBox nameBox = new HBox(20, name, saveNameButton, Spacer.fillHBox());
        nameBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().setAll(addressVBox, nameBox);
        root.setPadding(new Insets(60, 70, 0, 70));
        root.setMinHeight(276);
        root.getStyleClass().add("wallet-receive-address");
    }

    @Override
    protected void onViewAttached() {
        addressDescriptionLabel.textProperty().bind(model.getAddressTextFieldDescription());
        addressLabel.textProperty().bind(model.getReceiveAddress());
        name.visibleProperty().bind(model.getShouldShowAddressName());
        name.managedProperty().bind(model.getShouldShowAddressName());
        name.textProperty().bindBidirectional(model.getReceiveAddressName());
        name.descriptionProperty().bind(model.getNameTextFieldDescription());

        isAddressNameEditablePin = EasyBind.subscribe(model.getIsAddressNameEditable(), name::setEditable);
        isNewAddressPin = EasyBind.subscribe(model.getIsNewAddress(), this::resetValidation);
        isNameValidPin = EasyBind.subscribe(name.isValidProperty(), isValid -> updateSaveNameButtonVisibility());
        isNameEditablePin = EasyBind.subscribe(model.getIsAddressNameEditable(), isEditable -> updateSaveNameButtonVisibility());

        generateQrCodeButton.setOnAction(e -> controller.onGenerateQrCode());
        copyAddressButton.setOnAction(e -> controller.onCopyToClipboard());
        createAddressButton.setOnAction(e -> controller.onCreateNewReceiveAddress());
        saveNameButton.setOnAction(e -> controller.onSaveAddressName());
        root.setOnMouseClicked(e -> {
            root.requestFocus();
            name.validate();
        });
    }

    @Override
    protected void onViewDetached() {
        addressDescriptionLabel.textProperty().unbind();
        addressLabel.textProperty().unbind();
        name.visibleProperty().unbind();
        name.managedProperty().unbind();
        name.textProperty().unbindBidirectional(model.getReceiveAddressName());
        name.descriptionProperty().unbind();

        isAddressNameEditablePin.unsubscribe();
        isNewAddressPin.unsubscribe();
        isNameValidPin.unsubscribe();
        isNameEditablePin.unsubscribe();

        generateQrCodeButton.setOnAction(null);
        copyAddressButton.setOnAction(null);
        createAddressButton.setOnAction(null);
        saveNameButton.setOnAction(null);
        root.setOnMouseClicked(null);

        name.resetValidation();
    }

    private void resetValidation(boolean isNewAddress) {
        name.resetValidation();
        updateSaveNameButtonVisibility();
    }

    private void updateSaveNameButtonVisibility() {
        boolean shouldBeVisible = model.getIsNewAddress().get()
                && name.isValidProperty().get()
                && model.getIsAddressNameEditable().get();
        saveNameButton.setVisible(shouldBeVisible);
        saveNameButton.setManaged(shouldBeVisible);
    }
}
