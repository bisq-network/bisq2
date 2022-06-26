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

package bisq.desktop.primary.main.content.settings.reputation.earnReputation.burn;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.TextInputBox;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqView extends View<VBox, BurnBsqModel, BurnBsqController> {
    private final Label pubKeyHashLabel;
    private final TextInputBox txIdInputBox;

    public BurnBsqView(BurnBsqModel model,
                       BurnBsqController controller,
                       Pane userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.CENTER_LEFT);
        // root.setPadding(new Insets(-40,0,0,0));

        Label headLine = new Label(Res.get("reputation.burnedBsq.headline"));
        headLine.getStyleClass().add("bisq-text-15");

        Label info = new Label(Res.get("reputation.burnedBsq.info"));
        info.setWrapText(true);
        info.getStyleClass().add("bisq-text-13");

        Label userProfileSelectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        userProfileSelectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelectLabel.setAlignment(Pos.TOP_LEFT);


        pubKeyHashLabel = new Label(Res.get("reputation.pubKeyHash"));
        pubKeyHashLabel.getStyleClass().addAll("bisq-text-3");

        txIdInputBox = new TextInputBox(Res.get("reputation.txId"), Res.get("reputation.txId.prompt"));
        TextField TextField2= new TextField("test");
     
        VBox.setMargin(headLine, new Insets(-40, 0, 0, 0));
        VBox.setMargin(userProfileSelectLabel, new Insets(0, 0, -20, 0));
        VBox.setMargin(userProfileSelection, new Insets(0, 0, 0, 0));
        root.getChildren().addAll(headLine, info,
                userProfileSelectLabel, userProfileSelection,
                pubKeyHashLabel,
                TextField2,
                txIdInputBox
        );
    }

    @Override
    protected void onViewAttached() {
        txIdInputBox.textProperty().bindBidirectional(model.getTxId());

    }

    @Override
    protected void onViewDetached() {
        txIdInputBox.textProperty().unbindBidirectional(model.getTxId());
    }
}
