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

package bisq.desktop.main.content.reputation.build_reputation;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqMenuItem;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BuildReputationView extends View<VBox, BuildReputationModel, BuildReputationController> {
    private final Button burnBsqButton, bsqBondButton, accountAgeButton, signedAccountButton;
    private final BisqMenuItem learnMoreLink;

    public BuildReputationView(BuildReputationModel model, BuildReputationController controller) {
        super(new VBox(), model, controller);

        Label headlineLabel = new Label(Res.get("reputation.buildReputation.headline"));
        headlineLabel.getStyleClass().add("reputation-headline");

        Label introLabelPart1 = new Label(Res.get("reputation.buildReputation.intro.part1"));
        Label introLabelPart2 = new Label(Res.get("reputation.buildReputation.intro.part2"));

        Label title = new Label(Res.get("reputation.buildReputation.title"));
        title.getStyleClass().add("reputation-title");

        // Burn BSQ
        burnBsqButton = new Button(Res.get("reputation.buildReputation.burnBsq.button"));
        burnBsqButton.setDefaultButton(true);
        VBox burnBsqBox = createAndGetBuildReputationMethodBox(
                Res.get("reputation.buildReputation.burnBsq.title"),
                Res.get("reputation.buildReputation.burnBsq.description"),
                burnBsqButton
        );

        // BSQ Bond
        bsqBondButton = new Button(Res.get("reputation.buildReputation.bsqBond.button"));
        VBox bsqBondBox = createAndGetBuildReputationMethodBox(
                Res.get("reputation.buildReputation.bsqBond.title"),
                Res.get("reputation.buildReputation.bsqBond.description"),
                bsqBondButton
        );

        HBox burnAndBondBox = new HBox(20, burnBsqBox, bsqBondBox);

        // Signed Account
        signedAccountButton = new Button(Res.get("reputation.buildReputation.signedAccount.button"));
        VBox signedAccountBox = createAndGetBuildReputationMethodBox(
                Res.get("reputation.buildReputation.signedAccount.title"),
                Res.get("reputation.buildReputation.signedAccount.description"),
                signedAccountButton
        );

        // Account Age
        accountAgeButton = new Button(Res.get("reputation.buildReputation.accountAge.button"));
        VBox accountAgeBox = createAndGetBuildReputationMethodBox(
                Res.get("reputation.buildReputation.accountAge.title"),
                Res.get("reputation.buildReputation.accountAge.description"),
                accountAgeButton
        );

        HBox signedAccountAndAgeBox = new HBox(20, signedAccountBox, accountAgeBox);

        Label learnMoreLabel = new Label(Res.get("reputation.buildReputation.learnMore"));
        learnMoreLink = new BisqMenuItem(Res.get("reputation.buildReputation.learnMore.link"));
        learnMoreLabel.getStyleClass().addAll("reputation-learn-more");
        learnMoreLink.getStyleClass().addAll("reputation-learn-more-link");
        HBox learnMoreHBox = new HBox(4, learnMoreLabel, learnMoreLink);

        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(headlineLabel, introLabelPart1, introLabelPart2, title, burnAndBondBox,
                signedAccountAndAgeBox, learnMoreHBox);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
        root.getStyleClass().add("reputation");
    }

    @Override
    protected void onViewAttached() {
        burnBsqButton.setOnAction(e -> controller.onBurnBsq());
        bsqBondButton.setOnAction(e -> controller.onBsqBond());
        signedAccountButton.setOnAction(e -> controller.onSignedAccount());
        accountAgeButton.setOnAction(e -> controller.onAccountAge());
        learnMoreLink.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        burnBsqButton.setOnAction(null);
        bsqBondButton.setOnAction(null);
        signedAccountButton.setOnAction(null);
        accountAgeButton.setOnAction(null);
        learnMoreLink.setOnAction(null);
    }

    private VBox createAndGetBuildReputationMethodBox(String title, String description, Button button) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        Label descriptionLabel = new Label(description);
        if (!button.isDefaultButton()) {
            button.getStyleClass().add("outlined-button");
        }
        button.getStyleClass().addAll("medium-large-button");
        button.setMaxWidth(Double.MAX_VALUE);
        VBox vBox = new VBox(20, titleLabel, descriptionLabel, Spacer.fillVBox(), button);
        vBox.setFillWidth(true);
        vBox.getStyleClass().add("reputation-card-small");
        return vBox;
    }
}
