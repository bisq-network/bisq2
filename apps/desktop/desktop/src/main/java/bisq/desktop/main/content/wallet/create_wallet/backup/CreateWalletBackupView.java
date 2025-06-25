package bisq.desktop.main.content.wallet.create_wallet.backup;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateWalletBackupView extends View<StackPane, CreateWalletBackupModel, CreateWalletBackupController> {

    public CreateWalletBackupView(CreateWalletBackupModel model,
                                  CreateWalletBackupController controller) {
        super(new StackPane(), model, controller);

        root.setAlignment(Pos.CENTER);
        VBox content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("wallet.backupSeeds.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");
        VBox.setMargin(headlineLabel, new Insets(0, 0, 10, 0));

        Label descriptionLabel = new Label(Res.get("wallet.backupSeeds.description"));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(550);
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.getStyleClass().add("bisq-text-1");
        VBox.setMargin(descriptionLabel, new Insets(0, 0, 20, 0));

        GridPane seedGrid = new GridPane();
        seedGrid.setHgap(10);
        seedGrid.setVgap(10);
        seedGrid.setPadding(new Insets(0, 0, 20, 0));
        seedGrid.setAlignment(Pos.CENTER);

        for (int i = 0; i < 12; i++) {
            Label wordLabel = new Label((i + 1) + ". Word " + (i + 1)); // Replace with actual seed word if available
            wordLabel.setMinWidth(124);
            wordLabel.setMinHeight(40);
            wordLabel.setAlignment(Pos.CENTER);
            wordLabel.getStyleClass().add("bisq-box-1");
            int row = i / 4;
            int col = i % 4;
            seedGrid.add(wordLabel, col, row);
        }

        Label description2Label = new Label(Res.get("wallet.backupSeeds.endInfo"));
        description2Label.setWrapText(true);
        description2Label.setMaxWidth(550);
        description2Label.setTextAlignment(TextAlignment.CENTER);
        description2Label.getStyleClass().add("bisq-text-1");
        VBox.setMargin(description2Label, new Insets(0, 0, 40, 0));

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, descriptionLabel, seedGrid, description2Label, Spacer.fillVBox());

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