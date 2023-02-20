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

package bisq.desktop.primary.main.top;

import bisq.common.data.Triple;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.primary.main.content.components.UserProfileSelection;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

public class TopPanelView extends View<HBox, TopPanelModel, TopPanelController> {
    public static final int HEIGHT = 57;
    private final Label balanceLabel;

    public TopPanelView(TopPanelModel model,
                        TopPanelController controller,
                        UserProfileSelection userProfileSelection,
                        Pane marketPriceBox) {
        super(new HBox(), model, controller);

        root.setMinHeight(HEIGHT);
        root.setMaxHeight(HEIGHT);
        root.setSpacing(28);
        root.setFillHeight(true);
        root.setStyle("-fx-background-color: -bisq-dark-grey;");
        HBox.setMargin(marketPriceBox, new Insets(0, 10, 0, 0));

        Pane userProfileSelectionRoot = userProfileSelection.getRoot();
        userProfileSelection.setIsLeftAligned(true);
        HBox.setMargin(userProfileSelectionRoot, new Insets(6.5, 15, 0, 0));

        Triple<HBox, Label, Label> balanceTriple = createBalanceBox();
        HBox balanceBox = balanceTriple.getFirst();
        balanceLabel = balanceTriple.getSecond();
        HBox.setMargin(balanceBox, new Insets(0, 0, 0, 30));

        root.getChildren().addAll(Spacer.fillHBox(), marketPriceBox, userProfileSelectionRoot);

        if (model.isWalletEnabled()) {
            root.getChildren().add(0, balanceBox);
        }
    }

    @Override
    protected void onViewAttached() {
        balanceLabel.textProperty().bind(model.getFormattedBalanceProperty());
    }

    @Override
    protected void onViewDetached() {
        balanceLabel.textProperty().unbind();
    }

    private Triple<HBox, Label, Label> createBalanceBox() {
        Label titleLabel = new Label(Res.get("wallet.balance").toUpperCase());
        titleLabel.getStyleClass().add("bisq-text-18");

        Label valueLabel = new Label();
        valueLabel.setId("bisq-text-20");

        Label codeLabel = new Label("BTC");
        codeLabel.setId("bisq-text-20");
        HBox.setMargin(codeLabel, new Insets(0, 0, 0, -10));

        HBox hBox = new HBox(12, titleLabel, valueLabel, codeLabel);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return new Triple<>(hBox, valueLabel, codeLabel);
    }
}
