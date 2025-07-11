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

package bisq.desktop.overlay.chat_rules;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqIconButton;
import bisq.desktop.components.controls.UnorderedList;
import bisq.desktop.overlay.OverlayModel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatRulesView extends View<VBox, ChatRulesModel, ChatRulesController> {
    private final Button closeIconButton;
    private final Button closeButton;

    public ChatRulesView(ChatRulesModel model, ChatRulesController controller) {
        super(new VBox(20), model, controller);

        root.setAlignment(Pos.TOP_LEFT);
        root.setPrefWidth(OverlayModel.WIDTH);
        root.setPrefHeight(505);

        root.setPadding(new Insets(30, 60, 30, 60));

        Label headline = new Label(Res.get("chat.chatRules.headline"));
        headline.getStyleClass().add("chat-guide-headline");

        UnorderedList content = new UnorderedList(Res.get("chat.chatRules.content"), "bisq-text-13",
                7, 5, UnorderedList.REGEX, UnorderedList.BULLET_SYMBOL);

        closeIconButton = BisqIconButton.createIconButton("close");

        HBox.setMargin(closeIconButton, new Insets(-1, -15, 0, 0));
        HBox hBox = new HBox(headline, Spacer.fillHBox(), closeIconButton);

        closeButton = new Button(Res.get("action.close"));
        closeButton.setDefaultButton(true);

        VBox.setMargin(closeButton, new Insets(15, 0, 0, 0));
        root.getChildren().addAll(hBox, content, closeButton);
    }

    @Override
    protected void onViewAttached() {
        closeIconButton.setOnAction(e -> controller.onClose());
        closeButton.setOnAction(e -> controller.onClose());
    }

    @Override
    protected void onViewDetached() {
        closeIconButton.setOnAction(null);
        closeButton.setOnAction(null);
    }
}
