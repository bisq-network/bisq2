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

package bisq.desktop.main.content.reputation.build_reputation.burn.tab3;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.OrderedList;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BurnBsqTab3View extends View<VBox, BurnBsqTab3Model, BurnBsqTab3Controller> {
    private final MaterialTextField pubKeyHash;
    private final Button closeButton, backButton;
    private final Hyperlink learnMore;

    public BurnBsqTab3View(BurnBsqTab3Model model,
                           BurnBsqTab3Controller controller,
                           Pane userProfileSelection) {
        super(new VBox(), model, controller);

        Label headline = new Label(Res.get("reputation.burnedBsq.howToHeadline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        OrderedList info = new OrderedList(Res.get("reputation.burnedBsq.howTo"), "bisq-text-13", 7, 5);

        Label userProfileSelectLabel = new Label(Res.get("user.bondedRoles.userProfile.select").toUpperCase());
        userProfileSelectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelectLabel.setAlignment(Pos.TOP_LEFT);

        pubKeyHash = new MaterialTextField(Res.get("reputation.pubKeyHash"), "");
        pubKeyHash.setEditable(false);
        pubKeyHash.showCopyIcon();

        backButton = new Button(Res.get("action.back"));

        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        HBox buttons = new HBox(20, backButton, closeButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setVgrow(info, Priority.ALWAYS);
        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        VBox.setMargin(userProfileSelectLabel, new Insets(10, 0, -20, 0));
        VBox.setMargin(userProfileSelection, new Insets(0, 0, -30, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));

        VBox contentBox = new VBox(20);
        contentBox.getChildren().addAll(headline, info, userProfileSelectLabel, userProfileSelection, pubKeyHash, buttons);
        contentBox.getStyleClass().addAll("bisq-common-bg", "common-line-spacing");
        root.getChildren().addAll(contentBox);
        root.setPadding(new Insets(20, 0, 0, 0));
    }

    @Override
    protected void onViewAttached() {
        pubKeyHash.textProperty().bind(model.getPubKeyHash());

        pubKeyHash.getIconButton().setOnAction(e -> controller.onCopyToClipboard(pubKeyHash.getText()));
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        pubKeyHash.textProperty().unbind();

        pubKeyHash.getIconButton().setOnAction(null);
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
