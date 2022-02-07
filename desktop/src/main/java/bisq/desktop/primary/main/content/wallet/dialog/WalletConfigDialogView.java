package bisq.desktop.primary.main.content.wallet.dialog;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTextField;
import bisq.i18n.Res;
import javafx.beans.property.Property;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class WalletConfigDialogView extends View<VBox, WalletConfigDialogModel, WalletConfigDialogController> {
    public WalletConfigDialogView(WalletConfigDialogModel model, WalletConfigDialogController controller) {
        super(new VBox(), model, controller);

        var walletBackendComboBox = new ComboBox<>(model.getWalletBackends());
        walletBackendComboBox.setPromptText(Res.get("wallet.config.selectWallet"));

        root.getChildren().add(walletBackendComboBox);

        createAndAttachIpPortTextFields();
        createAndAttachUsernamePasswordTextFields();

        var walletPassphraseTextField = createAndBindPasswordField(
                Res.get("wallet.config.enterPassphrase"),
                model.walletPassphraseProperty()
        );
        root.getChildren().add(walletPassphraseTextField);
    }

    public boolean createAndShowDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(Res.get("wallet.config.title"));
        dialog.setHeaderText(Res.get("wallet.config.header"));

        ButtonType connectToWalletButton = new ButtonType(Res.get("wallet.config.connect"), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectToWalletButton, ButtonType.CANCEL);
        dialog.getDialogPane().setContent(root);

        Optional<ButtonType> dialogResult = dialog.showAndWait();
        return dialogResult.stream()
                .anyMatch(buttonType -> buttonType == connectToWalletButton);
    }

    private void createAndAttachIpPortTextFields() {
        TextField ipTextField = createAndBindTextField(
                Res.get("wallet.config.enterHostname"),
                model.hostnameProperty()
        );
        TextField portTextField = createAndBindTextField(
                Res.get("wallet.config.enterPort"),
                model.portProperty()
        );
        root.getChildren().addAll(ipTextField, portTextField);
    }

    private void createAndAttachUsernamePasswordTextFields() {
        TextField usernameTextField = createAndBindTextField(
                Res.get("wallet.config.enterUsername"),
                model.usernameProperty()
        );

        PasswordField passwordTextField = createAndBindPasswordField(
                Res.get("wallet.config.enterPassword"),
                model.passwordProperty()
        );

        root.getChildren().addAll(usernameTextField, passwordTextField);
    }

    private TextField createAndBindTextField(String promptText, Property<String> propertyToBindTo) {
        var bisqTextField = new BisqTextField();
        bisqTextField.setPromptText(promptText);
        bisqTextField.textProperty().bindBidirectional(propertyToBindTo);
        return bisqTextField;
    }

    private PasswordField createAndBindPasswordField(String promptText, Property<String> propertyToBindTo) {
        var passwordTextField = new PasswordField();
        passwordTextField.setPromptText(promptText);
        passwordTextField.textProperty().bindBidirectional(propertyToBindTo);
        return passwordTextField;
    }
}
