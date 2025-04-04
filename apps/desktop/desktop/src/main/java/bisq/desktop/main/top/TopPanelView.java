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

package bisq.desktop.main.top;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BtcSatsText;
import bisq.desktop.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class TopPanelView extends View<HBox, TopPanelModel, TopPanelController> {
    public static final int HEIGHT = 57;
    private BtcSatsText balanceText;

    public TopPanelView(TopPanelModel model,
                        TopPanelController controller,
                        UserProfileSelection userProfileSelection,
                        Pane marketPriceComponent) {
        super(new HBox(), model, controller);

        root.setMinHeight(HEIGHT);
        root.setMaxHeight(HEIGHT);
        root.setSpacing(28);
        root.setFillHeight(true);
        root.setStyle("-fx-background-color: -bisq-dark-grey;");
        HBox.setMargin(marketPriceComponent, new Insets(0, 10, 0, 0));

        Pane userProfileSelectionRoot = userProfileSelection.getRoot();
        HBox.setMargin(userProfileSelectionRoot, new Insets(6.5, 15, 0, 0));

        HBox balanceBox = createBalanceBox();
        HBox.setMargin(balanceBox, new Insets(0, 0, 0, 30));

        root.getChildren().addAll(Spacer.fillHBox(), marketPriceComponent, userProfileSelectionRoot);

        if (model.isWalletEnabled()) {
            root.getChildren().add(0, balanceBox);
        }
    }

    @Override
    protected void onViewAttached() {
        balanceText.btcAmountProperty().bind(model.getFormattedBalanceProperty());
    }

    @Override
    protected void onViewDetached() {
        balanceText.btcAmountProperty().unbind();
    }

    private HBox createBalanceBox() {
        Label titleLabel = new Label(Res.get("topPanel.wallet.balance").toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-18");

        balanceText = new BtcSatsText("0");
        configureBtcSatsText(balanceText);

        HBox hBox = new HBox(12, titleLabel, balanceText);
        balanceText.setTranslateY(4);
        hBox.setAlignment(Pos.CENTER_LEFT);

        return hBox;
    }

    private void configureBtcSatsText(BtcSatsText btcText) {
        btcText.applyMicroCompactConfig();
    }
}