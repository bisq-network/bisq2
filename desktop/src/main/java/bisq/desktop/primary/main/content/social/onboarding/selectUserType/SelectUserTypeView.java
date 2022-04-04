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

package bisq.desktop.primary.main.content.social.onboarding.selectUserType;

import bisq.common.data.Pair;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.SectionBox;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

public class SelectUserTypeView extends View<AnchorPane, SelectUserTypeModel, SelectUserTypeController> {
    private final BisqButton newTraderButton, proTraderButton, skipButton;

    public SelectUserTypeView(SelectUserTypeModel model, SelectUserTypeController controller) {
        super(new AnchorPane(), model, controller);

        Pair<Node, BisqButton> newTraderPair = getButton(Res.get("satoshisquareapp.selectTraderType.newbie"),
                Res.get("satoshisquareapp.selectTraderType.newbie.info"),
                Res.get("satoshisquareapp.selectTraderType.newbie.button"));
        Pair<Node, BisqButton> proTraderPair = getButton(Res.get("satoshisquareapp.selectTraderType.proTrader"),
                Res.get("satoshisquareapp.selectTraderType.proTrader.info"),
                Res.get("satoshisquareapp.selectTraderType.proTrader.button"));
        Pair<Node, BisqButton> skipPair = getButton(Res.get("satoshisquareapp.selectTraderType.skip"),
                Res.get("satoshisquareapp.selectTraderType.skip.info"),
                Res.get("satoshisquareapp.selectTraderType.skip.button"));
        newTraderButton = newTraderPair.second();
        proTraderButton = proTraderPair.second();
        skipButton = skipPair.second();

        SectionBox sectionBox = new SectionBox(Res.get("satoshisquareapp.selectTraderType.headline"));
        sectionBox.setAlignment(Pos.CENTER);
        sectionBox.getChildren().addAll(newTraderPair.first(), proTraderPair.first(), skipPair.first());

        Layout.pinToAnchorPane(sectionBox, 0, 20, null, 20);
        root.getChildren().addAll(sectionBox);
    }

    @Override
    protected void onViewAttached() {
        newTraderButton.setOnAction(e -> controller.onNewTrader());
        proTraderButton.setOnAction(e -> controller.onProTrader());
        skipButton.setOnAction(e -> controller.onSkip());
    }

    @Override
    protected void onViewDetached() {
    }

    private Pair<Node, BisqButton> getButton(String header, String info, String buttonText) {
        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-size: 1.3em; -fx-font-weight:bold; -fx-fill: -fx-dark-text-color;");

        Label infoLabel = new Label(info);
        infoLabel.setStyle("-fx-font-size: 1.1em; -fx-fill: -fx-dark-text-color;");
        infoLabel.setWrapText(true);

        VBox box = new VBox();
        box.setSpacing(10);
        box.setPadding(new Insets(20, 20, 20, 20));
        box.setStyle("-fx-background-color: -bs-background-color");

        BisqButton button = new BisqButton(buttonText);
        button.setStyle("-fx-background-color: -fx-selection-bar");
        box.getChildren().addAll(headerLabel, infoLabel, Layout.hBoxWith(Spacer.fillHBox(), button));
        return new Pair<>(box, button);
    }

}
