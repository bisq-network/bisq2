package bisq.desktop.main.content.wallet.create_wallet.verify;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateWalletVerifyView extends View<StackPane, CreateWalletVerifyModel, CreateWalletVerifyController> {

    public CreateWalletVerifyView(CreateWalletVerifyModel model,
                                  CreateWalletVerifyController controller) {
        super(new StackPane(), model, controller);


        root.setAlignment(Pos.CENTER);
        VBox content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("wallet.verifySeeds.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(headlineLabel, new Insets(0, 0, 10, 0));

        Label successHeadlineLabel = new Label(Res.get("wallet.verifySeeds.success.title"));
        successHeadlineLabel.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(successHeadlineLabel, new Insets(20, 0, 10, 0));

        Label successDescriptionLabel = new Label(Res.get("wallet.verifySeeds.success.description"));
        successDescriptionLabel.setWrapText(true);
        successDescriptionLabel.setMaxWidth(550);
        successDescriptionLabel.setTextAlignment(TextAlignment.CENTER);
        successDescriptionLabel.getStyleClass().add("bisq-text-1");
        VBox.setMargin(successDescriptionLabel, new Insets(0, 0, 20, 0));

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, successHeadlineLabel, successDescriptionLabel, Spacer.fillVBox());

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