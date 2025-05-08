package bisq.desktop.main.content.wallet.seed;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialPasswordField;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.open_trades.trade_state.states.FormUtils;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class WalletSeedView extends View<VBox, WalletSeedModel, WalletSeedController> {
    private final Button showSeedButton, restoreButton;
    private final MaterialTextField displaySeedWordsTextArea, seedWordsTextArea;
    private final MaterialPasswordField currentPasswordField;
    private final ChangeListener<String> seedWordsTextChangeListener;

    public WalletSeedView(WalletSeedModel model, WalletSeedController controller) {
        super(new VBox(20), model, controller);
        root.setPadding(new Insets(40, 0, 0, 0));
        Label backupTitle = new Label(Res.get("wallet.seed.backup.title"));
        backupTitle.getStyleClass().add("large-thin-headline");
        displaySeedWordsTextArea = FormUtils.getTextField(Res.get("wallet.seed.seedWords"), "", false);
        displaySeedWordsTextArea.setEditable(false);
        currentPasswordField = new MaterialPasswordField(Res.get("wallet.send.password"));
        showSeedButton = new Button(Res.get("wallet.seed.backup.show.seed"));

        Label restoreTitle = new Label(Res.get("wallet.seed.restore.title"));
        restoreTitle.getStyleClass().add("large-thin-headline");
        seedWordsTextArea = FormUtils.getTextField(Res.get("wallet.seed.seedWords"), "", true);
        seedWordsTextArea.showEditIcon();
        restoreButton = new Button(Res.get("wallet.seed.restore"));
        root.getChildren().addAll(backupTitle, displaySeedWordsTextArea, currentPasswordField, showSeedButton,
                restoreTitle, seedWordsTextArea, restoreButton);
        seedWordsTextChangeListener = (observable, oldValue, newValue) -> controller.onSeedValidator();
    }

    @Override
    protected void onViewAttached() {
        currentPasswordField.textProperty().bindBidirectional(model.getCurrentPassword());
        currentPasswordField.visibleProperty().bindBidirectional(model.getIsCurrentPasswordVisible());
        currentPasswordField.managedProperty().bindBidirectional(model.getIsCurrentPasswordVisible());
        displaySeedWordsTextArea.textProperty().bindBidirectional(model.getWalletSeed());
        seedWordsTextArea.textProperty().bindBidirectional(model.getRestoreSeed());
        seedWordsTextArea.textProperty().addListener(seedWordsTextChangeListener);
        showSeedButton.setOnAction(e -> {
            new Popup().information(Res.get("wallet.seed.backup.info"))
                    .closeButtonText(Res.get("action.cancel"))
                    .actionButtonText(Res.get("action.iUnderstand"))
                    .onAction(controller::onShowSeed)
                    .show();
        });
        showSeedButton.disableProperty().bindBidirectional(model.getShowSeedButtonDisable());
        restoreButton.setOnAction(e -> {
            new Popup().information(Res.get("wallet.seed.restore.info"))
                    .closeButtonText(Res.get("action.cancel"))
                    .actionButtonText(Res.get("action.iUnderstand"))
                    .onAction(controller::onRestore)
                    .show();
        });
        restoreButton.disableProperty().bindBidirectional(model.getRestoreButtonDisable());


    }

    @Override
    protected void onViewDetached() {
        currentPasswordField.textProperty().unbindBidirectional(model.getCurrentPassword());
        currentPasswordField.visibleProperty().unbindBidirectional(model.getIsCurrentPasswordVisible());
        currentPasswordField.managedProperty().unbindBidirectional(model.getIsCurrentPasswordVisible());
        displaySeedWordsTextArea.textProperty().unbindBidirectional(model.getWalletSeed());
        showSeedButton.setOnAction(null);
        showSeedButton.disableProperty().unbindBidirectional(model.getShowSeedButtonDisable());
        seedWordsTextArea.textProperty().unbindBidirectional(model.getRestoreSeed());
        seedWordsTextArea.textProperty().removeListener(seedWordsTextChangeListener);
        restoreButton.setOnAction(null);
        restoreButton.disableProperty().unbindBidirectional(model.getRestoreButtonDisable());
    }
}
