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

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.containers.WizardOverlay;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.desktop.main.content.bisq_easy.trade_wizard.TradeWizardView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class WalletReceiveAddressView extends View<StackPane, WalletReceiveAddressModel, WalletReceiveAddressController> {
    private final AddressNoteInputBox addressNoteInputBox;
    private final Label addressDescriptionLabel, addressLabel;
    private final BisqMenuItem createAddressButton, saveNoteButton, clearNoteButton, generateQrCodeButton,
            copyAddressButton, addAddressNoteButton;
    private final ImageView chevronDownWhite, chevronDownGrey, chevronUpWhite, chevronUpGrey;
    private final HBox addressNoteHBox;
    private final VBox content;
    private final WizardOverlay overlay;
    private final Button closeOverlayButton, proceedOverlayButton;
    private Subscription isNewAddressPin, isAddressNoteValidPin, isAddressNoteEditablePin,
            shouldShowAddressNotePin, shouldShowOverlayPin;

    public WalletReceiveAddressView(WalletReceiveAddressModel model, WalletReceiveAddressController controller) {
        super(new StackPane(), model, controller);

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

        // More options
        chevronDownWhite = ImageUtil.getImageViewById("chevron-drop-menu-white");
        chevronDownGrey = ImageUtil.getImageViewById("chevron-drop-menu-grey");
        chevronUpWhite = ImageUtil.getImageViewById("chevron-drop-menu-up-white");
        chevronUpGrey = ImageUtil.getImageViewById("chevron-drop-menu-up-grey");
        addAddressNoteButton = new BisqMenuItem(Res.get("wallet.receive.addLocalNote"));
        addAddressNoteButton.setContentDisplay(ContentDisplay.RIGHT);

        // Address note
        addressNoteInputBox = new AddressNoteInputBox();
        addressNoteInputBox.setEditable(true);
        addressNoteInputBox.setMinWidth(230);
        addressNoteInputBox.setValidators(model.getAddressNoteMaxLengthValidator());
        addressNoteInputBox.setPromptText(Res.get("wallet.receive.addNote"));
        addressNoteInputBox.setIconTooltip(Res.get("wallet.receive.addressNoteInfo"));

        saveNoteButton = new BisqMenuItem("save-mid-grey", "save-mid-white");
        saveNoteButton.setTooltip(Res.get("wallet.receive.save"));

        clearNoteButton = new BisqMenuItem("close-mid-grey", "close-mid-white");
        clearNoteButton.setTooltip(Res.get("wallet.receive.clear"));

        HBox.setMargin(saveNoteButton, new Insets(8, 0, -8, 0));
        HBox.setMargin(clearNoteButton, new Insets(8, 0, -8, -10));
        addressNoteHBox = new HBox(20, addressNoteInputBox, saveNoteButton, clearNoteButton);
        addressNoteHBox.setAlignment(Pos.TOP_CENTER);
        addressNoteHBox.setMinHeight(56);

        // Overlay
        closeOverlayButton = new Button(Res.get("confirmation.no"));
        proceedOverlayButton = new Button(Res.get("confirmation.yes"));
        proceedOverlayButton.setDefaultButton(true);
        overlay = new WizardOverlay(root)
                .whiteWarning()
                .headline(Res.get("wallet.receive.overlay.headline"))
                .description(Res.get("wallet.receive.overlay.description"))
                .buttons(closeOverlayButton, proceedOverlayButton)
                .build();

        VBox.setMargin(addressVBox, new Insets(0, 0, 0, 40));
        VBox.setMargin(addressNoteHBox, new Insets(-30, 0, 0, 65));
        content = new VBox(40);
        content.getChildren().setAll(addressVBox, addAddressNoteButton, addressNoteHBox);
        content.setPadding(new Insets(100, 70, 0, 70));
        content.setAlignment(Pos.CENTER);
        content.setMinHeight(276);

        StackPane.setMargin(overlay, new Insets(-130, 0, 0, 0));
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(content, overlay);
        root.getStyleClass().add("wallet-receive-address");
    }

    @Override
    protected void onViewAttached() {
        addressDescriptionLabel.textProperty().bind(model.getAddressTextFieldDescription());
        addressLabel.textProperty().bind(model.getReceiveAddress());
        addressNoteHBox.visibleProperty().bind(model.getShouldShowAddressNote());
        addressNoteInputBox.textProperty().bindBidirectional(model.getReceiveAddressNote());

        isNewAddressPin = EasyBind.subscribe(model.getIsNewAddress(), this::resetValidation);
        isAddressNoteValidPin = EasyBind.subscribe(addressNoteInputBox.isValidProperty(), isValid -> {
            updateAddressNoteButtonsVisibility();
        });
        isAddressNoteEditablePin = EasyBind.subscribe(model.getIsAddressNoteEditable(), isEditable -> {
            addressNoteInputBox.setEditable(isEditable);
            updateAddressNoteButtonsVisibility();
        });
        shouldShowAddressNotePin = EasyBind.subscribe(model.getShouldShowAddressNote(), shouldShow -> {
            updateAddAddressNoteButtonIcon();
        });
        shouldShowOverlayPin = EasyBind.subscribe(model.getShouldShowOverlay(), shouldShow -> {
            overlay.updateOverlayVisibility(content, shouldShow, controller::onKeyPressedWhileShowingOverlay);
        });

        generateQrCodeButton.setOnAction(e -> controller.onGenerateQrCode());
        copyAddressButton.setOnAction(e -> controller.onCopyToClipboard());
        createAddressButton.setOnAction(e -> controller.onCreateNewReceiveAddress());
        saveNoteButton.setOnAction(e -> controller.onSaveAddressNote());
        clearNoteButton.setOnAction(e -> controller.onClearAddressNote());
        addAddressNoteButton.setOnAction(e -> controller.onAddAddressNote());
        addAddressNoteButton.setOnMouseEntered(e -> updateAddAddressNoteButtonIcon());
        addAddressNoteButton.setOnMouseExited(e -> updateAddAddressNoteButtonIcon());
        proceedOverlayButton.setOnAction(e -> controller.onProceedOverlay());
        closeOverlayButton.setOnAction(e -> controller.onCloseOverlay());
        root.setOnMouseClicked(e -> {
            root.requestFocus();
            addressNoteInputBox.validate();
        });
    }

    @Override
    protected void onViewDetached() {
        addressDescriptionLabel.textProperty().unbind();
        addressLabel.textProperty().unbind();
        addressNoteHBox.visibleProperty().unbind();
        addressNoteInputBox.textProperty().unbindBidirectional(model.getReceiveAddressNote());

        isNewAddressPin.unsubscribe();
        isAddressNoteValidPin.unsubscribe();
        isAddressNoteEditablePin.unsubscribe();
        shouldShowAddressNotePin.unsubscribe();
        shouldShowOverlayPin.unsubscribe();

        generateQrCodeButton.setOnAction(null);
        copyAddressButton.setOnAction(null);
        createAddressButton.setOnAction(null);
        saveNoteButton.setOnAction(null);
        clearNoteButton.setOnAction(null);
        addAddressNoteButton.setOnAction(null);
        addAddressNoteButton.setOnMouseEntered(null);
        addAddressNoteButton.setOnMouseExited(null);
        proceedOverlayButton.setOnAction(null);
        closeOverlayButton.setOnAction(null);
        root.setOnMouseClicked(null);

        addressNoteInputBox.resetValidation();
    }

    private void resetValidation(boolean isNewAddress) {
        addressNoteInputBox.resetValidation();
        updateAddressNoteButtonsVisibility();
    }

    private void updateAddressNoteButtonsVisibility() {
        boolean shouldBeVisible = model.getIsNewAddress().get()
                && addressNoteInputBox.isValidProperty().get()
                && model.getIsAddressNoteEditable().get();
        saveNoteButton.setVisible(shouldBeVisible);
        clearNoteButton.setVisible(shouldBeVisible);
    }

    private void updateAddAddressNoteButtonIcon() {
        boolean shouldShowAddressNote = model.getShouldShowAddressNote().get();
        boolean isHovered = addAddressNoteButton.isHover();
        if (shouldShowAddressNote) {
            addAddressNoteButton.setGraphic(isHovered ? chevronUpWhite : chevronUpGrey);
        } else {
            addAddressNoteButton.setGraphic(isHovered ? chevronDownWhite : chevronDownGrey);
        }
    }
}
