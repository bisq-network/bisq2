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

package bisq.desktop.main.content.user.reputation;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReputationView extends View<VBox, ReputationModel, ReputationController> {
    private final Button burnBsqButton, bsqBondButton, accountAgeButton, signedAccountButton;
    private final Hyperlink learnMore;
    private final VBox listVBox;

    public ReputationView(ReputationModel model, ReputationController controller, VBox listVBox) {
        super(new VBox(20), model, controller);
        this.listVBox = listVBox;

        Label headlineLabel = new Label(Res.get("user.reputation.headline"));
        headlineLabel.getStyleClass().add("bisq-text-headline-5");

        Label info = new Label(Res.get("user.reputation.info"));
        info.setWrapText(true);
        info.getStyleClass().addAll("bisq-text-13");
        info.setMinHeight(220);

        burnBsqButton = new Button(Res.get("user.reputation.burnBsq"));
        burnBsqButton.getStyleClass().add("button-reduced-padding");
        burnBsqButton.setPrefWidth(140);

        bsqBondButton = new Button(Res.get("user.reputation.bond"));
        bsqBondButton.getStyleClass().add("button-reduced-padding");
        bsqBondButton.setPrefWidth(130);

        signedAccountButton = new Button(Res.get("user.reputation.signedWitness"));
        signedAccountButton.getStyleClass().add("button-reduced-padding");
        signedAccountButton.setPrefWidth(230);

        accountAgeButton = new Button(Res.get("user.reputation.accountAge"));
        accountAgeButton.getStyleClass().add("button-reduced-padding");
        accountAgeButton.setPrefWidth(140);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        HBox buttons = new HBox(20, burnBsqButton, bsqBondButton, signedAccountButton, accountAgeButton);

        VBox.setMargin(headlineLabel, new Insets(-10, 0, 0, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        VBox vBox = new VBox(10, headlineLabel, info, buttons, learnMore);
        vBox.getStyleClass().add("bisq-box-2");
        vBox.setPadding(new Insets(30, 30, 20, 30));
        vBox.setAlignment(Pos.TOP_LEFT);


        VBox.setMargin(vBox, new Insets(0, 0, 20, 0));
        VBox.setVgrow(vBox, Priority.SOMETIMES);
        root.setPadding(new Insets(0, 40, 40, 40));
        root.getChildren().addAll(vBox, listVBox);
    }

    @Override
    protected void onViewAttached() {
        burnBsqButton.setOnAction(e -> controller.onBurnBsq());
        bsqBondButton.setOnAction(e -> controller.onBsqBond());
        signedAccountButton.setOnAction(e -> controller.onSignedAccount());
        accountAgeButton.setOnAction(e -> controller.onAccountAge());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        burnBsqButton.setOnAction(null);
        bsqBondButton.setOnAction(null);
        signedAccountButton.setOnAction(null);
        accountAgeButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
