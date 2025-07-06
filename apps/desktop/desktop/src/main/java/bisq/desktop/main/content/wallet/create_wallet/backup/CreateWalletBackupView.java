package bisq.desktop.main.content.wallet.create_wallet.backup;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.wallet.create_wallet.SeedState;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class CreateWalletBackupView extends View<StackPane, CreateWalletBackupModel, CreateWalletBackupController> {
    VBox content;
    private final List<Label> seedLabelList = new ArrayList<>();
    private final List<ChangeListener<String>> seedWordListeners = new ArrayList<>();
    private final ChangeListener<SeedState> seedStateListener;

    private final Consumer<Boolean> navigationButtonsVisibleHandler;

    private static final int GRID_COLUMNS = 4;
    private static final int WORD_LABEL_WIDTH = 124;
    private static final int WORD_LABEL_HEIGHT = 40;

    public CreateWalletBackupView(CreateWalletBackupModel model,
                                  CreateWalletBackupController controller,
                                  Consumer<Boolean> navigationButtonsVisibleHandler) {
        super(new StackPane(), model, controller);

        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;

        root.setAlignment(Pos.CENTER);

        content = new VBox(10);
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
        seedGrid.setAlignment(Pos.CENTER);

        for (int i = 0; i < model.getSEED_WORD_COUNT(); i++) {
            Label wordLabel = new Label();
            seedLabelList.add(wordLabel);
            wordLabel.setMinWidth(WORD_LABEL_WIDTH);
            wordLabel.setMinHeight(WORD_LABEL_HEIGHT);
            wordLabel.setAlignment(Pos.CENTER);
            wordLabel.getStyleClass().add("bisq-box-1");
            int row = i / GRID_COLUMNS;
            int col = i % GRID_COLUMNS;
            seedGrid.add(wordLabel, col, row);

            int finalI = i;
            ChangeListener<String> listener = (obs, oldVal, newVal) ->
                    wordLabel.setText((finalI + 1) + ". " + newVal);
            seedWordListeners.add(listener);
        }

        seedStateListener = (obs, oldState, newState) -> updateUI(newState);

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, descriptionLabel, seedGrid, Spacer.fillVBox());

        root.getChildren().addAll(content);
    }

    @Override
    protected void onViewAttached() {
        for (int i = 0; i < model.getSEED_WORD_COUNT(); i++) {
            model.getSeedWords()[i].addListener(seedWordListeners.get(i));

            // Initial state
            Label label = seedLabelList.get(i);
            label.setText((i+ 1) + ". " + model.getSeedWords()[i].get());
        }

        // Listen to state and apply UI
        model.getSeedState().addListener(seedStateListener);

        updateUI(model.getSeedState().get());
    }

    @Override
    protected void onViewDetached() {
        if (seedStateListener != null) {
            model.getSeedState().removeListener(seedStateListener);
        }
        for (int i = 0; i < model.getSEED_WORD_COUNT(); i++) {
            model.getSeedWords()[i].removeListener(seedWordListeners.get(i));
        }
    }

    private void updateUI(SeedState state) {
        root.getChildren().clear();

        switch (state) {
            case LOADING -> {
                this.navigationButtonsVisibleHandler.accept(false);
                Label loadingLabel = new Label(Res.get("wallet.loading"));
                loadingLabel.getStyleClass().add("bisq-text-1");
                root.getChildren().add(loadingLabel);
            }
            case ERROR -> {
                this.navigationButtonsVisibleHandler.accept(false);
                Label errorLabel = new Label(Res.get("wallet.backupSeeds.error.failedToLoad"));
                errorLabel.getStyleClass().addAll("bisq-text-error", "font-size-16");

                Button retryButton = new Button(Res.get("wallet.retry"));
                retryButton.setDefaultButton(true);
                retryButton.setOnAction(e -> controller.onRetrySeed());

                VBox container = new VBox(40);
                container.getChildren().addAll(errorLabel, retryButton);
                container.setAlignment(Pos.CENTER);
                root.getChildren().addAll(container);
            }
            case SUCCESS -> {
                this.navigationButtonsVisibleHandler.accept(true);
                root.getChildren().add(content);
            }
        }
    }

}