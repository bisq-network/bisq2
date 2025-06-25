package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateWalletVerifyView extends View<StackPane, CreateWalletVerifyModel, CreateWalletVerifyController> {
    private final Label headlineLabel;
    private final VBox content;

    public CreateWalletVerifyView(CreateWalletVerifyModel model,
                                  CreateWalletVerifyController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        headlineLabel = new Label("Verify your backup");
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, Spacer.fillVBox());

        root.getChildren().addAll(content);
    }

    @Override
    protected void onViewAttached() {

    }

    @Override
    protected void onViewDetached() {
        root.setOnKeyPressed(null);
    }
}