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

package bisq.desktop.primary.main.content.settings.reputation.burn.tab3;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
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
    private final Button copyButton, closeButton, backButton;
    private final Hyperlink learnMore;

    public BurnBsqTab3View(BurnBsqTab3Model model,
                           BurnBsqTab3Controller controller,
                           Pane userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headLine = new Label(Res.get("reputation.burnedBsq.howToHeadline"));
        headLine.getStyleClass().add("bisq-text-headline-2");

        Label info = new Label(Res.get("reputation.burnedBsq.howTo"));
        info.getStyleClass().addAll("bisq-text-13", "wrap-text");

        Label userProfileSelectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        userProfileSelectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelectLabel.setAlignment(Pos.TOP_LEFT);

        pubKeyHash = new MaterialTextField(Res.get("reputation.pubKeyHash"), "");
        pubKeyHash.setEditable(false);

        copyButton = BisqIconButton.createIconButton(AwesomeIcon.COPY);

        HBox.setHgrow(pubKeyHash, Priority.ALWAYS);
        HBox pubKeyHashHBox = new HBox(10, pubKeyHash, copyButton);
        pubKeyHashHBox.setAlignment(Pos.CENTER_LEFT);

        closeButton = new Button(Res.get("close"));
        closeButton.setDefaultButton(true);
        learnMore = new Hyperlink(Res.get("reputation.learnMore"));
        backButton = new Button(Res.get("back"));

        HBox buttons = new HBox(20, backButton, closeButton);

        VBox.setVgrow(info, Priority.ALWAYS);
        VBox.setMargin(headLine, new Insets(10, 0, 0, 0));
        VBox.setMargin(userProfileSelectLabel, new Insets(10, 0, -20, 0));
        VBox.setMargin(userProfileSelection, new Insets(0, 0, -30, 0));
        root.getChildren().addAll(headLine, info, learnMore,
                userProfileSelectLabel, userProfileSelection,
                pubKeyHashHBox,
                buttons
        );
    }

    @Override
    protected void onViewAttached() {
        pubKeyHash.textProperty().bind(model.getPubKeyHash());

        copyButton.setOnAction(e -> controller.onCopyToClipboard(pubKeyHash.getText()));
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        pubKeyHash.textProperty().unbind();

        copyButton.setOnAction(null);
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
