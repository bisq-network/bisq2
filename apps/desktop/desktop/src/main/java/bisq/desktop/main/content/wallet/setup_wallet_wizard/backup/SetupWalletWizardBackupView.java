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

package bisq.desktop.main.content.wallet.setup_wallet_wizard.backup;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.main.content.wallet.setup_wallet_wizard.SeedState;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class SetupWalletWizardBackupView extends View<StackPane, SetupWalletWizardBackupModel, SetupWalletWizardBackupController> {
    private static final int WORD_LABEL_WIDTH = 124;
    private static final int WORD_LABEL_HEIGHT = 40;

    private final VBox content;
    private final GridPane seedGrid;
    private final ListChangeListener<String> seedWordsListener;
    private final ChangeListener<SeedState> seedStateListener;
    private final Consumer<Boolean> navigationButtonsVisibleHandler;

    public SetupWalletWizardBackupView(SetupWalletWizardBackupModel model,
                                       SetupWalletWizardBackupController controller,
                                       Consumer<Boolean> navigationButtonsVisibleHandler) {
        super(new StackPane(), model, controller);

        this.navigationButtonsVisibleHandler = navigationButtonsVisibleHandler;

        root.setAlignment(Pos.CENTER);

        content = new VBox(10);
        content.setAlignment(Pos.TOP_CENTER);

        Label headlineLabel = new Label(Res.get("wallet.backupSeeds.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-2");

        Label descriptionLabel = new Label(Res.get("wallet.backupSeeds.description"));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(550);
        descriptionLabel.setTextAlignment(TextAlignment.CENTER);
        descriptionLabel.getStyleClass().add("bisq-text-3");
        VBox.setMargin(descriptionLabel, new Insets(0, 0, 10, 0));

        seedGrid = new GridPane();
        seedGrid.setHgap(10);
        seedGrid.setVgap(10);
        seedGrid.setAlignment(Pos.CENTER);

        seedWordsListener = change -> renderSeedWords();
        seedStateListener = (obs, oldState, newState) -> updateUI(newState);

        content.getChildren().addAll(Spacer.fillVBox(), headlineLabel, descriptionLabel, seedGrid, Spacer.fillVBox());

        root.getChildren().addAll(content);
    }

    @Override
    protected void onViewAttached() {
        model.getSeedWords().addListener(seedWordsListener);
        renderSeedWords();

        model.getSeedState().addListener(seedStateListener);
        updateUI(model.getSeedState().get());
    }

    @Override
    protected void onViewDetached() {
        model.getSeedState().removeListener(seedStateListener);
        model.getSeedWords().removeListener(seedWordsListener);
    }

    // 12 words fit in 4 columns, 24 words need 6 to keep the grid on screen
    private void renderSeedWords() {
        List<String> seedWords = model.getSeedWords();
        int columns = seedWords.size() <= 12 ? 4 : 6;
        seedGrid.getChildren().clear();
        for (int i = 0; i < seedWords.size(); i++) {
            Label wordLabel = new Label((i + 1) + ". " + seedWords.get(i));
            wordLabel.setMinWidth(WORD_LABEL_WIDTH);
            wordLabel.setMinHeight(WORD_LABEL_HEIGHT);
            wordLabel.setAlignment(Pos.CENTER);
            wordLabel.getStyleClass().add("chips-button");
            seedGrid.add(wordLabel, i % columns, i / columns);
        }
    }

    private void updateUI(SeedState state) {
        switch (state) {
            case LOADING -> {
                root.getChildren().clear();
                this.navigationButtonsVisibleHandler.accept(false);
                Label loadingLabel = new Label(Res.get("wallet.loading"));
                loadingLabel.getStyleClass().add("bisq-text-1");
                root.getChildren().add(loadingLabel);
            }
            case ERROR -> {
                // Handled in controller with Error dialog.
                // On closing it, the wizard navigates back to previous step.
            }
            case SUCCESS -> {
                root.getChildren().clear();
                this.navigationButtonsVisibleHandler.accept(true);
                root.getChildren().add(content);
            }
        }
    }
}
